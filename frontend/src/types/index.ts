export interface Player {
  deviceId: string;
  displayName: string;
  isHost: boolean;
  connected: boolean;
}

export interface RoomState {
  roomCode: string;
  status: 'WAITING' | 'PLAYING' | 'ENDED';
  hostPlayerId: string;
  players: Player[];
}

export interface ChatMessage {
  senderId: string;
  senderName: string;
  content: string;
  type: 'PUBLIC' | 'SYSTEM' | 'WEREWOLF' | 'PRIVATE_HOST';
  timestamp: string;
}

export interface GamePlayer {
  id: string;
  gameId: string;
  playerId: string;
  displayName: string;
  role: string;
  team: string;
  alive: boolean;
  protectedByBodyguard: boolean;
  disconnected: boolean;
}

export interface GameSessionState {
  id: string;
  roomId: string;
  currentPhase: 'NIGHT' | 'DISCUSSION' | 'VOTING' | 'ENDED';
  currentDay: number;
  phaseStartedAt: string;
  phaseEndAt: string;
  winnerTeam?: string;
  players?: GamePlayer[];
}
