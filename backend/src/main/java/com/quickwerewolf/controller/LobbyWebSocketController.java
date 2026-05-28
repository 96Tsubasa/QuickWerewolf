package com.quickwerewolf.controller;

import com.quickwerewolf.dto.ChatMessageDto;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;

@Controller
public class LobbyWebSocketController {

    @MessageMapping("/room/{roomCode}/chat")
    @SendTo("/topic/room/{roomCode}/chat")
    public ChatMessageDto sendChat(@DestinationVariable String roomCode, @Payload ChatMessageDto message) {
        return message;
    }
}
