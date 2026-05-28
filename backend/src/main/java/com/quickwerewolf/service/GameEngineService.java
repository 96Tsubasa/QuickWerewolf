package com.quickwerewolf.service;

import com.quickwerewolf.domain.GamePhase;
import com.quickwerewolf.domain.Player;
import com.quickwerewolf.domain.Role;
import com.quickwerewolf.domain.Room;
import com.quickwerewolf.dto.RoomStateDto;
import com.quickwerewolf.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledFuture;
import java.util.stream.Collectors;

@Service
public class GameEngineService {

    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private RoomService roomService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private TaskScheduler taskScheduler;

    private Map<String, ScheduledFuture<?>> roomSchedules = new ConcurrentHashMap<>();

    private static final int NIGHT_DURATION_SEC = 30;
    private static final int DAY_DISCUSS_DURATION_SEC = 120;
    private static final int DAY_VOTE_DURATION_SEC = 30;

    public void startGame(String roomId, String hostDeviceId) {
        Room room = roomRepository.findById(roomId).orElseThrow();
        
        if (room.getCurrentPhase() != GamePhase.LOBBY) {
            throw new IllegalStateException("Game already started");
        }

        // Assign roles
        List<Role> rolesToAssign = new ArrayList<>();
        room.getRoleCounts().forEach((role, count) -> {
            for (int i = 0; i < count; i++) {
                rolesToAssign.add(role);
            }
        });
        Collections.shuffle(rolesToAssign);

        List<Player> playingPlayers = room.getPlayers().stream()
                .filter(p -> room.isHostPlays() || !p.isHost())
                .collect(Collectors.toList());

        if (playingPlayers.size() > rolesToAssign.size()) {
            throw new IllegalStateException("Not enough roles for all players");
        }

        for (int i = 0; i < playingPlayers.size(); i++) {
            playingPlayers.get(i).setRole(rolesToAssign.get(i));
            playingPlayers.get(i).setAlive(true);
        }

        room.setCurrentPhase(GamePhase.NIGHT);
        room.setNightNumber(1);
        room.setDayNumber(0);
        room.setNightActions(new HashMap<>());
        room.setDayVotes(new HashMap<>());
        room.setPreviousProtectedPlayerId(null);
        
        long endTime = System.currentTimeMillis() + (NIGHT_DURATION_SEC * 1000);
        room.setPhaseEndTime(endTime);

        roomRepository.save(room);

        broadcastSystemMessage(roomId, "Night 1 started");
        roomService.broadcastRoomState(roomId, roomService.getRoomState(roomId));

        scheduleNextPhase(roomId, endTime);
    }

    private void scheduleNextPhase(String roomId, long endTime) {
        ScheduledFuture<?> future = roomSchedules.get(roomId);
        if (future != null) {
            future.cancel(false);
        }
        
        future = taskScheduler.schedule(() -> {
            advancePhase(roomId);
        }, Instant.ofEpochMilli(endTime));
        
        roomSchedules.put(roomId, future);
    }

