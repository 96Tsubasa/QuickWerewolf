package com.quickwerewolf.service;

import com.quickwerewolf.domain.role.RoleType;
import com.quickwerewolf.entity.ChatMessage;
import com.quickwerewolf.entity.GamePlayer;
import com.quickwerewolf.entity.GameSession;
import com.quickwerewolf.entity.Room;
import com.quickwerewolf.repository.ChatMessageRepository;
import com.quickwerewolf.repository.GamePlayerRepository;
import com.quickwerewolf.repository.GameSessionRepository;
import com.quickwerewolf.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {

    @Autowired
    private ChatMessageRepository chatMessageRepository;
    
    @Autowired
    private GameSessionRepository gameSessionRepository;
    
    @Autowired
    private GamePlayerRepository gamePlayerRepository;
    
    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public void sendMessage(String roomId, String senderId, String chatType, String receiverId, String message) {
        Room room = roomRepository.findByRoomCode(roomId).orElseThrow();
        GameSession session = gameSessionRepository.findByRoomId(roomId).orElse(null);
        
        if (session != null && "PLAYING".equals(room.getStatus())) {
            GamePlayer player = gamePlayerRepository.findByGameIdAndPlayerId(session.getId(), senderId).orElse(null);
            
            if (player != null && !player.isAlive()) {
                throw new IllegalStateException("Dead players cannot send messages");
            }
            
            if ("PUBLIC".equals(chatType) && !"DISCUSSION".equals(session.getCurrentPhase())) {
                throw new IllegalStateException("Public chat is only available during discussion");
            }
            
            if ("WEREWOLF".equals(chatType)) {
                if (player == null || !RoleType.WEREWOLF.name().equals(player.getRole())) {
                    throw new IllegalStateException("Only werewolves can use werewolf chat");
                }
                if (!"NIGHT".equals(session.getCurrentPhase())) {
                    throw new IllegalStateException("Werewolf chat is only available at night");
                }
            }
        }
        
        ChatMessage chatMsg = new ChatMessage();
        chatMsg.setGameId(session != null ? session.getId() : roomId);
        chatMsg.setSenderId(senderId);
        chatMsg.setChatType(chatType);
        chatMsg.setReceiverId(receiverId);
        chatMsg.setMessage(message);
        chatMessageRepository.save(chatMsg);
        
        if ("PUBLIC".equals(chatType)) {
            messagingTemplate.convertAndSend("/topic/chat/" + room.getRoomCode(), chatMsg);
        } else if ("WEREWOLF".equals(chatType)) {
            messagingTemplate.convertAndSend("/topic/chat/" + room.getRoomCode() + "/werewolf", chatMsg);
        } else if ("PRIVATE_HOST".equals(chatType)) {
            // Send to the receiver and the host
            messagingTemplate.convertAndSend("/topic/chat/" + room.getRoomCode() + "/private/" + receiverId, chatMsg);
            if (!senderId.equals(room.getHostPlayerId()) && !receiverId.equals(room.getHostPlayerId())) {
                 messagingTemplate.convertAndSend("/topic/chat/" + room.getRoomCode() + "/private/" + room.getHostPlayerId(), chatMsg);
            }
        }
    }
}
