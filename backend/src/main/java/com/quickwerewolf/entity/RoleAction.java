package com.quickwerewolf.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "role_actions")
@Data
public class RoleAction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "game_id", nullable = false)
    private String gameId;

    @Column(name = "actor_id", nullable = false)
    private String actorId;

    @Column(name = "target_id")
    private String targetId;

    @Column(name = "action_type")
    private String actionType; // "KILL", "PROTECT", "INSPECT"

    @Column(name = "night_number")
    private int nightNumber;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();
}
