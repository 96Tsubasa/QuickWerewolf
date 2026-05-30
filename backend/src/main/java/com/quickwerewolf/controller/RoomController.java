package com.quickwerewolf.controller;

import com.quickwerewolf.dto.RoomStateDto;
import com.quickwerewolf.service.RoomService;
import com.quickwerewolf.domain.Role;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.HashMap;

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

    @PostMapping("/{roomId}/join")
    public ResponseEntity<RoomStateDto> joinRoom(@PathVariable String roomId,
            @RequestBody Map<String, String> payload) {
        String displayName = payload.get("displayName");
        String deviceId = payload.get("deviceId");

        RoomStateDto state = roomService.joinRoom(roomId, deviceId, displayName);
        return ResponseEntity.ok(state);
    }

    @PostMapping("/{roomId}/settings")
    public ResponseEntity<Void> updateSettings(@PathVariable String roomId, @RequestBody Map<String, Object> payload) {
        String hostDeviceId = (String) payload.get("hostDeviceId");
        int maxPlayers = (Integer) payload.get("maxPlayers");
        boolean hostPlays = (Boolean) payload.get("hostPlays");

        Map<String, Integer> rawRoleCounts = (Map<String, Integer>) payload.get("roleCounts");
        HashMap<Role, Integer> roleCounts = new HashMap<>();
        if (rawRoleCounts != null) {
            for (Map.Entry<String, Integer> entry : rawRoleCounts.entrySet()) {
                roleCounts.put(Role.valueOf(entry.getKey()), entry.getValue());
            }
        }

        roomService.updateRoomSettings(roomId, hostDeviceId, maxPlayers, hostPlays, roleCounts);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/phase-durations")
    public ResponseEntity<Void> updatePhaseDurations(@PathVariable String roomId,
            @RequestBody Map<String, Object> payload) {
        String hostDeviceId = (String) payload.get("hostDeviceId");

        Map<String, Long> phaseDurations = new HashMap<>();
        Map<String, Object> rawDurations = (Map<String, Object>) payload.get("phaseDurations");
        if (rawDurations != null) {
            for (Map.Entry<String, Object> entry : rawDurations.entrySet()) {
                phaseDurations.put(entry.getKey(), ((Number) entry.getValue()).longValue());
            }
        }

        roomService.updatePhaseDurations(roomId, hostDeviceId, phaseDurations);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/kick")
    public ResponseEntity<Void> kickPlayer(@PathVariable String roomId, @RequestBody Map<String, String> payload) {
        String hostDeviceId = payload.get("hostDeviceId");
        String targetDeviceId = payload.get("targetDeviceId");

        roomService.kickPlayer(roomId, hostDeviceId, targetDeviceId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/close")
    public ResponseEntity<Void> closeRoom(@PathVariable String roomId, @RequestBody Map<String, String> payload) {
        String hostDeviceId = payload.get("hostDeviceId");

        roomService.closeRoom(roomId, hostDeviceId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/end")
    public ResponseEntity<Void> endGame(@PathVariable String roomId, @RequestBody Map<String, String> payload) {
        String hostDeviceId = payload.get("hostDeviceId");

        roomService.closeRoom(roomId, hostDeviceId);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{roomId}/quit")
    public ResponseEntity<Void> quitRoom(@PathVariable String roomId, @RequestBody Map<String, String> payload) {
        String deviceId = payload.get("deviceId");

        roomService.quitRoom(roomId, deviceId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{roomId}")
    public ResponseEntity<RoomStateDto> getRoom(@PathVariable String roomId) {
        RoomStateDto state = roomService.getRoomState(roomId);
        return ResponseEntity.ok(state);
    }
}
