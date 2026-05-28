package com.quickwerewolf.domain.role;

public class BodyguardRole implements Role {
    @Override
    public RoleType getType() { return RoleType.BODYGUARD; }
    
    @Override
    public Team getTeam() { return Team.VILLAGE; }

    @Override
    public boolean hasNightAction() { return true; }
}
