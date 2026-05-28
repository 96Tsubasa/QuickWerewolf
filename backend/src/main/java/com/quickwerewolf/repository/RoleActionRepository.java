package com.quickwerewolf.repository;

import com.quickwerewolf.entity.RoleAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RoleActionRepository extends JpaRepository<RoleAction, String> {
    List<RoleAction> findByGameIdAndNightNumber(String gameId, int nightNumber);
}
