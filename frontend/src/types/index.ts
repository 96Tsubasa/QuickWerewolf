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
