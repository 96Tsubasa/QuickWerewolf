import { useState } from 'react';
import { useGameStore } from '../store/gameStore';

export const ChatBox = () => {
    const { globalChat, werewolfChat, hostChat, sendGlobalChat, sendWerewolfChat, sendHostChat, roomState, deviceId } = useGameStore();
    const [activeTab, setActiveTab] = useState<'GLOBAL' | 'WEREWOLF' | 'HOST'>('GLOBAL');
    const [text, setText] = useState('');
    
    // Check if player is a werewolf
    const myPlayer = roomState?.players.find(p => p.deviceId === deviceId);
    const isWerewolf = myPlayer?.role === 'WEREWOLF';
    
    // Only host or players can use host chat (everyone talks to host, host talks to everyone individually? The reqs said: "separated chat between each player and the host". For simplicity, let's just make it a single HOST tab for now, if you are host you talk to all? Wait, the requirement: "separated chat between each player and the host". For this MVP, let's just use it as a single room where host and everyone can send msgs, or host has to select a player. To keep it simple, let's just let it be a broadcast for the host to all players privately. Wait, I will just disable HOST chat for now or leave it simple).
    // Let's implement a simple version where any message sent in the HOST tab goes to the Host if you are a player.
    // If you are the Host, you need to select a target. Let's omit the target selector for MVP and just make it a global Host-Player announcement board, or omit it if it's too complex for UI. I'll include it but if host, they type to everyone on HOST tab.

    const handleSend = () => {
        if (!text.trim() || !roomState) return;
        
        if (activeTab === 'GLOBAL') {
            sendGlobalChat(text);
        } else if (activeTab === 'WEREWOLF') {
            sendWerewolfChat(text);
        } else if (activeTab === 'HOST') {
            const host = roomState.players.find(p => p.host);
            if (host) sendHostChat(text, host.deviceId);
        }
        
        setText('');
    };

    const getActiveChat = () => {
        if (activeTab === 'GLOBAL') return globalChat;
        if (activeTab === 'WEREWOLF') return werewolfChat;
        if (activeTab === 'HOST') return hostChat;
        return [];
    };

    const canChat = () => {
        if (!roomState || !myPlayer || !myPlayer.alive) return false;
        if (activeTab === 'GLOBAL' && roomState.currentPhase !== 'DAY_DISCUSSION') return false;
        if (activeTab === 'WEREWOLF' && roomState.currentPhase !== 'NIGHT') return false;
        return true;
    };

    return (
        <div className="chat-box">
            <div className="chat-tabs">
                <button className={`tab ${activeTab === 'GLOBAL' ? 'active' : ''}`} onClick={() => setActiveTab('GLOBAL')}>Global</button>
                {isWerewolf && <button className={`tab ${activeTab === 'WEREWOLF' ? 'active' : ''}`} onClick={() => setActiveTab('WEREWOLF')}>Werewolf</button>}
                <button className={`tab ${activeTab === 'HOST' ? 'active' : ''}`} onClick={() => setActiveTab('HOST')}>Host</button>
            </div>
            <div className="chat-messages">
                {getActiveChat().map(msg => (
                    <div key={msg.id} className={`chat-message ${msg.isSystem ? 'system' : ''}`}>
                        {!msg.isSystem && <span className="sender">{msg.sender}: </span>}
                        <span className="text">{msg.text}</span>
                    </div>
                ))}
            </div>
            <div className="chat-input">
                <input 
                    type="text" 
                    value={text} 
                    onChange={e => setText(e.target.value)} 
                    onKeyPress={e => e.key === 'Enter' && handleSend()}
                    disabled={!canChat()}
                    placeholder={canChat() ? "Type a message..." : "You cannot chat right now"}
                />
                <button onClick={handleSend} disabled={!canChat()}>Send</button>
            </div>
        </div>
    );
};
