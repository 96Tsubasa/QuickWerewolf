package com.quickwerewolf.dto;

import lombok.Data;

@Data
public class PlayerDto {
    private String deviceId;
    private String displayName;
    private boolean isHost;
    private boolean connected;
}
