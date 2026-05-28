package com.quickwerewolf.controller;

import com.quickwerewolf.entity.RoleAction;
import com.quickwerewolf.repository.GameSessionRepository;
import com.quickwerewolf.repository.RoleActionRepository;
import com.quickwerewolf.service.ChatService;
import com.quickwerewolf.service.VotingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class GameWebSocketController {

    @Autowired
    private ChatService chatService;
    
    @Autowired
    private VotingService votingService;
    
    @Autowired
    private RoleActionRepository roleActionRepository;

    @Autowired
    private GameSessionRepository gameSessionRepository;

    @MessageMapping("/chat/{roomId}/send")
    public void sendMessage(@DestinationVariable String roomId, Map<String, String> payload) {
        String senderId = payload.get("senderId");
        String chatType = payload.get("chatType");
        String receiverId = payload.get("receiverId");
        String message = payload.get("message");
        
        chatService.sendMessage(roomId, senderId, chatType, receiverId, message);
    }
    
    @MessageMapping("/game/{gameId}/action")
    public void submitAction(@DestinationVariable String gameId, Map<String, String> payload) {
        String actorId = payload.get("actorId");
        String targetId = payload.get("targetId");
        String actionType = payload.get("actionType");
        
        int currentDay = gameSessionRepository.findById(gameId).orElseThrow().getCurrentDay();
        
        RoleAction action = new RoleAction();
        action.setGameId(gameId);
        action.setActorId(actorId);
        action.setTargetId(targetId);
        action.setActionType(actionType);
        action.setNightNumber(currentDay);
        
        roleActionRepository.save(action);
    }

    @MessageMapping("/game/{gameId}/vote")
    public void submitVote(@DestinationVariable String gameId, Map<String, String> payload) {
        String voterId = payload.get("voterId");
        String targetId = payload.get("targetId");
        
        votingService.castVote(gameId, voterId, targetId);
    }
}
