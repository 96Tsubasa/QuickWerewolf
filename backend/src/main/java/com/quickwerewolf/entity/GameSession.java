package com.quickwerewolf.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_sessions")
@Data
public class GameSession {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "room_id", nullable = false)
    private String roomId;

    @Column(name = "current_phase")
    private String currentPhase; // e.g., "NIGHT", "DISCUSSION", "VOTING", "ENDED"

    @Column(name = "current_day")
    private int currentDay;

    @Column(name = "phase_started_at")
    private LocalDateTime phaseStartedAt;

    @Column(name = "phase_end_at")
    private LocalDateTime phaseEndAt;

    @Column(name = "winner_team")
    private String winnerTeam; // "VILLAGE", "WEREWOLF", "SOLO_SK", "SOLO_FOOL", etc.
}
