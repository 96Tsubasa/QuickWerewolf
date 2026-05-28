package com.quickwerewolf.domain;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Player {
    private String deviceId;
    private String displayName;
    private Role role;
    private boolean isHost;
    private boolean isAlive;
    private boolean hasDisconnected;
}
