import { create } from 'zustand';
import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { RoomState, ChatMessage, Role } from '../types';

interface GameStore {
    deviceId: string;
    displayName: string;
    roomState: RoomState | null;
    globalChat: ChatMessage[];
    werewolfChat: ChatMessage[];
    seerResults: Record<string, string>;
    stompClient: Client | null;
    error: string | null;
    theme: 'light' | 'dark';

    setUserInfo: (deviceId: string, displayName: string) => void;
    connectWebSocket: (roomId: string) => void;
    disconnectWebSocket: () => void;
    leaveRoom: () => void;

    createRoom: () => Promise<void>;
    joinRoom: (roomId: string) => Promise<void>;
    updateSettings: (maxPlayers: number, hostPlays: boolean, roleCounts: Record<Role, number>) => Promise<void>;
    updatePhaseDurations: (phaseDurations: Record<string, number>) => Promise<void>;
    startGame: () => void;
    endGame: () => Promise<void>;
    sendGlobalChat: (text: string) => void;
    sendWerewolfChat: (text: string) => void;
    performNightAction: (targetId: string) => void;
    performDayVote: (targetId: string) => void;
    kickPlayer: (targetDeviceId: string) => Promise<void>;
    updateTheme: () => void;
}

const BACKEND_URL = 'http://localhost:8080';

