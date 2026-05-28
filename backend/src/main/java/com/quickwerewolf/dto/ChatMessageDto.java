package com.quickwerewolf.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessageDto {
    private String senderId;
    private String senderName;
    private String content;
    private String type; // PUBLIC, SYSTEM, etc.
    private LocalDateTime timestamp = LocalDateTime.now();
}
