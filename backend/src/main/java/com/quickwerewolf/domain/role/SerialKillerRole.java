package com.quickwerewolf.domain.role;

public class SerialKillerRole implements Role {
    @Override
    public RoleType getType() { return RoleType.SERIAL_KILLER; }
    
    @Override
    public Team getTeam() { return Team.SOLO_SK; }

    @Override
    public boolean hasNightAction() { return true; }
}
