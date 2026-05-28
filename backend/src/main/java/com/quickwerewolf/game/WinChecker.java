package com.quickwerewolf.game;

import com.quickwerewolf.domain.role.Team;
import com.quickwerewolf.entity.GamePlayer;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class WinChecker {

    public Optional<Team> checkWinCondition(List<GamePlayer> alivePlayers) {
        long totalAlive = alivePlayers.size();
        if (totalAlive == 0) {
            return Optional.empty(); // Draw? Or maybe Village wins by default if they wiped out wolves? No, let's leave as no winner for now.
        }

        long werewolfCount = alivePlayers.stream()
                .filter(p -> Team.WEREWOLF.name().equals(p.getTeam()))
                .count();

        long skCount = alivePlayers.stream()
                .filter(p -> Team.SOLO_SK.name().equals(p.getTeam()))
                .count();

        // 1. Check Serial Killer
        if (totalAlive == 1 && skCount == 1) {
            return Optional.of(Team.SOLO_SK);
        }

        // 2. Check Werewolf (needs at least half, and no SK alive)
        if (werewolfCount >= (totalAlive + 1) / 2 && skCount == 0) {
            // Half or more, and no solo killers threatening them
            return Optional.of(Team.WEREWOLF);
        }

        // 3. Check Village
        if (werewolfCount == 0 && skCount == 0) {
            return Optional.of(Team.VILLAGE);
        }

        return Optional.empty();
    }
}
