import { create } from 'zustand';

interface GameState {
  gameId: string | null;
  currentPhase: 'NIGHT' | 'DISCUSSION' | 'VOTING' | 'ENDED';
  currentDay: number;
  timer: number;
  setGameState: (state: Partial<GameState>) => void;
}

export const useGameStore = create<GameState>((set) => ({
  gameId: null,
  currentPhase: 'NIGHT',
  currentDay: 1,
  timer: 0,
  setGameState: (newState) => set((state) => ({ ...state, ...newState }))
}));
