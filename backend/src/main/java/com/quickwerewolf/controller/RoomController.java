package com.quickwerewolf.controller;

import com.quickwerewolf.dto.RoomStateDto;
import com.quickwerewolf.service.RoomService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/rooms")
@CrossOrigin(origins = "*") // For development
public class RoomController {

    @Autowired
    private RoomService roomService;

    @PostMapping
    public ResponseEntity<RoomStateDto> createRoom(@RequestBody Map<String, String> payload) {
        String displayName = payload.get("displayName");
        String deviceId = payload.get("deviceId");
        
        RoomStateDto state = roomService.createRoom(deviceId, displayName);
        return ResponseEntity.ok(state);
    }

    @PostMapping("/{roomCode}/join")
    public ResponseEntity<RoomStateDto> joinRoom(@PathVariable String roomCode, @RequestBody Map<String, String> payload) {
        String displayName = payload.get("displayName");
        String deviceId = payload.get("deviceId");
        
        RoomStateDto state = roomService.joinRoom(roomCode, deviceId, displayName);
        return ResponseEntity.ok(state);
    }
    
    @PostMapping("/{roomCode}/kick")
    public ResponseEntity<Void> kickPlayer(@PathVariable String roomCode, @RequestBody Map<String, String> payload) {
        String hostDeviceId = payload.get("hostDeviceId");
        String targetDeviceId = payload.get("targetDeviceId");
        
        roomService.kickPlayer(roomCode, hostDeviceId, targetDeviceId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{roomCode}/close")
    public ResponseEntity<Void> closeRoom(@PathVariable String roomCode, @RequestBody Map<String, String> payload) {
        String hostDeviceId = payload.get("hostDeviceId");
        
        roomService.closeRoom(roomCode, hostDeviceId);
        return ResponseEntity.ok().build();
    }
    
    @PostMapping("/{roomCode}/quit")
    public ResponseEntity<Void> quitRoom(@PathVariable String roomCode, @RequestBody Map<String, String> payload) {
        String deviceId = payload.get("deviceId");
        
        roomService.quitRoom(roomCode, deviceId);
        return ResponseEntity.ok().build();
    }
    
    @GetMapping("/{roomCode}")
    public ResponseEntity<RoomStateDto> getRoom(@PathVariable String roomCode) {
        RoomStateDto state = roomService.getRoomState(roomCode);
        return ResponseEntity.ok(state);
    }
    
    @Autowired
    private com.quickwerewolf.service.GameService gameService;

    @PostMapping("/{roomCode}/start")
    public ResponseEntity<Void> startGame(@PathVariable String roomCode, @RequestBody Map<String, Object> payload) {
        String hostDeviceId = (String) payload.get("hostDeviceId");
        List<String> roleStrings = (List<String>) payload.get("selectedRoles");
        List<com.quickwerewolf.domain.role.RoleType> selectedRoles = new java.util.ArrayList<>();
        if (roleStrings != null) {
            for (String r : roleStrings) {
                selectedRoles.add(com.quickwerewolf.domain.role.RoleType.valueOf(r));
            }
        }
        
        gameService.startGame(roomCode, hostDeviceId, selectedRoles);
        return ResponseEntity.ok().build();
    }
}
