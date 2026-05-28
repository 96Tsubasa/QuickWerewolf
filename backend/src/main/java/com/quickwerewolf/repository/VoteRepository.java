package com.quickwerewolf.repository;

import com.quickwerewolf.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, String> {
    List<Vote> findByGameIdAndDay(String gameId, int day);
    Optional<Vote> findByGameIdAndDayAndVoterId(String gameId, int day, String voterId);
}
