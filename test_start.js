const http = require('http');

async function test() {
  try {
    // 1. Create Room
    const createReq = await fetch('http://localhost:8082/api/rooms', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ deviceId: 'test-host', displayName: 'Host' })
    });
    const roomState = await createReq.json();
    console.log('Room created:', roomState.roomCode);

    // 2. Start Game
    const startReq = await fetch(`http://localhost:8081/api/rooms/${roomState.roomCode}/start`, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ hostDeviceId: 'test-host', selectedRoles: ['VILLAGER'] })
    });
    
    if (!startReq.ok) {
        console.error('Start failed with status:', startReq.status);
        const text = await startReq.text();
        console.error('Response text:', text);
    } else {
        console.log('Game started successfully');
    }
  } catch (e) {
    console.error(e);
  }
}

test();
