export type Role = 'VILLAGER' | 'SEER' | 'BODYGUARD' | 'WEREWOLF' | 'SERIAL_KILLER' | 'FOOL';

export type GamePhase = 'LOBBY' | 'NIGHT' | 'DAY_DISCUSSION' | 'DAY_VOTING' | 'ENDED';

export interface Player {
    deviceId: string;
    displayName: string;
    role: Role | null;
    host: boolean;
    alive: boolean;
    hasDisconnected: boolean;
}

export interface RoomState {
    roomId: string;
    players: Player[];
    maxPlayers: number;
    currentPhase: GamePhase;
    dayNumber: number;
    nightNumber: number;
    roleCounts: Record<Role, number>;
    hostPlays: boolean;
    phaseEndTime: number;
    previousProtectedPlayerId?: string;
}

export interface ChatMessage {
    id: string;
    sender: string;
    text: string;
    isSystem?: boolean;
}
