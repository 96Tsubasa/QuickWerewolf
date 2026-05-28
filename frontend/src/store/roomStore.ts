import { create } from 'zustand';
import type { RoomState } from '../types';

interface RoomStore {
  roomState: RoomState | null;
  setRoomState: (state: RoomState | null) => void;
  clearRoom: () => void;
}

export const useRoomStore = create<RoomStore>((set) => ({
  roomState: null,
  setRoomState: (state) => set({ roomState: state }),
  clearRoom: () => set({ roomState: null }),
}));
