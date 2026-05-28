package com.quickwerewolf.service;

import com.quickwerewolf.domain.Player;
import com.quickwerewolf.domain.Room;
import com.quickwerewolf.domain.GamePhase;
import com.quickwerewolf.domain.Role;
import com.quickwerewolf.dto.PlayerDto;
import com.quickwerewolf.dto.RoomStateDto;
import com.quickwerewolf.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    public RoomStateDto createRoom(String deviceId, String displayName) {
        String roomId = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        
        Room room = new Room();
        room.setRoomId(roomId);
        room.setCurrentPhase(GamePhase.LOBBY);
        
        Player host = new Player(deviceId, displayName, null, true, true, false);
        room.getPlayers().add(host);
        
        roomRepository.save(room);
        
        return getRoomState(roomId);
    }

    public RoomStateDto joinRoom(String roomId, String deviceId, String displayName) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if (room.getCurrentPhase() == GamePhase.ENDED) {
            throw new IllegalStateException("Room has ended");
        }

        Optional<Player> existingPlayerOpt = room.getPlayers().stream()
                .filter(p -> p.getDeviceId().equals(deviceId))
                .findFirst();
        
        if (existingPlayerOpt.isPresent()) {
            Player player = existingPlayerOpt.get();
            player.setDisplayName(displayName);
            player.setHasDisconnected(false);
        } else {
            if (room.getCurrentPhase() != GamePhase.LOBBY) {
                throw new IllegalStateException("Game has already started");
            }
            
            if (room.getPlayers().size() >= room.getMaxPlayers()) {
                throw new IllegalStateException("Room is full");
            }
            
            Player newPlayer = new Player(deviceId, displayName, null, false, true, false);
            room.getPlayers().add(newPlayer);
        }
        
        roomRepository.save(room);
        
        RoomStateDto state = getRoomState(roomId);
        broadcastRoomState(roomId, state);
        return state;
    }
    
    public void kickPlayer(String roomId, String hostDeviceId, String targetDeviceId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
                
        boolean isHost = room.getPlayers().stream()
                .anyMatch(p -> p.getDeviceId().equals(hostDeviceId) && p.isHost());
                
        if (!isHost) {
            throw new IllegalStateException("Only host can kick players");
        }
        
        if (hostDeviceId.equals(targetDeviceId)) {
            throw new IllegalStateException("Host cannot kick themselves");
        }
        
        room.getPlayers().removeIf(p -> p.getDeviceId().equals(targetDeviceId));
        roomRepository.save(room);
        
        RoomStateDto state = getRoomState(roomId);
        broadcastRoomState(roomId, state);
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/kicked", targetDeviceId);
    }
    
    public void closeRoom(String roomId, String hostDeviceId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
                
        boolean isHost = room.getPlayers().stream()
                .anyMatch(p -> p.getDeviceId().equals(hostDeviceId) && p.isHost());
                
        if (!isHost) {
            throw new IllegalStateException("Only host can close room");
        }
        
        room.setCurrentPhase(GamePhase.ENDED);
        roomRepository.save(room);
        
        RoomStateDto state = getRoomState(roomId);
        broadcastRoomState(roomId, state);
    }
    
    public void quitRoom(String roomId, String deviceId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
                
        Optional<Player> playerOpt = room.getPlayers().stream()
                .filter(p -> p.getDeviceId().equals(deviceId))
                .findFirst();
        
        if (playerOpt.isPresent()) {
            if (room.getCurrentPhase() != GamePhase.LOBBY) {
                // Just mark disconnected
                playerOpt.get().setHasDisconnected(true);
            } else {
                // Remove from lobby
                room.getPlayers().remove(playerOpt.get());
                
                // If host quits, assign new host or close room
                if (playerOpt.get().isHost()) {
                    if (room.getPlayers().isEmpty()) {
                        room.setCurrentPhase(GamePhase.ENDED);
                    } else {
                        room.getPlayers().get(0).setHost(true);
                    }
                }
            }
            
            roomRepository.save(room);
            
            RoomStateDto state = getRoomState(roomId);
            broadcastRoomState(roomId, state);
        }
    }
    
    public RoomStateDto getRoomState(String roomId) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
                
        RoomStateDto state = new RoomStateDto();
        state.setRoomId(room.getRoomId());
        state.setMaxPlayers(room.getMaxPlayers());
        state.setCurrentPhase(room.getCurrentPhase());
        state.setDayNumber(room.getDayNumber());
        state.setNightNumber(room.getNightNumber());
        state.setRoleCounts(room.getRoleCounts());
        state.setHostPlays(room.isHostPlays());
        state.setPhaseEndTime(room.getPhaseEndTime());
        
        List<PlayerDto> playerDtos = room.getPlayers().stream().map(p -> {
            PlayerDto dto = new PlayerDto();
            dto.setDeviceId(p.getDeviceId());
            dto.setDisplayName(p.getDisplayName());
            dto.setRole(p.getRole());
            dto.setHost(p.isHost());
            dto.setAlive(p.isAlive());
            dto.setHasDisconnected(p.isHasDisconnected());
            return dto;
        }).collect(Collectors.toList());
        
        state.setPlayers(playerDtos);
        return state;
    }

    public void updateRoomSettings(String roomId, String hostDeviceId, int maxPlayers, boolean hostPlays, HashMap<Role, Integer> roleCounts) {
        Room room = roomRepository.findById(roomId)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
                
        boolean isHost = room.getPlayers().stream()
                .anyMatch(p -> p.getDeviceId().equals(hostDeviceId) && p.isHost());
                
        if (!isHost) {
            throw new IllegalStateException("Only host can change settings");
        }
        
        if (room.getCurrentPhase() != GamePhase.LOBBY) {
            throw new IllegalStateException("Cannot change settings after game started");
        }
        
        if (maxPlayers < room.getPlayers().size()) {
            throw new IllegalStateException("Max players cannot be less than current players");
        }
        
        int totalRoles = roleCounts.values().stream().mapToInt(Integer::intValue).sum();
        if (totalRoles > maxPlayers) {
            throw new IllegalStateException("Total roles cannot exceed max players");
        }
        
        room.setMaxPlayers(maxPlayers);
        room.setHostPlays(hostPlays);
        room.setRoleCounts(roleCounts);
        roomRepository.save(room);
        
        RoomStateDto state = getRoomState(roomId);
        broadcastRoomState(roomId, state);
    }

    public void broadcastRoomState(String roomId, RoomStateDto state) {
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/state", state);
    }
}
