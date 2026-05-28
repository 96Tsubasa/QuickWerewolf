package com.quickwerewolf.service;

import com.quickwerewolf.dto.PlayerDto;
import com.quickwerewolf.dto.RoomStateDto;
import com.quickwerewolf.entity.Room;
import com.quickwerewolf.entity.RoomPlayer;
import com.quickwerewolf.repository.RoomPlayerRepository;
import com.quickwerewolf.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoomService {

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private RoomPlayerRepository roomPlayerRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Transactional
    public RoomStateDto createRoom(String deviceId, String displayName) {
        String roomCode = UUID.randomUUID().toString().substring(0, 6).toUpperCase();
        
        Room room = new Room();
        room.setRoomCode(roomCode);
        room.setHostPlayerId(deviceId);
        room = roomRepository.save(room);
        
        RoomPlayer host = new RoomPlayer();
        host.setRoom(room);
        host.setDeviceId(deviceId);
        host.setDisplayName(displayName);
        host.setHost(true);
        host.setConnected(true);
        roomPlayerRepository.save(host);
        
        return getRoomState(roomCode);
    }

    @Transactional
    public RoomStateDto joinRoom(String roomCode, String deviceId, String displayName) {
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));

        if ("ENDED".equals(room.getStatus())) {
            throw new IllegalStateException("Room has ended");
        }

        Optional<RoomPlayer> existingPlayer = roomPlayerRepository.findByRoomIdAndDeviceId(room.getId(), deviceId);
        
        if (existingPlayer.isPresent()) {
            RoomPlayer player = existingPlayer.get();
            player.setDisplayName(displayName);
            player.setConnected(true);
            roomPlayerRepository.save(player);
        } else {
            if ("PLAYING".equals(room.getStatus())) {
                throw new IllegalStateException("Game has already started");
            }
            
            long playerCount = roomPlayerRepository.findByRoomId(room.getId()).size();
            if (playerCount >= room.getMaxPlayers()) {
                throw new IllegalStateException("Room is full");
            }
            
            RoomPlayer newPlayer = new RoomPlayer();
            newPlayer.setRoom(room);
            newPlayer.setDeviceId(deviceId);
            newPlayer.setDisplayName(displayName);
            newPlayer.setHost(false);
            newPlayer.setConnected(true);
            roomPlayerRepository.save(newPlayer);
        }
        
        RoomStateDto state = getRoomState(roomCode);
        broadcastRoomState(roomCode, state);
        return state;
    }
    
    @Transactional
    public void kickPlayer(String roomCode, String hostDeviceId, String targetDeviceId) {
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
                
        if (!room.getHostPlayerId().equals(hostDeviceId)) {
            throw new IllegalStateException("Only host can kick players");
        }
        
        if (hostDeviceId.equals(targetDeviceId)) {
            throw new IllegalStateException("Host cannot kick themselves");
        }
        
        Optional<RoomPlayer> targetPlayer = roomPlayerRepository.findByRoomIdAndDeviceId(room.getId(), targetDeviceId);
        if (targetPlayer.isPresent()) {
            roomPlayerRepository.delete(targetPlayer.get());
            RoomStateDto state = getRoomState(roomCode);
            broadcastRoomState(roomCode, state);
            
            // Notify the kicked player specifically (optional, they can also just receive the state update and see they are missing)
            messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/kicked", targetDeviceId);
        }
    }
    
    @Transactional
    public void closeRoom(String roomCode, String hostDeviceId) {
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
                
        if (!room.getHostPlayerId().equals(hostDeviceId)) {
            throw new IllegalStateException("Only host can close room");
        }
        
        room.setStatus("ENDED");
        roomRepository.save(room);
        
        RoomStateDto state = getRoomState(roomCode);
        broadcastRoomState(roomCode, state);
    }
    
    @Transactional
    public void quitRoom(String roomCode, String deviceId) {
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
                
        Optional<RoomPlayer> playerOpt = roomPlayerRepository.findByRoomIdAndDeviceId(room.getId(), deviceId);
        
        if (playerOpt.isPresent()) {
            if ("PLAYING".equals(room.getStatus())) {
                // Just mark disconnected
                RoomPlayer player = playerOpt.get();
                player.setConnected(false);
                roomPlayerRepository.save(player);
            } else {
                // Remove from lobby
                roomPlayerRepository.delete(playerOpt.get());
                
                // If host quits, assign new host or close room
                if (room.getHostPlayerId().equals(deviceId)) {
                    List<RoomPlayer> remaining = roomPlayerRepository.findByRoomId(room.getId());
                    if (remaining.isEmpty()) {
                        room.setStatus("ENDED");
                        roomRepository.save(room);
                    } else {
                        RoomPlayer newHost = remaining.get(0);
                        newHost.setHost(true);
                        room.setHostPlayerId(newHost.getDeviceId());
                        roomPlayerRepository.save(newHost);
                        roomRepository.save(room);
                    }
                }
            }
            
            RoomStateDto state = getRoomState(roomCode);
            broadcastRoomState(roomCode, state);
        }
    }
    
    @Transactional
    public void handleDisconnect(String deviceId) {
        // Find all rooms this player is in and mark them disconnected
        // This would require a more complex query if players can be in multiple rooms,
        // but for now let's assume they are only actively connected to one.
        // We will need a way to track session ID to device ID mapping.
    }

    public RoomStateDto getRoomState(String roomCode) {
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
                
        List<RoomPlayer> players = roomPlayerRepository.findByRoomId(room.getId());
        
        RoomStateDto state = new RoomStateDto();
        state.setRoomCode(room.getRoomCode());
        state.setStatus(room.getStatus());
        state.setHostPlayerId(room.getHostPlayerId());
        
        List<PlayerDto> playerDtos = players.stream().map(p -> {
            PlayerDto dto = new PlayerDto();
            dto.setDeviceId(p.getDeviceId());
            dto.setDisplayName(p.getDisplayName());
            dto.setHost(p.isHost());
            dto.setConnected(p.isConnected());
            return dto;
        }).collect(Collectors.toList());
        
        state.setPlayers(playerDtos);
        return state;
    }

    public void broadcastRoomState(String roomCode, RoomStateDto state) {
        messagingTemplate.convertAndSend("/topic/room/" + roomCode + "/state", state);
    }
}
