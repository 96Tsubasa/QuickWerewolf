import React, { useEffect, useState } from 'react';
import { useParams } from 'react-router-dom';
import { useGameStore } from '../store/gameStore';
import { usePlayerStore } from '../store/playerStore';
import { useChatStore } from '../store/chatStore';
import { socketClient } from '../websocket/socketClient';
import type { GameSessionState, ChatMessage } from '../types';

export const GamePage: React.FC = () => {
  const { roomId } = useParams<{ roomId: string }>();
  const game = useGameStore();
  const player = usePlayerStore();
  const chat = useChatStore();
  
  const [chatInput, setChatInput] = useState('');
  const [chatChannel, setChatChannel] = useState<'PUBLIC' | 'WEREWOLF'>('PUBLIC');
  
  const myPlayerId = player.deviceId;
  
  const myGamePlayer = game.players?.find(p => p.playerId === myPlayerId);

  useEffect(() => {
    if (!roomId) return;
    
    socketClient.subscribeToGame(roomId, (state: GameSessionState) => {
      game.setGameState(state);
      if (state.players) {
        game.setPlayers(state.players);
      }
    });
    
    socketClient.subscribeToChat(roomId, (msg: ChatMessage) => {
      chat.addMessage(msg);
    });

    if (myGamePlayer?.role === 'WEREWOLF') {
      socketClient.subscribeToWerewolfChat(roomId, (msg: ChatMessage) => {
        chat.addMessage(msg);
      });
    }
    
    // Subscribe to private notifications
    socketClient.subscribeToPrivateChat(roomId, myPlayerId, (msg: ChatMessage) => {
      chat.addMessage(msg);
    });

    return () => {
      // Clean up subscriptions (assuming socketClient provides this, if not, handle it)
    };
  }, [roomId, myPlayerId, myGamePlayer?.role]);

  const handleAction = (targetId: string) => {
    if (game.currentPhase === 'NIGHT' && myGamePlayer?.alive) {
      let actionType = '';
      if (myGamePlayer.role === 'SEER') actionType = 'INSPECT';
      if (myGamePlayer.role === 'BODYGUARD') actionType = 'PROTECT';
      if (myGamePlayer.role === 'WEREWOLF') actionType = 'WOLF_KILL';
      if (myGamePlayer.role === 'SERIAL_KILLER') actionType = 'SK_KILL';
      
      if (actionType) {
        socketClient.sendAction(game.id!, myPlayerId, targetId, actionType);
      }
    } else if (game.currentPhase === 'VOTING' && myGamePlayer?.alive) {
      socketClient.sendVote(game.id!, myPlayerId, targetId);
    }
  };

  const handleSendMessage = (e: React.FormEvent) => {
    e.preventDefault();
    if (!chatInput.trim() || !roomId) return;
    
    socketClient.sendChatMessage(roomId, myPlayerId, chatChannel, null, chatInput);
    setChatInput('');
  };

  if (!game.id) return <div className="loading-screen">Loading Game...</div>;

  return (
    <div className="game-container" style={{ display: 'flex', flexDirection: 'column', height: '100vh', padding: '1rem', backgroundColor: '#1a1a2e', color: '#fff' }}>
      
      {/* Header */}
      <div className="game-header" style={{ display: 'flex', justifyContent: 'space-between', marginBottom: '1rem' }}>
        <div>
          <h2>Phase: {game.currentPhase} (Day {game.currentDay})</h2>
        </div>
        <div>
          <h3>My Role: {myGamePlayer?.role || 'Spectator'}</h3>
          <h4>Status: {myGamePlayer?.alive ? 'Alive' : 'DEAD'}</h4>
        </div>
      </div>

      {/* Main Content */}
      <div className="game-content" style={{ display: 'flex', flex: 1, gap: '1rem', minHeight: 0 }}>
        
        {/* Player Grid */}
        <div className="player-grid" style={{ flex: 2, display: 'grid', gridTemplateColumns: 'repeat(4, 1fr)', gap: '10px', overflowY: 'auto' }}>
          {game.players?.map(player => (
            <div 
              key={player.id} 
              onClick={() => handleAction(player.playerId)}
              style={{
                backgroundColor: player.alive ? '#16213e' : '#0f0f0f',
                padding: '1rem',
                borderRadius: '8px',
                border: '1px solid #0f3460',
                cursor: myGamePlayer?.alive ? 'pointer' : 'default',
                display: 'flex',
                flexDirection: 'column',
                alignItems: 'center',
                justifyContent: 'center',
                opacity: player.alive ? 1 : 0.5
              }}
            >
              <span style={{ fontSize: '1.2rem', fontWeight: 'bold' }}>{player.displayName}</span>
              {!player.alive && <span style={{ color: '#e94560' }}>DEAD</span>}
              {player.disconnected && <span style={{ color: '#aaa', fontSize: '0.8rem' }}>(Offline)</span>}
            </div>
          ))}
        </div>

        {/* Chat Box */}
        <div className="chat-box" style={{ flex: 1, display: 'flex', flexDirection: 'column', backgroundColor: '#16213e', borderRadius: '8px', padding: '1rem' }}>
          <div className="chat-messages" style={{ flex: 1, overflowY: 'auto', marginBottom: '1rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            {chat.messages.map((msg, idx) => (
              <div key={idx} style={{ 
                padding: '0.5rem', 
                borderRadius: '4px', 
                backgroundColor: msg.type === 'SYSTEM' ? 'rgba(255,255,255,0.1)' : msg.type === 'WEREWOLF' ? 'rgba(233,69,96,0.2)' : 'rgba(15,52,96,0.3)' 
              }}>
                <span style={{ fontWeight: 'bold', color: msg.type === 'WEREWOLF' ? '#e94560' : '#43bccd' }}>
                  {msg.senderName || msg.senderId}: 
                </span> {msg.content}
              </div>
            ))}
          </div>
          
          <form onSubmit={handleSendMessage} style={{ display: 'flex', gap: '0.5rem' }}>
            {myGamePlayer?.role === 'WEREWOLF' && (
              <select 
                value={chatChannel} 
                onChange={(e) => setChatChannel(e.target.value as 'PUBLIC' | 'WEREWOLF')}
                style={{ backgroundColor: '#0f3460', color: '#fff', border: 'none', padding: '0.5rem', borderRadius: '4px' }}
              >
                <option value="PUBLIC">Public</option>
                <option value="WEREWOLF">Wolf</option>
              </select>
            )}
            <input 
              type="text" 
              value={chatInput} 
              onChange={(e) => setChatInput(e.target.value)}
              placeholder="Type message..." 
              disabled={!myGamePlayer?.alive || (game.currentPhase !== 'DISCUSSION' && chatChannel === 'PUBLIC')}
              style={{ flex: 1, padding: '0.5rem', backgroundColor: '#0f0f0f', color: '#fff', border: '1px solid #0f3460', borderRadius: '4px' }}
            />
            <button 
              type="submit"
              disabled={!myGamePlayer?.alive || (game.currentPhase !== 'DISCUSSION' && chatChannel === 'PUBLIC')}
              style={{ padding: '0.5rem 1rem', backgroundColor: '#e94560', color: '#fff', border: 'none', borderRadius: '4px', cursor: 'pointer' }}
            >Send</button>
          </form>
        </div>

      </div>
    </div>
  );
};
