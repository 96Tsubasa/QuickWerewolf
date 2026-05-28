import { useState, useEffect } from 'react';
import { useGameStore } from '../store/gameStore';
import { ChatBox } from '../components/ChatBox';

export const GameRoom = () => {
    const { roomState, deviceId, performNightAction, performDayVote } = useGameStore();
    const [timeLeft, setTimeLeft] = useState(0);
    const [selectedPlayer, setSelectedPlayer] = useState<string | null>(null);

    useEffect(() => {
        if (!roomState?.phaseEndTime) return;
        
        const updateTimer = () => {
            const remaining = Math.max(0, Math.floor((roomState.phaseEndTime - Date.now()) / 1000));
            setTimeLeft(remaining);
        };
        
        updateTimer();
        const interval = setInterval(updateTimer, 1000);
        return () => clearInterval(interval);
    }, [roomState?.phaseEndTime]);

    if (!roomState) return null;

    const myPlayer = roomState.players.find(p => p.deviceId === deviceId);
    const isAlive = myPlayer?.alive;

    const getActionLabel = () => {
        if (!isAlive) return "You are dead";
        if (roomState.currentPhase === 'DAY_VOTING') return "Vote to Lynch";
        if (roomState.currentPhase === 'NIGHT') {
            switch (myPlayer?.role) {
                case 'WEREWOLF': return "Vote to Kill";
                case 'SERIAL_KILLER': return "Kill";
                case 'BODYGUARD': return "Protect";
                case 'SEER': return "Check Role";
                default: return "Sleeping...";
            }
        }
        return "Waiting...";
    };

    const handleAction = () => {
        if (!selectedPlayer || !isAlive) return;
        if (roomState.currentPhase === 'DAY_VOTING') {
            performDayVote(selectedPlayer);
        } else if (roomState.currentPhase === 'NIGHT') {
            performNightAction(selectedPlayer);
        }
        setSelectedPlayer(null); // Reset selection after action
    };

    const canPerformAction = () => {
        if (!isAlive || !selectedPlayer) return false;
        if (roomState.currentPhase === 'DAY_VOTING') return true;
        if (roomState.currentPhase === 'NIGHT') {
            const role = myPlayer?.role;
            if (role === 'BODYGUARD' && selectedPlayer === roomState.previousProtectedPlayerId) return false;
            return role === 'WEREWOLF' || role === 'SERIAL_KILLER' || role === 'BODYGUARD' || role === 'SEER';
        }
        return false;
    };

    return (
        <div className="game-room-container">
            <div className="game-header glass-panel">
                <div className="phase-info">
                    <h2>{roomState.currentPhase.replace('_', ' ')}</h2>
                    <p className="timer">{timeLeft}s</p>
                </div>
                <div className="player-info">
                    <h3>Role: {myPlayer?.role?.replace('_', ' ') || 'Spectator'}</h3>
                    <p className={isAlive ? 'status-alive' : 'status-dead'}>{isAlive ? 'ALIVE' : 'DEAD'}</p>
                </div>
            </div>

            <div className="game-main">
                <div className="grid-container glass-panel">
                    <div className="player-grid">
                        {roomState.players.map(p => {
                            if (!roomState.hostPlays && p.host) return null; // Skip non-playing host
                            
                            const isSelected = selectedPlayer === p.deviceId;
                            const isMe = p.deviceId === deviceId;
                            const isPrevProtected = myPlayer?.role === 'BODYGUARD' && roomState.currentPhase === 'NIGHT' && p.deviceId === roomState.previousProtectedPlayerId;
                            
                            return (
                                <div 
                                    key={p.deviceId} 
                                    className={`player-card ${!p.alive ? 'dead' : ''} ${isSelected ? 'selected' : ''} ${isMe ? 'me' : ''} ${isPrevProtected ? 'protected-prev' : ''}`}
                                    onClick={() => p.alive && !isPrevProtected && setSelectedPlayer(p.deviceId)}
                                >
                                    <span className="player-name">{p.displayName}</span>
                                    {!p.alive && <span className="dead-label">DEAD</span>}
                                    {isPrevProtected && <span className="afk-label" style={{background: 'orange'}}>Protected Last Night</span>}
                                    {p.hasDisconnected && <span className="afk-label">AFK</span>}
                                </div>
                            );
                        })}
                    </div>
                    
                    <div className="action-area">
                        <button 
                            className="primary-btn action-btn" 
                            disabled={!canPerformAction()}
                            onClick={handleAction}
                        >
                            {getActionLabel()}
                        </button>
                    </div>
                </div>

                <div className="chat-container glass-panel">
                    <ChatBox />
                </div>
            </div>
        </div>
    );
};
