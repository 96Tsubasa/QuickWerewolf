package com.quickwerewolf.repository;

import com.quickwerewolf.entity.GamePlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface GamePlayerRepository extends JpaRepository<GamePlayer, String> {
    List<GamePlayer> findByGameId(String gameId);
    Optional<GamePlayer> findByGameIdAndPlayerId(String gameId, String playerId);
    List<GamePlayer> findByGameIdAndAliveTrue(String gameId);
}
