import { create } from 'zustand';

interface PlayerStore {
  deviceId: string;
  displayName: string;
  setPlayerInfo: (deviceId: string, displayName: string) => void;
}

// Generate or get device ID from localStorage
const getStoredDeviceId = () => {
  let id = localStorage.getItem('quickwerewolf_device_id');
  if (!id) {
    id = crypto.randomUUID();
    localStorage.setItem('quickwerewolf_device_id', id);
  }
  return id;
};

const getStoredDisplayName = () => {
  return localStorage.getItem('quickwerewolf_display_name') || '';
};

export const usePlayerStore = create<PlayerStore>((set) => ({
  deviceId: getStoredDeviceId(),
  displayName: getStoredDisplayName(),
  setPlayerInfo: (deviceId, displayName) => {
    localStorage.setItem('quickwerewolf_device_id', deviceId);
    localStorage.setItem('quickwerewolf_display_name', displayName);
    set({ deviceId, displayName });
  },
}));
