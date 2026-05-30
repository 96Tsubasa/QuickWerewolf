import { useState } from "react";
import { useGameStore } from "../store/gameStore";

export const ChatBox = () => {
  const {
    globalChat,
    werewolfChat,
    sendGlobalChat,
    sendWerewolfChat,
    roomState,
    deviceId,
  } = useGameStore();
  const [activeTab, setActiveTab] = useState<"GLOBAL" | "WEREWOLF">("GLOBAL");
  const [text, setText] = useState("");

  // Check if player is a werewolf
  const myPlayer = roomState?.players.find((p) => p.deviceId === deviceId);
  const isWerewolf = myPlayer?.role === "WEREWOLF";

  const handleSend = () => {
    if (!text.trim() || !roomState) return;

    if (activeTab === "GLOBAL") {
      sendGlobalChat(text);
    } else if (activeTab === "WEREWOLF") {
      sendWerewolfChat(text);
    }

    setText("");
  };

  const getActiveChat = () => {
    if (activeTab === "GLOBAL") return globalChat;
    if (activeTab === "WEREWOLF") return werewolfChat;
    return [];
  };

  const canChat = () => {
    if (!roomState || !myPlayer || !myPlayer.alive) return false;
    if (
      activeTab === "GLOBAL" &&
      roomState.currentPhase !== "DAY_DISCUSSION" &&
      roomState.currentPhase !== "DAY_VOTING"
    )
      return false;
    if (activeTab === "WEREWOLF" && roomState.currentPhase !== "NIGHT")
      return false;
    return true;
  };

  return (
    <div className="chat-box">
      <div className="chat-tabs">
        <button
          className={`tab ${activeTab === "GLOBAL" ? "active" : ""}`}
          onClick={() => setActiveTab("GLOBAL")}
        >
          Global
        </button>
        {isWerewolf && (
          <button
            className={`tab ${activeTab === "WEREWOLF" ? "active" : ""}`}
            onClick={() => setActiveTab("WEREWOLF")}
          >
            Werewolf
          </button>
        )}
      </div>
      <div className="chat-messages">
        {getActiveChat().map((msg) => (
          <div
            key={msg.id}
            className={`chat-message ${msg.isSystem ? "system" : ""}`}
          >
            {!msg.isSystem && <span className="sender">{msg.sender}: </span>}
            <span className="text">{msg.text}</span>
          </div>
        ))}
      </div>
      <div className="chat-input">
        <input
          type="text"
          value={text}
          onChange={(e) => setText(e.target.value)}
          onKeyPress={(e) => e.key === "Enter" && handleSend()}
          disabled={!canChat()}
          placeholder={
            canChat() ? "Type a message..." : "You cannot chat right now"
          }
        />
        <button onClick={handleSend} disabled={!canChat()}>
          Send
        </button>
      </div>
    </div>
  );
};
