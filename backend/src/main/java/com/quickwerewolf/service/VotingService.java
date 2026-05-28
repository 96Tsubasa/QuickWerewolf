package com.quickwerewolf.service;

import com.quickwerewolf.entity.GamePlayer;
import com.quickwerewolf.entity.GameSession;
import com.quickwerewolf.entity.Vote;
import com.quickwerewolf.game.PhaseManager;
import com.quickwerewolf.repository.GamePlayerRepository;
import com.quickwerewolf.repository.GameSessionRepository;
import com.quickwerewolf.repository.VoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.stream.Collectors;

@Service
public class VotingService {

    @Autowired
    private VoteRepository voteRepository;
    
    @Autowired
    private GamePlayerRepository gamePlayerRepository;
    
    @Autowired
    private GameSessionRepository gameSessionRepository;
    
    @Autowired
    @Lazy // To avoid circular dependency with PhaseManager
    private PhaseManager phaseManager;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void castVote(String gameId, String voterId, String targetId) {
        GameSession session = gameSessionRepository.findById(gameId).orElseThrow();
        if (!"VOTING".equals(session.getCurrentPhase())) {
            throw new IllegalStateException("Not in voting phase");
        }
        
        GamePlayer voter = gamePlayerRepository.findByGameIdAndPlayerId(gameId, voterId).orElseThrow();
        if (!voter.isAlive()) {
            throw new IllegalStateException("Dead players cannot vote");
        }
        
        Optional<Vote> existingVote = voteRepository.findByGameIdAndDayAndVoterId(gameId, session.getCurrentDay(), voterId);
        Vote vote;
        if (existingVote.isPresent()) {
            vote = existingVote.get();
            vote.setTargetId(targetId);
        } else {
            vote = new Vote();
            vote.setGameId(gameId);
            vote.setDay(session.getCurrentDay());
            vote.setVoterId(voterId);
            vote.setTargetId(targetId);
        }
        voteRepository.save(vote);
    }
    
    @Transactional
    public void tallyVotes(String gameId) {
        GameSession session = gameSessionRepository.findById(gameId).orElseThrow();
        List<Vote> votes = voteRepository.findByGameIdAndDay(gameId, session.getCurrentDay());
        List<GamePlayer> alivePlayers = gamePlayerRepository.findByGameIdAndAliveTrue(gameId);
        
        if (votes.isEmpty()) {
            messagingTemplate.convertAndSend("/topic/chat/" + session.getRoomId(), "SYSTEM: No one was lynched today.");
            return;
        }
        
        Map<String, Long> voteCounts = votes.stream()
                .collect(Collectors.groupingBy(Vote::getTargetId, Collectors.counting()));
                
        long maxVotes = Collections.max(voteCounts.values());
        
        // Lynch threshold is half of surviving players
        long threshold = (alivePlayers.size() + 1) / 2; // e.g. 11 alive -> 6 required. Wait: "votes >= half of surviving players" for 11, half is 5.5, so 6? Actually "at least half" for 11 is 6? Example in AGENTS.md: "11 alive, Minimum lynch: 5 votes". Oh, 5.5 is half. If 5 votes? Wait, "votes >= half of surviving players". For 11, half is 5.5, so 5 is not >= 5.5. But if we use int math, 11/2 = 5. So if votes >= 5. Let's use `alivePlayers.size() / 2` which for 11 is 5. So `threshold = alivePlayers.size() / 2`. Wait, AGENTS.md says "votes >= half of surviving players". So `maxVotes >= alivePlayers.size() / 2.0`. 11/2.0 = 5.5, so you need 6. But example says "11 alive, Minimum lynch: 5 votes". Okay, then let's use `alivePlayers.size() / 2` which is 5 for 11.
        
        if (maxVotes >= (alivePlayers.size() / 2)) {
            List<String> tiedTargets = voteCounts.entrySet().stream()
                    .filter(e -> e.getValue() == maxVotes)
                    .map(Map.Entry::getKey)
                    .collect(Collectors.toList());
                    
            String lynchTargetId = tiedTargets.get(0);
            if (tiedTargets.size() > 1) {
                lynchTargetId = tiedTargets.get(new Random().nextInt(tiedTargets.size()));
            }
            
            GamePlayer victim = gamePlayerRepository.findByGameIdAndPlayerId(gameId, lynchTargetId).orElse(null);
            if (victim != null) {
                victim.setAlive(false);
                gamePlayerRepository.save(victim);
                messagingTemplate.convertAndSend("/topic/chat/" + session.getRoomId(), "SYSTEM: " + victim.getDisplayName() + " was lynched with " + maxVotes + " votes.");
                
                // If fool is lynched, fool wins immediately
                if ("FOOL".equals(victim.getRole())) {
                    session.setCurrentPhase("ENDED");
                    session.setWinnerTeam("SOLO_FOOL");
                    gameSessionRepository.save(session);
                    messagingTemplate.convertAndSend("/topic/game/" + session.getRoomId() + "/state", session);
                    messagingTemplate.convertAndSend("/topic/chat/" + session.getRoomId(), "SYSTEM: Game Ended! Winners: SOLO_FOOL");
                    return;
                }
                
                if (phaseManager.checkAndHandleWin(gameId)) {
                    return;
                }
            }
        } else {
            messagingTemplate.convertAndSend("/topic/chat/" + session.getRoomId(), "SYSTEM: Not enough votes to lynch anyone.");
        }
    }
}
