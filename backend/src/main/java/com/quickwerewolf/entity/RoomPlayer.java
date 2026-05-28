package com.quickwerewolf.entity;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "room_players", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"room_id", "device_id"})
})
@Data
public class RoomPlayer {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    @JsonIgnore
    private Room room;
    
    @Column(name = "device_id", nullable = false)
    private String deviceId;
    
    @Column(name = "display_name", nullable = false)
    private String displayName;
    
    @Column(name = "is_host", nullable = false)
    private boolean isHost = false;
    
    @Column(name = "connected", nullable = false)
    private boolean connected = true;
    
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt = LocalDateTime.now();
}
