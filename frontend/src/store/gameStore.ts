import { create } from 'zustand';
import type { GameSessionState, GamePlayer } from '../types';

interface GameStore extends Partial<GameSessionState> {
  timerRemaining: number;
  setGameState: (state: Partial<GameSessionState>) => void;
  setTimer: (time: number) => void;
  players: GamePlayer[];
  setPlayers: (players: GamePlayer[]) => void;
}

export const useGameStore = create<GameStore>((set) => ({
  timerRemaining: 0,
  players: [],
  setGameState: (newState) => set((state) => ({ ...state, ...newState })),
  setTimer: (time) => set({ timerRemaining: time }),
  setPlayers: (players) => set({ players })
}));
