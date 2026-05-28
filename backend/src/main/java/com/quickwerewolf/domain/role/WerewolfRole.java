package com.quickwerewolf.domain.role;

public class WerewolfRole implements Role {
    @Override
    public RoleType getType() { return RoleType.WEREWOLF; }
    
    @Override
    public Team getTeam() { return Team.WEREWOLF; }

    @Override
    public boolean hasNightAction() { return true; }
}
