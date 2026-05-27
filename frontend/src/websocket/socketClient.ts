import { Client } from '@stomp/stompjs';
import SockJS from 'sockjs-client';

class SocketClient {
  private client: Client | null = null;

  connect(roomId: string, onMessage: (msg: any) => void) {
    this.client = new Client({
      webSocketFactory: () => new SockJS('http://localhost:8080/ws'),
      onConnect: () => {
        console.log('Connected to WebSocket');
        this.client?.subscribe(`/topic/room/${roomId}`, (message) => {
          onMessage(JSON.parse(message.body));
        });
      },
      onStompError: (frame) => {
        console.error('Broker reported error: ' + frame.headers['message']);
        console.error('Additional details: ' + frame.body);
      },
    });
    this.client.activate();
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
    }
  }

  sendMessage(destination: string, body: any) {
    if (this.client && this.client.connected) {
      this.client.publish({ destination, body: JSON.stringify(body) });
    }
  }
}

export const socketClient = new SocketClient();
