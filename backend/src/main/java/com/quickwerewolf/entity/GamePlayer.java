package com.quickwerewolf.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "game_players")
@Data
public class GamePlayer {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "game_id", nullable = false)
    private String gameId;

    @Column(name = "player_id", nullable = false) // Device ID or Player identifier
    private String playerId;
    
    @Column(name = "display_name")
    private String displayName;

    @Column(name = "role")
    private String role;

    @Column(name = "team")
    private String team;

    @Column(name = "alive")
    private boolean alive = true;

    @Column(name = "is_protected")
    private boolean protectedByBodyguard = false;

    @Column(name = "previous_protection_target")
    private String previousProtectionTarget;

    @Column(name = "disconnected")
    private boolean disconnected = false;
}
