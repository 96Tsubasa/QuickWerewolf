import { create } from 'zustand';

interface RoomState {
  roomId: string | null;
  players: any[];
  isHost: boolean;
  status: 'WAITING' | 'PLAYING' | 'ENDED';
  setRoom: (roomId: string, isHost: boolean) => void;
  updatePlayers: (players: any[]) => void;
  setStatus: (status: 'WAITING' | 'PLAYING' | 'ENDED') => void;
}

export const useRoomStore = create<RoomState>((set) => ({
  roomId: null,
  players: [],
  isHost: false,
  status: 'WAITING',
  setRoom: (roomId, isHost) => set({ roomId, isHost }),
  updatePlayers: (players) => set({ players }),
  setStatus: (status) => set({ status })
}));
