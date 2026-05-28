package com.quickwerewolf.repository;

import com.quickwerewolf.entity.RoomPlayer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface RoomPlayerRepository extends JpaRepository<RoomPlayer, Long> {
    Optional<RoomPlayer> findByRoomIdAndDeviceId(Long roomId, String deviceId);
    List<RoomPlayer> findByRoomId(Long roomId);
}
