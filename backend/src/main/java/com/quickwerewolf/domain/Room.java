package com.quickwerewolf.domain;

import lombok.Data;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

@Data
@RedisHash("Room")
public class Room {
    @Id
    private String roomId;
    private List<Player> players = new ArrayList<>();
    private int maxPlayers = 16;
    private GamePhase currentPhase = GamePhase.LOBBY;
    private int dayNumber = 0;
    private int nightNumber = 0;
    private Map<Role, Integer> roleCounts = new HashMap<>();
    private Map<String, String> nightActions = new HashMap<>(); // deviceId -> target deviceId
    private Map<String, String> dayVotes = new HashMap<>(); // deviceId -> target deviceId
    private String previousProtectedPlayerId;
    private boolean hostPlays = true;
    private long phaseEndTime; // Timestamp in ms for countdowns
}
