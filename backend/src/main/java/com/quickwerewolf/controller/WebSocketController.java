package com.quickwerewolf.controller;

import com.quickwerewolf.service.GameEngineService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.Map;

@Controller
public class WebSocketController {

    @Autowired
    private GameEngineService gameEngineService;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/room/{roomId}/start")
    public void startGame(@DestinationVariable String roomId, @Payload Map<String, String> payload) {
        String hostDeviceId = payload.get("hostDeviceId");
        gameEngineService.startGame(roomId, hostDeviceId);
    }

    @MessageMapping("/room/{roomId}/chat")
    public void handleGlobalChat(@DestinationVariable String roomId, @Payload Map<String, Object> payload) {
        // Forward message to everyone
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/chat", payload);
    }

    @MessageMapping("/room/{roomId}/chat/werewolf")
    public void handleWerewolfChat(@DestinationVariable String roomId, @Payload Map<String, Object> payload) {
        // We broadcast to a specific topic that only werewolves subscribe to
        messagingTemplate.convertAndSend("/topic/room/" + roomId + "/werewolf", payload);
    }

    @MessageMapping("/room/{roomId}/action/night")
    public void handleNightAction(@DestinationVariable String roomId, @Payload Map<String, String> payload) {
        String deviceId = payload.get("deviceId");
        String targetId = payload.get("targetId");

        try {
            gameEngineService.handleNightAction(roomId, deviceId, targetId);
        } catch (IllegalArgumentException e) {
            messagingTemplate.convertAndSend("/topic/room/" + roomId + "/error/" + deviceId, e.getMessage());
        }
    }

    @MessageMapping("/room/{roomId}/action/vote")
    public void handleDayVote(@DestinationVariable String roomId, @Payload Map<String, String> payload) {
        String deviceId = payload.get("deviceId");
        String targetId = payload.get("targetId");

        gameEngineService.handleDayVote(roomId, deviceId, targetId);
    }
}
