import React, { useEffect, useState, useRef } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useRoomStore } from '../store/roomStore';
import { usePlayerStore } from '../store/playerStore';
import { useChatStore } from '../store/chatStore';
import { socketClient } from '../websocket/socketClient';
import { api } from '../services/api';
import { LogOut, Play, UserX, Crown } from 'lucide-react';

export const Lobby = () => {
  const { roomCode } = useParams<{ roomCode: string }>();
  const navigate = useNavigate();
  const { roomState, setRoomState, clearRoom } = useRoomStore();
  const { deviceId } = usePlayerStore();
  const { messages, clearMessages } = useChatStore();
  
  const [chatInput, setChatInput] = useState('');
  const chatEndRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!roomCode) {
      navigate('/');
      return;
    }

    // Connect to WebSocket
    socketClient.connect(roomCode, () => {
      // On Kicked
      alert("You have been kicked from the room.");
      handleQuit(true);
    });

    // Fetch initial state if we don't have it (e.g. direct URL access)
    if (!roomState) {
      api.getRoomState(roomCode)
        .then(state => setRoomState(state))
        .catch(() => navigate('/')); // If room doesn't exist or not joined
    }

    return () => {
      socketClient.disconnect();
      clearMessages();
    };
  }, [roomCode]);

  useEffect(() => {
    // Scroll chat to bottom
    chatEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages]);

  // Handle room ended status
  useEffect(() => {
    if (roomState?.status === 'ENDED') {
      alert("The room was closed by the host.");
      clearRoom();
      navigate('/');
    }
  }, [roomState?.status]);

  if (!roomState) return <div style={styles.loading}>Connecting to Lobby...</div>;

  const isHost = roomState.hostPlayerId === deviceId;
  const players = roomState.players || [];

  const handleSendChat = (e: React.FormEvent) => {
    e.preventDefault();
    if (!chatInput.trim()) return;
    
    const sender = players.find(p => p.deviceId === deviceId);
    if (!sender) return;

    socketClient.sendChat({
      senderId: deviceId,
      senderName: sender.displayName,
      content: chatInput,
      type: 'PUBLIC'
    });
    setChatInput('');
  };

  const handleKick = async (targetId: string) => {
    if (!isHost || targetId === deviceId) return;
    try {
      await api.kickPlayer(roomState.roomCode, deviceId, targetId);
    } catch (err) {
      console.error(err);
    }
  };

  const handleQuit = async (force: boolean = false) => {
    try {
      if (!force) {
        await api.quitRoom(roomState.roomCode, deviceId);
      }
    } catch (err) {
      console.error(err);
    } finally {
      clearRoom();
      navigate('/');
    }
  };

  const handleCloseRoom = async () => {
    if (!isHost) return;
    try {
      await api.closeRoom(roomState.roomCode, deviceId);
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="app-container" style={styles.container}>
      <div style={styles.mainContent}>
        <div style={styles.header}>
          <div>
            <h2 style={styles.roomCodeTitle}>Room Code</h2>
            <div style={styles.roomCode}>{roomState.roomCode}</div>
          </div>
          <div style={styles.actions}>
            {isHost ? (
              <>
                <button className="primary" style={styles.actionBtn}>
                  <Play size={18} /> Start Game
                </button>
                <button className="danger" onClick={handleCloseRoom} style={styles.actionBtn}>
                  <UserX size={18} /> Close Room
                </button>
              </>
            ) : (
              <button className="danger" onClick={() => handleQuit(false)} style={styles.actionBtn}>
                <LogOut size={18} /> Quit Room
              </button>
            )}
          </div>
        </div>

        <div style={styles.gridContainer}>
          {Array.from({ length: 16 }).map((_, index) => {
            const player = players[index];
            return (
              <div key={index} className="glass-panel" style={{
                ...styles.playerCard,
                ...(player ? styles.playerCardActive : {}),
                ...((player && !player.connected) ? styles.playerCardDisconnected : {})
              }}>
                {player ? (
                  <>
                    <div style={styles.playerAvatar}>
                      {player.displayName.charAt(0).toUpperCase()}
                    </div>
                    <div style={styles.playerName}>
                      {player.displayName}
                      {player.isHost && <Crown size={14} color="#fbbf24" style={{marginLeft: 4}}/>}
                    </div>
                    {isHost && player.deviceId !== deviceId && (
                      <button 
                        style={styles.kickBtn} 
                        onClick={() => handleKick(player.deviceId)}
                        title="Kick Player"
                      >
                        <UserX size={14} />
                      </button>
                    )}
                  </>
                ) : (
                  <div style={styles.emptySlot}>Empty Slot</div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      <div className="glass-panel" style={styles.sidebar}>
        <div style={styles.chatHeader}>Lobby Chat</div>
        
        <div style={styles.chatMessages}>
          {messages.map((msg, i) => (
            <div key={i} style={msg.senderId === deviceId ? styles.msgRowRight : styles.msgRowLeft}>
              <div style={{
                ...styles.messageBubble,
                ...(msg.senderId === deviceId ? styles.msgOwn : styles.msgOther)
              }}>
                {msg.senderId !== deviceId && <div style={styles.msgName}>{msg.senderName}</div>}
                <div>{msg.content}</div>
              </div>
            </div>
          ))}
          <div ref={chatEndRef} />
        </div>

        <form onSubmit={handleSendChat} style={styles.chatForm}>
          <input 
            type="text" 
            value={chatInput}
            onChange={e => setChatInput(e.target.value)}
            placeholder="Type a message..."
            style={styles.chatInput}
          />
        </form>
      </div>
    </div>
  );
};

const styles: Record<string, React.CSSProperties> = {
  container: {
    padding: '24px',
    gap: '24px',
    height: '100vh',
    overflow: 'hidden',
  },
  loading: {
    display: 'flex',
    justifyContent: 'center',
    alignItems: 'center',
    height: '100vh',
    fontSize: '1.5rem',
  },
  mainContent: {
    flex: 1,
    display: 'flex',
    flexDirection: 'column',
    gap: '24px',
    height: '100%',
  },
  header: {
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    padding: '0 16px',
  },
  roomCodeTitle: {
    fontSize: '0.9rem',
    color: 'var(--text-secondary)',
    textTransform: 'uppercase',
    letterSpacing: '1px',
    marginBottom: '4px',
  },
  roomCode: {
    fontSize: '2.5rem',
    fontWeight: 800,
    letterSpacing: '4px',
    background: 'linear-gradient(135deg, #f0f2f5 0%, #a0aec0 100%)',
    WebkitBackgroundClip: 'text',
    WebkitTextFillColor: 'transparent',
  },
  actions: {
    display: 'flex',
    gap: '12px',
  },
  actionBtn: {
    display: 'flex',
    alignItems: 'center',
    gap: '8px',
  },
  gridContainer: {
    display: 'grid',
    gridTemplateColumns: 'repeat(4, 1fr)',
    gridTemplateRows: 'repeat(4, 1fr)',
    gap: '16px',
    flex: 1,
    minHeight: 0, // important for nested flex scroll
  },
  playerCard: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    position: 'relative',
    transition: 'all 0.3s ease',
    border: '1px dashed rgba(255,255,255,0.1)',
    background: 'rgba(20, 27, 45, 0.3)',
  },
  playerCardActive: {
    border: '1px solid rgba(99, 102, 241, 0.3)',
    background: 'var(--panel-bg)',
    boxShadow: '0 4px 12px rgba(0,0,0,0.2)',
  },
  playerCardDisconnected: {
    opacity: 0.5,
    filter: 'grayscale(100%)',
  },
  playerAvatar: {
    width: '48px',
    height: '48px',
    borderRadius: '50%',
    background: 'linear-gradient(135deg, var(--accent-color) 0%, var(--accent-hover) 100%)',
    display: 'flex',
    alignItems: 'center',
    justifyContent: 'center',
    fontSize: '1.5rem',
    fontWeight: 'bold',
    marginBottom: '12px',
    boxShadow: '0 4px 10px rgba(99, 102, 241, 0.4)',
  },
  playerName: {
    fontWeight: 600,
    display: 'flex',
    alignItems: 'center',
  },
  emptySlot: {
    color: 'rgba(255,255,255,0.2)',
    fontSize: '0.9rem',
  },
  kickBtn: {
    position: 'absolute',
    top: '8px',
    right: '8px',
    background: 'rgba(239, 68, 68, 0.2)',
    color: '#ef4444',
    padding: '6px',
    borderRadius: '6px',
  },
  sidebar: {
    width: '350px',
    display: 'flex',
    flexDirection: 'column',
    height: '100%',
  },
  chatHeader: {
    padding: '20px',
    fontWeight: 800,
    fontSize: '1.2rem',
    borderBottom: '1px solid var(--panel-border)',
  },
  chatMessages: {
    flex: 1,
    padding: '20px',
    overflowY: 'auto',
    display: 'flex',
    flexDirection: 'column',
    gap: '12px',
  },
  msgRowLeft: {
    display: 'flex',
    justifyContent: 'flex-start',
  },
  msgRowRight: {
    display: 'flex',
    justifyContent: 'flex-end',
  },
  messageBubble: {
    maxWidth: '80%',
    padding: '10px 14px',
    borderRadius: '12px',
    fontSize: '0.95rem',
  },
  msgOwn: {
    background: 'var(--accent-color)',
    color: 'white',
    borderBottomRightRadius: '4px',
  },
  msgOther: {
    background: 'rgba(255,255,255,0.1)',
    color: 'var(--text-primary)',
    borderBottomLeftRadius: '4px',
  },
  msgName: {
    fontSize: '0.75rem',
    color: 'var(--accent-color)',
    filter: 'brightness(1.2)',
    marginBottom: '2px',
    fontWeight: 600,
  },
  chatForm: {
    padding: '20px',
    borderTop: '1px solid var(--panel-border)',
  },
  chatInput: {
    width: '100%',
  }
};
