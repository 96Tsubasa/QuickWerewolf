package com.quickwerewolf.repository;

import com.quickwerewolf.entity.GameSession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameSessionRepository extends JpaRepository<GameSession, String> {
    Optional<GameSession> findByRoomId(String roomId);
}
