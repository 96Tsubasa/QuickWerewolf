import { useState, useEffect } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useGameStore } from '../store/gameStore';
import type { Role } from '../types';
import { GameRoom } from './GameRoom';

const ROLES: Role[] = ['VILLAGER', 'SEER', 'BODYGUARD', 'WEREWOLF', 'SERIAL_KILLER', 'FOOL'];

export const Lobby = () => {
    const { roomCode } = useParams<{ roomCode: string }>();
    const navigate = useNavigate();
    const { 
        roomState, 
        deviceId, 
        joinRoom, 
        updateSettings, 
        startGame, 
        error 
    } = useGameStore();

    const [maxPlayers, setMaxPlayers] = useState(16);
    const [hostPlays, setHostPlays] = useState(true);
    const [roleCounts, setRoleCounts] = useState<Record<Role, number>>({
        VILLAGER: 0,
        SEER: 0,
        BODYGUARD: 0,
        WEREWOLF: 0,
        SERIAL_KILLER: 0,
        FOOL: 0
    });

    useEffect(() => {
        if (!roomState && roomCode) {
            joinRoom(roomCode).catch(() => {
                navigate('/');
            });
        }
    }, [roomCode, roomState, joinRoom, navigate]);

    useEffect(() => {
        if (roomState && roomState.roleCounts) {
            setMaxPlayers(roomState.maxPlayers);
            setHostPlays(roomState.hostPlays);
            setRoleCounts({ ...roomState.roleCounts } as any);
        }
    }, [roomState]);

    if (!roomState) {
        return <div className="loading-screen">Loading room...</div>;
    }

    if (error) {
        return <div className="error-screen">Error: {error}</div>;
    }

    if (roomState.currentPhase !== 'LOBBY') {
        return <GameRoom />;
    }

    const isHost = roomState.players.find(p => p.deviceId === deviceId)?.host;
    const totalRoles = Object.values(roleCounts).reduce((a, b) => a + b, 0);
    const canStart = totalRoles === maxPlayers && roomState.players.length >= 1; // at least some players

    const handleUpdateSettings = (newMaxPlayers: number, newHostPlays: boolean, newRoleCounts: Record<Role, number>) => {
        if (isHost) {
            updateSettings(newMaxPlayers, newHostPlays, newRoleCounts);
        }
    };

    const handleRoleChange = (role: Role, delta: number) => {
        const newVal = Math.max(0, (roleCounts[role] || 0) + delta);
        const newRoles = { ...roleCounts, [role]: newVal };
        setRoleCounts(newRoles);
        handleUpdateSettings(maxPlayers, hostPlays, newRoles);
    };

    return (
        <div className="lobby-container">
            <div className="glass-panel main-panel">
                <h1 className="title">Room: {roomState.roomId}</h1>
                <div className="lobby-content">
                    <div className="players-list">
                        <h2>Players ({roomState.players.length}/{maxPlayers})</h2>
                        <ul>
                            {roomState.players.map(p => (
                                <li key={p.deviceId} className={p.hasDisconnected ? 'disconnected' : ''}>
                                    {p.displayName} {p.host && '(Host)'} {!roomState.hostPlays && p.host && '(Spectating)'}
                                </li>
                            ))}
                        </ul>
                    </div>

                    <div className="room-settings">
                        <h2>Settings</h2>
                        {isHost ? (
                            <>
                                <div className="setting-item">
                                    <label>Max Players:</label>
                                    <input 
                                        type="number" 
                                        value={maxPlayers} 
                                        min={roomState.players.length} 
                                        max={16}
                                        onChange={(e) => {
                                            const v = parseInt(e.target.value);
                                            setMaxPlayers(v);
                                            handleUpdateSettings(v, hostPlays, roleCounts);
                                        }}
                                        className="input-field"
                                    />
                                </div>
                                <div className="setting-item">
                                    <label>Host plays?</label>
                                    <input 
                                        type="checkbox" 
                                        checked={hostPlays} 
                                        onChange={(e) => {
                                            setHostPlays(e.target.checked);
                                            handleUpdateSettings(maxPlayers, e.target.checked, roleCounts);
                                        }}
                                    />
                                </div>
                                
                                <div className="role-selector">
                                    <h3>Roles ({totalRoles}/{maxPlayers})</h3>
                                    {ROLES.map(role => (
                                        <div key={role} className="role-item">
                                            <span>{role.replace('_', ' ')}</span>
                                            <div className="role-controls">
                                                <button onClick={() => handleRoleChange(role, -1)}>-</button>
                                                <span>{roleCounts[role] || 0}</span>
                                                <button onClick={() => handleRoleChange(role, 1)}>+</button>
                                            </div>
                                        </div>
                                    ))}
                                </div>

                                <button 
                                    className="primary-btn start-btn" 
                                    onClick={startGame} 
                                    disabled={!canStart}
                                >
                                    Start Game
                                </button>
                                {!canStart && <p className="warning">Total roles must equal Max Players</p>}
                            </>
                        ) : (
                            <div className="waiting-info">
                                <p>Waiting for host to configure game...</p>
                                <p>Max Players: {maxPlayers}</p>
                                <h3>Selected Roles:</h3>
                                <ul>
                                    {Object.entries(roleCounts).filter(([_, count]) => count > 0).map(([role, count]) => (
                                        <li key={role}>{role.replace('_', ' ')}: {count as number}</li>
                                    ))}
                                </ul>
                            </div>
                        )}
                    </div>
                </div>
            </div>
        </div>
    );
};
