package com.quickwerewolf.game;

import com.quickwerewolf.domain.role.RoleType;
import com.quickwerewolf.domain.role.Team;
import com.quickwerewolf.entity.GamePlayer;
import com.quickwerewolf.entity.RoleAction;
import com.quickwerewolf.repository.GamePlayerRepository;
import com.quickwerewolf.repository.RoleActionRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class ActionResolver {

    @Autowired
    private RoleActionRepository roleActionRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;
    
    @Autowired
    private WinChecker winChecker;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public List<GamePlayer> resolveNightActions(String gameId, int nightNumber) {
        List<RoleAction> actions = roleActionRepository.findByGameIdAndNightNumber(gameId, nightNumber);
        List<GamePlayer> alivePlayers = gamePlayerRepository.findByGameIdAndAliveTrue(gameId);
        Map<String, GamePlayer> playerMap = alivePlayers.stream()
                .collect(Collectors.toMap(GamePlayer::getPlayerId, p -> p));
                
        List<GamePlayer> killedPlayers = new ArrayList<>();

        // Reset previous protections for everyone (actually, it should be done after the night or just before setting new ones)
        for (GamePlayer player : alivePlayers) {
            player.setProtectedByBodyguard(false);
        }

        // 1. Resolve Bodyguard
        Optional<RoleAction> bodyguardAction = actions.stream()
                .filter(a -> "PROTECT".equals(a.getActionType()))
                .findFirst();
                
        if (bodyguardAction.isPresent() && playerMap.containsKey(bodyguardAction.get().getTargetId())) {
            GamePlayer target = playerMap.get(bodyguardAction.get().getTargetId());
            target.setProtectedByBodyguard(true);
            
            GamePlayer bodyguard = playerMap.get(bodyguardAction.get().getActorId());
            if (bodyguard != null) {
                bodyguard.setPreviousProtectionTarget(target.getPlayerId());
                gamePlayerRepository.save(bodyguard);
            }
        }

        // 2. Resolve Seer
        Optional<RoleAction> seerAction = actions.stream()
                .filter(a -> "INSPECT".equals(a.getActionType()))
                .findFirst();
                
        if (seerAction.isPresent() && playerMap.containsKey(seerAction.get().getTargetId())) {
            GamePlayer target = playerMap.get(seerAction.get().getTargetId());
            String actorId = seerAction.get().getActorId();
            // Send private message to Seer
            messagingTemplate.convertAndSend("/topic/game/" + gameId + "/private/" + actorId, 
                    "INSPECT_RESULT:" + target.getRole());
        }

        // 3. Resolve Werewolf Kill
        // Handle tied votes from wolves.
        List<RoleAction> wolfActions = actions.stream()
                .filter(a -> "WOLF_KILL".equals(a.getActionType()))
                .collect(Collectors.toList());
                
        String wolfTargetId = getHighestVotedTarget(wolfActions);
        if (wolfTargetId != null && playerMap.containsKey(wolfTargetId)) {
            GamePlayer target = playerMap.get(wolfTargetId);
            // SK is immune to werewolf kill, and Bodyguard protection prevents it.
            if (!RoleType.SERIAL_KILLER.name().equals(target.getRole()) && !target.isProtectedByBodyguard()) {
                target.setAlive(false);
                killedPlayers.add(target);
            }
        }

        // 4. Resolve Serial Killer Kill
        Optional<RoleAction> skAction = actions.stream()
                .filter(a -> "SK_KILL".equals(a.getActionType()))
                .findFirst();
                
        if (skAction.isPresent() && playerMap.containsKey(skAction.get().getTargetId())) {
            GamePlayer target = playerMap.get(skAction.get().getTargetId());
            // Wait, does Bodyguard protect from SK? Yes.
            // But if target was already killed by wolf, no need to kill again.
            if (target.isAlive() && !target.isProtectedByBodyguard()) {
                target.setAlive(false);
                killedPlayers.add(target);
            }
        }

        // Save all changes
        gamePlayerRepository.saveAll(alivePlayers);
        
        return killedPlayers;
    }
    
    private String getHighestVotedTarget(List<RoleAction> actions) {
        if (actions.isEmpty()) return null;
        
        Map<String, Long> votes = actions.stream()
                .collect(Collectors.groupingBy(RoleAction::getTargetId, Collectors.counting()));
                
        long maxVotes = Collections.max(votes.values());
        
        List<String> tiedTargets = votes.entrySet().stream()
                .filter(e -> e.getValue() == maxVotes)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
                
        // Random tie break
        if (tiedTargets.size() > 1) {
            return tiedTargets.get(new Random().nextInt(tiedTargets.size()));
        }
        
        return tiedTargets.get(0);
    }
}