    public synchronized void advancePhase(String roomId) {
        Room room = roomRepository.findById(roomId).orElse(null);
        if (room == null || room.getCurrentPhase() == GamePhase.ENDED) return;

        if (room.getCurrentPhase() == GamePhase.NIGHT) {
            resolveNightActions(room);
            if (checkWinConditions(room)) return;
            
            room.setCurrentPhase(GamePhase.DAY_DISCUSSION);
            room.setDayNumber(room.getDayNumber() + 1);
            long endTime = System.currentTimeMillis() + (DAY_DISCUSS_DURATION_SEC * 1000);
            room.setPhaseEndTime(endTime);
            broadcastSystemMessage(roomId, "Day " + room.getDayNumber() + " started - Discussion time");
            
            roomRepository.save(room);
            roomService.broadcastRoomState(roomId, roomService.getRoomState(roomId));
            scheduleNextPhase(roomId, endTime);
            
        } else if (room.getCurrentPhase() == GamePhase.DAY_DISCUSSION) {
            room.setCurrentPhase(GamePhase.DAY_VOTING);
            room.setDayVotes(new HashMap<>());
            long endTime = System.currentTimeMillis() + (DAY_VOTE_DURATION_SEC * 1000);
            room.setPhaseEndTime(endTime);
            broadcastSystemMessage(roomId, "Voting time!");
            
            roomRepository.save(room);
            roomService.broadcastRoomState(roomId, roomService.getRoomState(roomId));
            scheduleNextPhase(roomId, endTime);
            
        } else if (room.getCurrentPhase() == GamePhase.DAY_VOTING) {
            resolveDayVotes(room);
            if (checkWinConditions(room)) return;
            
            room.setCurrentPhase(GamePhase.NIGHT);
            room.setNightNumber(room.getNightNumber() + 1);
            room.setNightActions(new HashMap<>());
            long endTime = System.currentTimeMillis() + (NIGHT_DURATION_SEC * 1000);
            room.setPhaseEndTime(endTime);
            broadcastSystemMessage(roomId, "Night " + room.getNightNumber() + " started");
            
            roomRepository.save(room);
            roomService.broadcastRoomState(roomId, roomService.getRoomState(roomId));
            scheduleNextPhase(roomId, endTime);
        }
    }

    private void resolveNightActions(Room room) {
        String protectedId = null;
        String skTargetId = null;
        
        List<String> wwTargets = new ArrayList<>();
        
        for (Map.Entry<String, String> action : room.getNightActions().entrySet()) {
            String actorId = action.getKey();
            String targetId = action.getValue();
            
            Player actor = room.getPlayers().stream().filter(p -> p.getDeviceId().equals(actorId)).findFirst().orElse(null);
            if (actor == null || !actor.isAlive()) continue;
            
            if (actor.getRole() == Role.BODYGUARD) {
                protectedId = targetId;
                room.setPreviousProtectedPlayerId(targetId);
            } else if (actor.getRole() == Role.SERIAL_KILLER) {
                skTargetId = targetId;
            } else if (actor.getRole() == Role.WEREWOLF) {
                wwTargets.add(targetId);
            } else if (actor.getRole() == Role.SEER) {
                Player target = room.getPlayers().stream().filter(p -> p.getDeviceId().equals(targetId)).findFirst().orElse(null);
                if (target != null) {
                    messagingTemplate.convertAndSendToUser(actorId, "/queue/room/" + room.getRoomId() + "/seer", 
                        "You checked " + target.getDisplayName() + " and their role is " + target.getRole().name());
                }
            }
        }
        
        String wwFinalTarget = null;
        if (!wwTargets.isEmpty()) {
            // Count votes
            Map<String, Long> counts = wwTargets.stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
            long maxVotes = counts.values().stream().max(Long::compare).orElse(0L);
            List<String> tiedTargets = counts.entrySet().stream().filter(e -> e.getValue() == maxVotes).map(Map.Entry::getKey).collect(Collectors.toList());
            wwFinalTarget = tiedTargets.get(new Random().nextInt(tiedTargets.size()));
        }

        if (wwFinalTarget != null && !wwFinalTarget.equals(protectedId)) {
            killPlayer(room, wwFinalTarget);
            broadcastSystemMessage(room.getRoomId(), "A player was killed by werewolves.");
        }
        
        if (skTargetId != null && !skTargetId.equals(protectedId)) {
            killPlayer(room, skTargetId);
            broadcastSystemMessage(room.getRoomId(), "A player was killed by the Serial Killer.");
        }
    }