export const useGameStore = create<GameStore>((set, get) => ({
    deviceId: localStorage.getItem('qw_device_id') || crypto.randomUUID(),
    displayName: localStorage.getItem('qw_display_name') || '',
    roomState: null,
    globalChat: [],
    werewolfChat: [],
    seerResults: {},
    stompClient: null,
    error: null,
    theme: 'dark',

    setUserInfo: (deviceId: string, displayName: string) => {
        localStorage.setItem('qw_device_id', deviceId);
        localStorage.setItem('qw_display_name', displayName);
        set({ deviceId, displayName });
    },

    connectWebSocket: (roomId: string) => {
        const { deviceId, stompClient } = get();
        if (stompClient) stompClient.deactivate();

        const client = new Client({
            webSocketFactory: () => new SockJS(`${BACKEND_URL}/ws`),
            reconnectDelay: 5000,
            onConnect: () => {
                // Subscribe to room state updates
                client.subscribe(`/topic/room/${roomId}/state`, (msg) => {
                    const newRoomState = JSON.parse(msg.body);
                    set({ roomState: newRoomState });
                    get().updateTheme();
                });

                // Subscribe to global chat
                client.subscribe(`/topic/room/${roomId}/chat`, (msg) => {
                    const chatMsg = JSON.parse(msg.body);
                    chatMsg.id = crypto.randomUUID();
                    set((state) => ({ globalChat: [...state.globalChat, chatMsg] }));
                });

                // Subscribe to werewolf chat
                client.subscribe(`/topic/room/${roomId}/werewolf`, (msg) => {
                    const chatMsg = JSON.parse(msg.body);
                    chatMsg.id = crypto.randomUUID();
                    set((state) => ({ werewolfChat: [...state.werewolfChat, chatMsg] }));
                });

                // Subscribe to seer result
                client.subscribe(`/topic/room/${roomId}/seer/${deviceId}`, (msg) => {
                    const data = JSON.parse(msg.body);
                    const chatMsg = { id: crypto.randomUUID(), sender: 'System', text: data.message, isSystem: true };

                    set((state) => ({
                        globalChat: [...state.globalChat, chatMsg],
                        seerResults: { ...state.seerResults, [data.targetId]: data.role }
                    }));
                });

                // Subscribe to errors
                client.subscribe(`/topic/room/${roomId}/error/${deviceId}`, (msg) => {
                    set({ error: msg.body });
                    setTimeout(() => set({ error: null }), 3000);
                });

                // Subscribe to kicks
                client.subscribe(`/topic/room/${roomId}/kicked`, (msg) => {
                    if (msg.body === deviceId) {
                        get().disconnectWebSocket();
                        set({ roomState: null, error: "You were kicked from the room." });
                    }
                });
            },
        });

        client.activate();
        set({ stompClient: client });
    },

    disconnectWebSocket: () => {
        const { stompClient } = get();
        if (stompClient) {
            stompClient.deactivate();
        }
        set({ stompClient: null });
    },

    leaveRoom: () => {
        get().disconnectWebSocket();
        set({ roomState: null, globalChat: [], werewolfChat: [], seerResults: {}, error: null, theme: 'dark' });
    },

    createRoom: async () => {
        const { deviceId, displayName } = get();
        try {
            const res = await fetch(`${BACKEND_URL}/api/rooms`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ deviceId, displayName })
            });
            const data = await res.json();
            set({ roomState: data });
            get().connectWebSocket(data.roomId);
        } catch (e) {
            set({ error: "Failed to create room" });
        }
    },

    joinRoom: async (roomId: string) => {
        const { deviceId, displayName } = get();
        try {
            const res = await fetch(`${BACKEND_URL}/api/rooms/${roomId}/join`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ deviceId, displayName })
            });
            if (!res.ok) throw new Error("Join failed");
            const data = await res.json();
            set({ roomState: data });
            get().connectWebSocket(data.roomId);
        } catch (e) {
            set({ error: "Failed to join room" });
            throw e;
        }
    },

    updateSettings: async (maxPlayers, hostPlays, roleCounts) => {
        const { deviceId, roomState } = get();
        if (!roomState) return;
        try {
            await fetch(`${BACKEND_URL}/api/rooms/${roomState.roomId}/settings`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ hostDeviceId: deviceId, maxPlayers, hostPlays, roleCounts })
            });
        } catch (e) {
            set({ error: "Failed to update settings" });
        }
    },

    updatePhaseDurations: async (phaseDurations) => {
        const { deviceId, roomState } = get();
        if (!roomState) return;
        try {
            await fetch(`${BACKEND_URL}/api/rooms/${roomState.roomId}/phase-durations`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ hostDeviceId: deviceId, phaseDurations })
            });
        } catch (e) {
            set({ error: "Failed to update phase durations" });
        }
    },

    startGame: () => {
        const { stompClient, roomState, deviceId } = get();
        if (stompClient && roomState) {
            stompClient.publish({
                destination: `/app/room/${roomState.roomId}/start`,
                body: JSON.stringify({ hostDeviceId: deviceId })
            });
        }
    },

    endGame: async () => {
        const { deviceId, roomState } = get();
        if (!roomState) return;
        try {
            await fetch(`${BACKEND_URL}/api/rooms/${roomState.roomId}/end`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ hostDeviceId: deviceId })
            });
        } catch (e) {
            set({ error: "Failed to end game" });
        }
    },

    sendGlobalChat: (text: string) => {
        const { stompClient, roomState, displayName } = get();
        if (stompClient && roomState) {
            stompClient.publish({
                destination: `/app/room/${roomState.roomId}/chat`,
                body: JSON.stringify({ sender: displayName, text })
            });
        }
    },

    sendWerewolfChat: (text: string) => {
        const { stompClient, roomState, displayName } = get();
        if (stompClient && roomState) {
            stompClient.publish({
                destination: `/app/room/${roomState.roomId}/chat/werewolf`,
                body: JSON.stringify({ sender: displayName, text })
            });
        }
    },

    performNightAction: (targetId: string) => {
        const { stompClient, roomState, deviceId } = get();
        if (stompClient && roomState) {
            stompClient.publish({
                destination: `/app/room/${roomState.roomId}/action/night`,
                body: JSON.stringify({ deviceId, targetId })
            });
        }
    },

    performDayVote: (targetId: string) => {
        const { stompClient, roomState, deviceId } = get();
        if (stompClient && roomState) {
            stompClient.publish({
                destination: `/app/room/${roomState.roomId}/action/vote`,
                body: JSON.stringify({ deviceId, targetId })
            });
        }
    },

    kickPlayer: async (targetDeviceId: string) => {
        const { deviceId, roomState } = get();
        if (!roomState) return;
        try {
            await fetch(`${BACKEND_URL}/api/rooms/${roomState.roomId}/kick`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ hostDeviceId: deviceId, targetDeviceId })
            });
        } catch (e) {
            set({ error: "Failed to kick player" });
        }
    },

    updateTheme: () => {
        const { roomState } = get();
        if (!roomState || roomState.currentPhase === 'LOBBY' || roomState.currentPhase === 'ENDED') {
            set({ theme: 'dark' });
            return;
        }

        // Use DAY phases for light theme, NIGHT for dark
        const isNightPhase = roomState.currentPhase === 'NIGHT';
        set({ theme: isNightPhase ? 'dark' : 'light' });
    }
}));
