package com.quickwerewolf.domain.role;

public interface Role {
    RoleType getType();
    Team getTeam();
    
    // Whether this role can perform a night action
    boolean hasNightAction();
    
    // In a more robust system, executeAction might take the game state, actor, and target.
    // We will leave it simple here as the ActionResolver will orchestrate the logic.
}
