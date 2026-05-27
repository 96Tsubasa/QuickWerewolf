import { create } from 'zustand';

interface PlayerState {
  playerId: string | null;
  displayName: string;
  role: string | null;
  team: string | null;
  isAlive: boolean;
  setPlayer: (player: Partial<PlayerState>) => void;
}

export const usePlayerStore = create<PlayerState>((set) => ({
  playerId: null,
  displayName: '',
  role: null,
  team: null,
  isAlive: true,
  setPlayer: (newState) => set((state) => ({ ...state, ...newState }))
}));
