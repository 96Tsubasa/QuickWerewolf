import { useState, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import { useGameStore } from '../store/gameStore';
import { Settings, Users, ArrowRight } from 'lucide-react';

export const Home = () => {
    const [name, setName] = useState('');
    const [roomCode, setRoomCode] = useState('');
    const [mode, setMode] = useState<'HOME' | 'CREATE' | 'JOIN'>('HOME');
    const navigate = useNavigate();
    
    const { setUserInfo, createRoom, joinRoom, deviceId, leaveRoom } = useGameStore();

    useEffect(() => {
        leaveRoom();
    }, [leaveRoom]);

    const handleCreate = async () => {
        if (!name.trim()) return;
        setUserInfo(deviceId, name);
        await createRoom();
        const { roomState } = useGameStore.getState();
        if (roomState) navigate(`/room/${roomState.roomId}`);
    };

    const handleJoin = async () => {
        if (!name.trim() || !roomCode.trim()) return;
        setUserInfo(deviceId, name);
        try {
            await joinRoom(roomCode);
            navigate(`/room/${roomCode}`);
        } catch (e) {
            alert('Failed to join room. Check code.');
        }
    };

    return (
        <div className="home-container">
            <div className="glass-panel main-panel">
                <h1 className="title">QuickWerewolf</h1>
                <p className="subtitle">Real-time social deduction game</p>
                
                {mode === 'HOME' && (
                    <div className="action-buttons">
                        <button className="primary-btn" onClick={() => setMode('CREATE')}>
                            <Settings size={20} /> Host Game
                        </button>
                        <button className="secondary-btn" onClick={() => setMode('JOIN')}>
                            <Users size={20} /> Join Game
                        </button>
                    </div>
                )}
                
                {mode === 'CREATE' && (
                    <div className="form-container">
                        <input 
                            type="text" 
                            placeholder="Enter your Display Name" 
                            value={name} 
                            onChange={(e) => setName(e.target.value)} 
                            className="input-field"
                        />
                        <button className="primary-btn" onClick={handleCreate} disabled={!name.trim()}>
                            Create Room <ArrowRight size={20} />
                        </button>
                        <button className="text-btn" onClick={() => setMode('HOME')}>Back</button>
                    </div>
                )}
                
                {mode === 'JOIN' && (
                    <div className="form-container">
                        <input 
                            type="text" 
                            placeholder="Display Name" 
                            value={name} 
                            onChange={(e) => setName(e.target.value)} 
                            className="input-field"
                        />
                        <input 
                            type="text" 
                            placeholder="Room Code" 
                            value={roomCode} 
                            onChange={(e) => setRoomCode(e.target.value.toUpperCase())} 
                            className="input-field"
                        />
                        <button className="secondary-btn" onClick={handleJoin} disabled={!name.trim() || !roomCode.trim()}>
                            Join Room <ArrowRight size={20} />
                        </button>
                        <button className="text-btn" onClick={() => setMode('HOME')}>Back</button>
                    </div>
                )}
            </div>
        </div>
    );
};
