package com.quickwerewolf.service;

import com.quickwerewolf.domain.role.RoleType;
import com.quickwerewolf.domain.role.Team;
import com.quickwerewolf.entity.GamePlayer;
import com.quickwerewolf.entity.GameSession;
import com.quickwerewolf.entity.Room;
import com.quickwerewolf.entity.RoomPlayer;
import com.quickwerewolf.game.PhaseManager;
import com.quickwerewolf.repository.GamePlayerRepository;
import com.quickwerewolf.repository.GameSessionRepository;
import com.quickwerewolf.repository.RoomPlayerRepository;
import com.quickwerewolf.repository.RoomRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class GameService {

    @Autowired
    private RoomRepository roomRepository;
    
    @Autowired
    private RoomPlayerRepository roomPlayerRepository;
    
    @Autowired
    private GameSessionRepository gameSessionRepository;
    
    @Autowired
    private GamePlayerRepository gamePlayerRepository;
    
    @Autowired
    private PhaseManager phaseManager;

    @Transactional
    public void startGame(String roomCode, String hostDeviceId, List<RoleType> selectedRoles) {
        Room room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new IllegalArgumentException("Room not found"));
                
        if (!room.getHostPlayerId().equals(hostDeviceId)) {
            throw new IllegalStateException("Only host can start game");
        }
        
        List<RoomPlayer> players = roomPlayerRepository.findByRoomId(room.getId());
        if (players.size() < selectedRoles.size()) {
            throw new IllegalStateException("Not enough players for selected roles");
        }
        
        room.setStatus("PLAYING");
        roomRepository.save(room);
        
        GameSession session = new GameSession();
        session.setRoomId(room.getRoomCode());
        session.setCurrentDay(1);
        session = gameSessionRepository.save(session);
        
        // Assign roles randomly
        List<RoleType> shuffledRoles = new ArrayList<>(selectedRoles);
        // Fill remaining players with VILLAGER if not enough roles provided (fallback)
        while (shuffledRoles.size() < players.size()) {
            shuffledRoles.add(RoleType.VILLAGER);
        }
        Collections.shuffle(shuffledRoles);
        
        List<GamePlayer> gamePlayers = new ArrayList<>();
        for (int i = 0; i < players.size(); i++) {
            RoomPlayer rp = players.get(i);
            RoleType assignedRole = shuffledRoles.get(i);
            
            GamePlayer gp = new GamePlayer();
            gp.setGameId(session.getId());
            gp.setPlayerId(rp.getDeviceId());
            gp.setDisplayName(rp.getDisplayName());
            gp.setRole(assignedRole.name());
            
            // Assign team based on role
            if (assignedRole == RoleType.WEREWOLF) {
                gp.setTeam(Team.WEREWOLF.name());
            } else if (assignedRole == RoleType.SERIAL_KILLER) {
                gp.setTeam(Team.SOLO_SK.name());
            } else if (assignedRole == RoleType.FOOL) {
                gp.setTeam(Team.SOLO_FOOL.name());
            } else {
                gp.setTeam(Team.VILLAGE.name());
            }
            
            gamePlayers.add(gp);
        }
        
        gamePlayerRepository.saveAll(gamePlayers);
        
        phaseManager.startNight(session.getId());
    }
}
