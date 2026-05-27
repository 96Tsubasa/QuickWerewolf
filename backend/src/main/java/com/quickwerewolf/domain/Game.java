package com.quickwerewolf.domain;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class Game {
    private String id;
    private String roomId;
    private GamePhase currentPhase;
    private int currentDay;
    private LocalDateTime phaseStartedAt;
    
    public enum GamePhase {
        NIGHT,
        DISCUSSION,
        VOTING,
        ENDED
    }
}
