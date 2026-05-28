package com.quickwerewolf.dto;

import lombok.Data;
import java.util.List;

@Data
public class RoomStateDto {
    private String roomCode;
    private String status;
    private String hostPlayerId;
    private List<PlayerDto> players;
}
