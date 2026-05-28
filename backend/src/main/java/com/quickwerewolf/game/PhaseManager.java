package com.quickwerewolf.game;

import com.quickwerewolf.domain.role.Team;
import com.quickwerewolf.entity.GamePlayer;
import com.quickwerewolf.entity.GameSession;
import com.quickwerewolf.repository.GamePlayerRepository;
import com.quickwerewolf.repository.GameSessionRepository;
import com.quickwerewolf.service.RoomService;
import com.quickwerewolf.service.VotingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PhaseManager {

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @Autowired
    private GamePlayerRepository gamePlayerRepository;
    
    @Autowired
    private ActionResolver actionResolver;
    
    @Autowired
    private WinChecker winChecker;
    
    @Autowired
    private RoomService roomService; // to close room if ended

    @Autowired
    private VotingService votingService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private TaskScheduler taskScheduler;

    // Default durations in seconds
    private static final int NIGHT_DURATION = 30;
    private static final int DISCUSSION_DURATION = 120;
    private static final int VOTING_DURATION = 30;

    public void startNight(String gameId) {
        GameSession session = gameSessionRepository.findById(gameId).orElseThrow();
        session.setCurrentPhase("NIGHT");
        session.setPhaseStartedAt(LocalDateTime.now());
        session.setPhaseEndAt(LocalDateTime.now().plusSeconds(NIGHT_DURATION));
        gameSessionRepository.save(session);
        
        broadcastGameState(session);
        broadcastSystemMessage(session.getRoomId(), "Night " + session.getCurrentDay() + " started");

        taskScheduler.schedule(() -> endNight(gameId), Instant.now().plusSeconds(NIGHT_DURATION));
    }

    private void endNight(String gameId) {
        // Resolve actions
        List<GamePlayer> killedPlayers = actionResolver.resolveNightActions(gameId, gameSessionRepository.findById(gameId).orElseThrow().getCurrentDay());
        
        // Announce deaths
        String roomId = gameSessionRepository.findById(gameId).orElseThrow().getRoomId();
        if (killedPlayers.isEmpty()) {
            broadcastSystemMessage(roomId, "No one died last night.");
        } else {
            String names = killedPlayers.stream().map(GamePlayer::getDisplayName).collect(Collectors.joining(", "));
            broadcastSystemMessage(roomId, "Players died last night: " + names);
        }

        // Check Win
        if (checkAndHandleWin(gameId)) return;

        // Start Discussion
        startDiscussion(gameId);
    }

    public void startDiscussion(String gameId) {
        GameSession session = gameSessionRepository.findById(gameId).orElseThrow();
        session.setCurrentPhase("DISCUSSION");
        session.setPhaseStartedAt(LocalDateTime.now());
        session.setPhaseEndAt(LocalDateTime.now().plusSeconds(DISCUSSION_DURATION));
        gameSessionRepository.save(session);

        broadcastGameState(session);
        broadcastSystemMessage(session.getRoomId(), "Day " + session.getCurrentDay() + " started - Discussion time");

        taskScheduler.schedule(() -> startVoting(gameId), Instant.now().plusSeconds(DISCUSSION_DURATION));
    }

    public void startVoting(String gameId) {
        GameSession session = gameSessionRepository.findById(gameId).orElseThrow();
        session.setCurrentPhase("VOTING");
        session.setPhaseStartedAt(LocalDateTime.now());
        session.setPhaseEndAt(LocalDateTime.now().plusSeconds(VOTING_DURATION));
        gameSessionRepository.save(session);

        broadcastGameState(session);
        broadcastSystemMessage(session.getRoomId(), "Voting time started");

        // The end of voting will be triggered by a VotingService timer or here.
        taskScheduler.schedule(() -> endVoting(gameId), Instant.now().plusSeconds(VOTING_DURATION));
    }
    
    public void endVoting(String gameId) {
        GameSession session = gameSessionRepository.findById(gameId).orElseThrow();
        if ("ENDED".equals(session.getCurrentPhase())) return;
        
        votingService.tallyVotes(gameId);
        
        // Refetch session because tallyVotes might have ended the game
        session = gameSessionRepository.findById(gameId).orElseThrow();
        if ("ENDED".equals(session.getCurrentPhase())) return; 
        
        session.setCurrentDay(session.getCurrentDay() + 1);
        gameSessionRepository.save(session);
        startNight(gameId);
    }
    
    public boolean checkAndHandleWin(String gameId) {
        List<GamePlayer> alivePlayers = gamePlayerRepository.findByGameIdAndAliveTrue(gameId);
        Optional<Team> winner = winChecker.checkWinCondition(alivePlayers);
        
        if (winner.isPresent()) {
            GameSession session = gameSessionRepository.findById(gameId).orElseThrow();
            session.setCurrentPhase("ENDED");
            session.setWinnerTeam(winner.get().name());
            gameSessionRepository.save(session);
            
            broadcastGameState(session);
            broadcastSystemMessage(session.getRoomId(), "Game Ended! Winners: " + winner.get().name());
            return true;
        }
        return false;
    }

    private void broadcastGameState(GameSession session) {
        messagingTemplate.convertAndSend("/topic/game/" + session.getRoomId() + "/state", session);
    }
    
    private void broadcastSystemMessage(String roomId, String message) {
        // Broadcast a chat message from SYSTEM
        messagingTemplate.convertAndSend("/topic/chat/" + roomId, "SYSTEM: " + message);
    }
}
