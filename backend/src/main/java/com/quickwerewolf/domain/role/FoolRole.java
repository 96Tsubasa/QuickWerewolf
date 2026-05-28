package com.quickwerewolf.domain.role;

public class FoolRole implements Role {
    @Override
    public RoleType getType() { return RoleType.FOOL; }
    
    @Override
    public Team getTeam() { return Team.SOLO_FOOL; }

    @Override
    public boolean hasNightAction() { return false; }
}