    private void resolveDayVotes(Room room) {
        long aliveCount = room.getPlayers().stream().filter(Player::isAlive).count();
        if (room.getDayVotes().isEmpty()) return;

        Map<String, Long> counts = room.getDayVotes().values().stream().collect(Collectors.groupingBy(e -> e, Collectors.counting()));
        long maxVotes = counts.values().stream().max(Long::compare).orElse(0L);
        
        if (maxVotes >= (aliveCount / 2.0)) {
            List<String> tiedTargets = counts.entrySet().stream().filter(e -> e.getValue() == maxVotes).map(Map.Entry::getKey).collect(Collectors.toList());
            String lynchedId = tiedTargets.get(new Random().nextInt(tiedTargets.size()));
            
            Player lynched = room.getPlayers().stream().filter(p -> p.getDeviceId().equals(lynchedId)).findFirst().orElse(null);
            if (lynched != null) {
                lynched.setAlive(false);
                broadcastSystemMessage(room.getRoomId(), lynched.getDisplayName() + " was lynched by the village.");
                
                if (lynched.getRole() == Role.FOOL) {
                    endGame(room, "Fool wins! " + lynched.getDisplayName() + " was successfully lynched.");
                }
            }
        } else {
            broadcastSystemMessage(room.getRoomId(), "Not enough votes to lynch anyone.");
        }
    }

    private void killPlayer(Room room, String targetId) {
        room.getPlayers().stream()
            .filter(p -> p.getDeviceId().equals(targetId))
            .findFirst()
            .ifPresent(p -> p.setAlive(false));
    }

    private boolean checkWinConditions(Room room) {
        List<Player> alive = room.getPlayers().stream().filter(Player::isAlive).collect(Collectors.toList());
        long wwCount = alive.stream().filter(p -> p.getRole() == Role.WEREWOLF).count();
        long skCount = alive.stream().filter(p -> p.getRole() == Role.SERIAL_KILLER).count();
        long totalAlive = alive.size();

        if (totalAlive == 1 && alive.get(0).getRole() == Role.SERIAL_KILLER) {
            endGame(room, "Serial Killer wins! They are the last survivor.");
            return true;
        }

        if (wwCount >= totalAlive / 2.0 && skCount == 0) {
            endGame(room, "Werewolves win! They control the village.");
            return true;
        }

        if (wwCount == 0 && skCount == 0) {
            endGame(room, "Village wins! All threats have been eliminated.");
            return true;
        }

        return false;
    }

    private void endGame(Room room, String message) {
        room.setCurrentPhase(GamePhase.ENDED);
        roomRepository.save(room);
        broadcastSystemMessage(room.getRoomId(), "GAME OVER: " + message);
        roomService.broadcastRoomState(room.getRoomId(), roomService.getRoomState(room.getRoomId()));
        
        ScheduledFuture<?> future = roomSchedules.remove(room.getRoomId());
        if (future != null) future.cancel(false);
    }

    public void handleNightAction(String roomId, String deviceId, String targetId) {
        Room room = roomRepository.findById(roomId).orElseThrow();
        if (room.getCurrentPhase() != GamePhase.NIGHT) return;
        
        Player actor = room.getPlayers().stream().filter(p -> p.getDeviceId().equals(deviceId)).findFirst().orElse(null);
        if (actor == null || !actor.isAlive()) return;
        
        if (actor.getRole() == Role.BODYGUARD && targetId.equals(room.getPreviousProtectedPlayerId())) {
            throw new IllegalArgumentException("Cannot protect the same player twice in a row");
        }
        
        room.getNightActions().put(deviceId, targetId);
        roomRepository.save(room);
    }

    public void handleDayVote(String roomId, String deviceId, String targetId) {
        Room room = roomRepository.findById(roomId).orElseThrow();
        if (room.getCurrentPhase() != GamePhase.DAY_VOTING) return;
        
        Player actor = room.getPlayers().stream().filter(p -> p.getDeviceId().equals(deviceId)).findFirst().orElse(null);
        if (actor == null || !actor.isAlive()) return;
        
        room.getDayVotes().put(deviceId, targetId);
        roomRepository.save(room);
    }

    private void broadcastSystemMessage(String roomId, String text) {
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/chat", 
            Map.of("sender", "System", "text", text, "isSystem", true));
    }
}
