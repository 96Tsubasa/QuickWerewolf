package com.quickwerewolf.entity;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "votes")
@Data
public class Vote {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "game_id", nullable = false)
    private String gameId;

    @Column(name = "day")
    private int day;

    @Column(name = "voter_id", nullable = false)
    private String voterId;

    @Column(name = "target_id", nullable = false)
    private String targetId;
}
