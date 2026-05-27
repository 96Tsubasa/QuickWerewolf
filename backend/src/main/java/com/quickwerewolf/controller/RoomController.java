package com.quickwerewolf.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*") // For development
public class RoomController {

    @PostMapping
    public ResponseEntity<Map<String, String>> createRoom() {
        String roomCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        // TODO: Save room to database/Redis
        
        Map<String, String> response = new HashMap<>();
        response.put("roomCode", roomCode);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/{roomCode}/join")
    public ResponseEntity<Map<String, String>> joinRoom(@PathVariable String roomCode, @RequestBody Map<String, String> payload) {
        String displayName = payload.get("displayName");
        String deviceId = payload.get("deviceId");
        
        // TODO: Validate room and add player
        
        Map<String, String> response = new HashMap<>();
        response.put("status", "JOINED");
        response.put("roomCode", roomCode);
        return ResponseEntity.ok(response);
    }
}
