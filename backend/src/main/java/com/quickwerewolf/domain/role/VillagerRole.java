package com.quickwerewolf.domain.role;

public class VillagerRole implements Role {
    @Override
    public RoleType getType() { return RoleType.VILLAGER; }
    
    @Override
    public Team getTeam() { return Team.VILLAGE; }

    @Override
    public boolean hasNightAction() { return false; }
}
