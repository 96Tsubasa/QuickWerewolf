package com.quickwerewolf.repository;

import com.quickwerewolf.domain.Room;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoomRepository extends CrudRepository<Room, String> {
}
