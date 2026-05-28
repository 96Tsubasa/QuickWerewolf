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
    hostChat: ChatMessage[];
    seerResults: Record<string, string>;
    stompClient: Client | null;
    error: string | null;

    setUserInfo: (deviceId: string, displayName: string) => void;
    connectWebSocket: (roomId: string) => void;
    disconnectWebSocket: () => void;
    leaveRoom: () => void;

    createRoom: () => Promise<void>;
    joinRoom: (roomId: string) => Promise<void>;
    updateSettings: (maxPlayers: number, hostPlays: boolean, roleCounts: Record<Role, number>) => Promise<void>;
    startGame: () => void;
    sendGlobalChat: (text: string) => void;
    sendWerewolfChat: (text: string) => void;
    sendHostChat: (text: string, targetDeviceId: string) => void;
    performNightAction: (targetId: string) => void;
    performDayVote: (targetId: string) => void;
}

const BACKEND_URL = 'http://localhost:8080';

export const useGameStore = create<GameStore>((set, get) => ({
    deviceId: localStorage.getItem('qw_device_id') || crypto.randomUUID(),
    displayName: localStorage.getItem('qw_display_name') || '',
    roomState: null,
    globalChat: [],
    werewolfChat: [],
    hostChat: [],
    seerResults: {},
    stompClient: null,
    error: null,

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
                    set({ roomState: JSON.parse(msg.body) });
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

                // Subscribe to host chat (private)
                client.subscribe(`/topic/room/${roomId}/host/${deviceId}`, (msg) => {
                    const chatMsg = JSON.parse(msg.body);
                    chatMsg.id = crypto.randomUUID();
                    set((state) => ({ hostChat: [...state.hostChat, chatMsg] }));
                });

                // Subscribe to seer result
                client.subscribe(`/topic/room/${roomId}/seer/${deviceId}`, (msg) => {
                    const text = msg.body;
                    const chatMsg = { id: crypto.randomUUID(), sender: 'System', text, isSystem: true };

                    // The text looks like: "You checked displayName and their role is ROLE_NAME"
                    // But we want to map targetId to role. Wait, the backend doesn't send the targetId in a structured way!
                    // Let's just pass the structured JSON from backend instead. But currently it sends a string.
                    // For a quick fix without changing backend, let's extract role from text: "role is X" and player name.
                    // Actually, let's just show it in chat for now. Wait, user specifically asked to "show the role on the grid".
                    // I need the targetId to show it on the grid!
                    // Let me change the backend to send JSON instead. I'll do that in GameEngineService.java.
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
        set({ roomState: null, globalChat: [], werewolfChat: [], hostChat: [], seerResults: {}, error: null });
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

    startGame: () => {
        const { stompClient, roomState, deviceId } = get();
        if (stompClient && roomState) {
            stompClient.publish({
                destination: `/app/room/${roomState.roomId}/start`,
                body: JSON.stringify({ hostDeviceId: deviceId })
            });
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

    sendHostChat: (text: string, targetDeviceId: string) => {
        const { stompClient, roomState, displayName, deviceId } = get();
        if (stompClient && roomState) {
            stompClient.publish({
                destination: `/app/room/${roomState.roomId}/chat/host`,
                body: JSON.stringify({ sender: displayName, text, targetDeviceId, senderDeviceId: deviceId })
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
    }
}));
