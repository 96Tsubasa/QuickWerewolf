import React, { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { api } from '../services/api';
import { usePlayerStore } from '../store/playerStore';
import { useRoomStore } from '../store/roomStore';

export const Home = () => {
  const navigate = useNavigate();
  const { deviceId, displayName, setPlayerInfo } = usePlayerStore();
  const { setRoomState } = useRoomStore();
  
  const [name, setName] = useState(displayName);
  const [roomCode, setRoomCode] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const handleCreateRoom = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim()) {
      setError('Please enter a display name');
      return;
    }
    
    try {
      setLoading(true);
      setError('');
      setPlayerInfo(deviceId, name);
      const state = await api.createRoom(deviceId, name);
      setRoomState(state);
      navigate(`/room/${state.roomCode}`);
    } catch (err: any) {
      setError(err.message || 'Failed to create room');
    } finally {
      setLoading(false);
    }
  };

  const handleJoinRoom = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!name.trim() || !roomCode.trim()) {
      setError('Please enter both name and room code');
      return;
    }

    try {
      setLoading(true);
      setError('');
      setPlayerInfo(deviceId, name);
      const state = await api.joinRoom(roomCode.toUpperCase(), deviceId, name);
      setRoomState(state);
      navigate(`/room/${state.roomCode}`);
    } catch (err: any) {
      setError(err.message || 'Failed to join room');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div style={styles.container}>
      <div style={styles.header}>
        <h1 style={styles.title}>Quick Werewolf</h1>
        <p style={styles.subtitle}>A game of deception and deduction. Start a game with your friends right away! No need to create accounts or download any apps!</p>
      </div>

      <div className="glass-panel" style={styles.panel}>
        {error && <div style={styles.error}>{error}</div>}
        
        <div style={styles.inputGroup}>
          <label style={styles.label}>Display Name</label>
          <input 
            type="text" 
            value={name} 
            onChange={(e) => setName(e.target.value)} 
            placeholder="Enter your name..."
            maxLength={15}
          />
        </div>

        <div style={styles.actions}>
          <button 
            className="primary" 
            style={styles.button} 
            onClick={handleCreateRoom}
            disabled={loading}
          >
            Create New Room
          </button>
          
          <div style={styles.divider}>
            <span style={styles.dividerText}>OR</span>
          </div>

          <div style={styles.joinGroup}>
            <input 
              type="text" 
              value={roomCode} 
              onChange={(e) => setRoomCode(e.target.value.toUpperCase())} 
              placeholder="ROOM CODE"
              maxLength={6}
              style={styles.codeInput}
            />
            <button 
              className="primary" 
              style={{ ...styles.button, flex: 1 }} 
              onClick={handleJoinRoom}
              disabled={loading}
            >
              Join Room
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};

const styles: Record<string, React.CSSProperties> = {
  container: {
    display: 'flex',
    flexDirection: 'column',
    alignItems: 'center',
    justifyContent: 'center',
    minHeight: '100vh',
    padding: '20px',
  },
  header: {
    textAlign: 'center',
    marginBottom: '40px',
  },
  title: {
    fontSize: '4rem',
    background: 'linear-gradient(to right, #818cf8, #c084fc)',
    WebkitBackgroundClip: 'text',
    WebkitTextFillColor: 'transparent',
    marginBottom: '8px',
  },
  subtitle: {
    color: 'var(--text-secondary)',
    fontSize: '1.2rem',
  },
  panel: {
    width: '100%',
    maxWidth: '400px',
    padding: '32px',
  },
  inputGroup: {
    marginBottom: '24px',
  },
  label: {
    display: 'block',
    marginBottom: '8px',
    color: 'var(--text-secondary)',
    fontSize: '0.9rem',
    fontWeight: 600,
  },
  actions: {
    display: 'flex',
    flexDirection: 'column',
    gap: '16px',
  },
  button: {
    width: '100%',
    padding: '14px',
    fontSize: '1.1rem',
  },
  divider: {
    position: 'relative',
    textAlign: 'center',
    margin: '16px 0',
  },
  dividerText: {
    background: 'var(--panel-bg)',
    padding: '0 16px',
    color: 'var(--text-secondary)',
    fontSize: '0.8rem',
    position: 'relative',
    zIndex: 1,
  },
  joinGroup: {
    display: 'flex',
    gap: '12px',
  },
  codeInput: {
    width: '140px',
    textAlign: 'center',
    letterSpacing: '2px',
    textTransform: 'uppercase',
  },
  error: {
    backgroundColor: 'rgba(239, 68, 68, 0.1)',
    color: '#ef4444',
    padding: '12px',
    borderRadius: '8px',
    marginBottom: '20px',
    textAlign: 'center',
    border: '1px solid rgba(239, 68, 68, 0.2)',
  }
};
