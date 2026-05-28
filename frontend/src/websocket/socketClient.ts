import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';
import type { RoomState, ChatMessage } from '../types';
import { useRoomStore } from '../store/roomStore';
import { useChatStore } from '../store/chatStore';
import { usePlayerStore } from '../store/playerStore';

class SocketClient {
  private client: Client | null = null;
  private currentRoomCode: string | null = null;

  connect(roomCode: string, onKicked: () => void) {
    this.currentRoomCode = roomCode;
    this.client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      onConnect: () => {
        console.log('Connected to WebSocket');
        
        // Subscribe to state updates
        this.client?.subscribe(`/topic/room/${roomCode}/state`, (message) => {
          const state: RoomState = JSON.parse(message.body);
          useRoomStore.getState().setRoomState(state);
        });

        // Subscribe to chat
        this.client?.subscribe(`/topic/room/${roomCode}/chat`, (message) => {
          const chatMsg: ChatMessage = JSON.parse(message.body);
          useChatStore.getState().addMessage(chatMsg);
        });

        // Subscribe to kicked notification
        this.client?.subscribe(`/topic/room/${roomCode}/kicked`, (message) => {
          const kickedDeviceId = message.body;
          if (kickedDeviceId === usePlayerStore.getState().deviceId) {
             onKicked();
          }
        });
      },
      onStompError: (frame) => {
        console.error('Broker reported error: ' + frame.headers['message']);
      },
    });
    this.client.activate();
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
  }

  sendChat(message: Omit<ChatMessage, 'timestamp'>) {
    if (this.client && this.client.connected && this.currentRoomCode) {
      this.client.publish({
        destination: `/app/room/${this.currentRoomCode}/chat`,
        body: JSON.stringify(message),
      });
    }
  }
  subscribeToGame(roomId: string, callback: (state: import('../types').GameSessionState) => void) {
    this.client?.subscribe(`/topic/game/${roomId}/state`, (message) => {
      callback(JSON.parse(message.body));
    });
  }

  subscribeToChat(roomId: string, callback: (msg: ChatMessage) => void) {
    this.client?.subscribe(`/topic/chat/${roomId}`, (message) => {
      try {
        const parsed = JSON.parse(message.body);
        callback(parsed);
      } catch (e) {
        callback({ senderId: 'SYSTEM', senderName: 'SYSTEM', content: message.body, type: 'SYSTEM', timestamp: new Date().toISOString() } as unknown as ChatMessage);
      }
    });
  }

  subscribeToWerewolfChat(roomId: string, callback: (msg: ChatMessage) => void) {
    this.client?.subscribe(`/topic/chat/${roomId}/werewolf`, (message) => {
      callback(JSON.parse(message.body));
    });
  }

  subscribeToPrivateChat(roomId: string, playerId: string, callback: (msg: ChatMessage) => void) {
    this.client?.subscribe(`/topic/chat/${roomId}/private/${playerId}`, (message) => {
      callback(JSON.parse(message.body));
    });
  }

  sendAction(gameId: string, actorId: string, targetId: string, actionType: string) {
    this.client?.publish({
      destination: `/app/game/${gameId}/action`,
      body: JSON.stringify({ actorId, targetId, actionType })
    });
  }

  sendVote(gameId: string, voterId: string, targetId: string) {
    this.client?.publish({
      destination: `/app/game/${gameId}/vote`,
      body: JSON.stringify({ voterId, targetId })
    });
  }

  sendChatMessage(roomId: string, senderId: string, chatType: string, receiverId: string | null, message: string) {
    this.client?.publish({
      destination: `/app/chat/${roomId}/send`,
      body: JSON.stringify({ senderId, chatType, receiverId, message })
    });
  }
}

export const socketClient = new SocketClient();
