package com.quickwerewolf.domain.role;

public class SeerRole implements Role {
    @Override
    public RoleType getType() { return RoleType.SEER; }
    
    @Override
    public Team getTeam() { return Team.VILLAGE; }

    @Override
    public boolean hasNightAction() { return true; }
}
