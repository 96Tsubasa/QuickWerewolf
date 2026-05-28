package com.quickwerewolf.dto;

import lombok.Data;
import java.util.List;
import java.util.Map;
import com.quickwerewolf.domain.GamePhase;
import com.quickwerewolf.domain.Role;

@Data
public class RoomStateDto {
    private String roomId;
    private List<PlayerDto> players;
    private int maxPlayers;
    private GamePhase currentPhase;
    private int dayNumber;
    private int nightNumber;
    private Map<Role, Integer> roleCounts;
    private boolean hostPlays;
    private long phaseEndTime;
    private String previousProtectedPlayerId;
}
