import type { RoomState } from '../types';

const API_BASE = 'http://localhost:8080/api';

export const api = {
  createRoom: async (deviceId: string, displayName: string): Promise<RoomState> => {
    const res = await fetch(`${API_BASE}/rooms`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ deviceId, displayName }),
    });
    if (!res.ok) throw new Error('Failed to create room');
    return res.json();
  },

  joinRoom: async (roomCode: string, deviceId: string, displayName: string): Promise<RoomState> => {
    const res = await fetch(`${API_BASE}/rooms/${roomCode}/join`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ deviceId, displayName }),
    });
    if (!res.ok) {
      const errorText = await res.text();
      throw new Error(errorText || 'Failed to join room');
    }
    return res.json();
  },

  kickPlayer: async (roomCode: string, hostDeviceId: string, targetDeviceId: string): Promise<void> => {
    const res = await fetch(`${API_BASE}/rooms/${roomCode}/kick`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ hostDeviceId, targetDeviceId }),
    });
    if (!res.ok) throw new Error('Failed to kick player');
  },

  closeRoom: async (roomCode: string, hostDeviceId: string): Promise<void> => {
    const res = await fetch(`${API_BASE}/rooms/${roomCode}/close`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ hostDeviceId }),
    });
    if (!res.ok) throw new Error('Failed to close room');
  },

  quitRoom: async (roomCode: string, deviceId: string): Promise<void> => {
    const res = await fetch(`${API_BASE}/rooms/${roomCode}/quit`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ deviceId }),
    });
    if (!res.ok) throw new Error('Failed to quit room');
  },

  getRoomState: async (roomCode: string): Promise<RoomState> => {
    const res = await fetch(`${API_BASE}/rooms/${roomCode}`);
    if (!res.ok) throw new Error('Failed to fetch room state');
    return res.json();
  },

  startGame: async (roomCode: string, hostDeviceId: string, selectedRoles: string[]): Promise<void> => {
    const res = await fetch(`${API_BASE}/rooms/${roomCode}/start`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ hostDeviceId, selectedRoles }),
    });
    if (!res.ok) {
      const text = await res.text();
      throw new Error(text || 'Failed to start game');
    }
  }
};
