package com.quickwerewolf.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "chat_messages")
@Data
public class ChatMessage {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "game_id", nullable = false)
    private String gameId;

    @Column(name = "sender_id", nullable = false)
    private String senderId;

    @Column(name = "chat_type", nullable = false)
    private String chatType; // "PUBLIC", "WEREWOLF", "PRIVATE_HOST"

    @Column(name = "receiver_id")
    private String receiverId; // Used if PRIVATE_HOST to specify the specific player ID

    @Column(name = "message", nullable = false)
    private String message;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
