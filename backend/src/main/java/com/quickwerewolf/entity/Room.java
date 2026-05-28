package com.quickwerewolf.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "rooms")
@Data
public class Room {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "room_code", unique = true, nullable = false, length = 10)
    private String roomCode;
    
    @Column(name = "host_player_id")
    private String hostPlayerId; // Maps to deviceId
    
    @Column(name = "status", nullable = false)
    private String status = "WAITING"; // WAITING, PLAYING, ENDED
    
    @Column(name = "max_players", nullable = false)
    private Integer maxPlayers = 16;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt = LocalDateTime.now();
    
    @Column(name = "started_at")
    private LocalDateTime startedAt;

    @OneToMany(mappedBy = "room", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RoomPlayer> players = new ArrayList<>();
}
