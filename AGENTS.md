# AGENTS.md

# Webapp Game — Werewolf / Wolvesville-inspired

## Project Goal

Build a mobile-first, browser-based social deduction game inspired by Wolvesville.

Primary goals:

- No account creation
- No app installation
- Players join instantly via room code/link
- Device/client identity instead of authentication
- Works well for groups of friends
- Real-time multiplayer
- Host-controlled game configuration

Maximum players for initial version: **16**

Grid layout: **4 × 4**

---

# Tech Stack

Frontend:
- React
- Vite
- TypeScript
- Zustand
- WebSocket Client

Backend:
- Java
- Spring Boot
- WebSocket

Storage:
- PostgreSQL
- Redis

Deployment (future):
- Docker
- Nginx

---

# Architectural Principles

## 1. Event-driven game architecture

Game state transitions must happen through events.

Examples:

```text
PLAYER_JOINED
GAME_STARTED
PHASE_STARTED
PLAYER_VOTED
ROLE_ACTION
PLAYER_DIED
GAME_ENDED
```

Avoid tightly coupling game logic to UI.

---

## 2. Backend owns game logic

Frontend:
- Rendering
- User interaction
- UI state

Backend:
- Game state
- Timers
- Win checking
- Role actions
- Validation

Frontend must never decide outcomes.

---

## 3. Prefer composition over condition chains

Avoid:

```java
if (role == WEREWOLF)
```

Prefer:

```java
interface Role {

executeAction();

onNight();

onDeath();

checkWin();

}
```

Each role should implement behavior separately.

---

# System Architecture

```text
React + Vite
     │
HTTP + WebSocket
     │
Spring Boot
│
├── Room Service
├── Match Service
├── Game Engine
├── Chat Service
├── Voting Service
├── Timer Scheduler
└── Event Dispatcher

↓

Redis
(PostgreSQL)
```

---

# Frontend Structure

```text
src/

components/

pages/

store/

services/

hooks/

types/

utils/

websocket/
```

Recommended state:

```text
roomStore
gameStore
chatStore
playerStore
```

---

# Backend Structure

```text
src/

controller/

service/

domain/

room/

game/

role/

vote/

entity/

repository/

event/

scheduler/

dto/

websocket/

config/
```

Game logic must live in `domain/`.

---

# Core Modules

## Room Module

Responsibilities:

- Create room
- Join room
- Generate room code
- Lock players after game starts
- Reconnect handling

Room states:

```text
WAITING
PLAYING
ENDED
```

---

## Game Engine

Responsible for:

- Phase progression
- Role actions
- Death resolution
- Win conditions

Structure:

```text
Game
│
├── PhaseManager
├── RoleExecutor
├── ActionResolver
└── WinChecker
```

---

## Chat Module

Channels:

```text
PUBLIC
WEREWOLF
PRIVATE_HOST
```

Rules:

- Public only during day
- Werewolf only at night
- Host private channel always available

---

# Gameplay Rules

## Player Identification

Players:

```text
device/client ID
+
display username
```

No accounts.

---

## Roles (V1)

### Villager

Team:
Village

Ability:
None

---

### Seer

Team:
Village

Night:
Inspect one player.

Result:
Receive target role.

---

### Bodyguard

Team:
Village

Night:
Protect one player.

Rules:

- Can protect self
- Cannot protect same target consecutively
- Prevents end-of-night kills

Protected from:

- Werewolf
- Serial Killer

Future:
Special instant-kill roles may bypass protection.

---

### Werewolf

Team:
Werewolf

Night:

- Vote victim
- Separate wolf chat

Kill resolves end of night.

---

### Serial Killer

Team:
Solo

Night:
Kill one player.

Kill resolves end of night.

Special:

- Immune to werewolf kill

Win:

Last survivor.

---

### Fool

Team:
Solo

Win:

Must be lynched.

Village does not need Fool dead.

---

# Game Flow

Game always starts:

```text
Night 1
```

Default timers:

```text
Night:
30 seconds

Discussion:
120 seconds

Voting:
30 seconds
```

Host may modify.

Loop:

```text
Night

↓

Discussion

↓

Voting

↓

Night
```

---

# Night Resolution Order

Current order:

```text
Bodyguard

↓

Seer

↓

Werewolf Kill

↓

Serial Killer Kill

↓

Apply Death

↓

Check Win
```

Only apply deaths after all actions resolve.

---

# Voting Rules

Day voting:

- Everyone votes

Player is lynched if:

1. Highest vote count
2. Votes ≥ half of surviving players

Example:

```text
11 alive

Minimum lynch:
5 votes
```

---

# Winning Conditions

## Village

Wins if:

- Werewolves eliminated
- Solo killing teams eliminated

Exception:

- Fool may survive

---

## Werewolf

Wins if:

```text
alive_werewolves >= half_of_survivors
AND
solo_killers_dead
```

Example:

```text
1 Villager
1 Fool
2 Werewolves

→ Werewolf Win
```

---

## Serial Killer

Win:

Only survivor.

---

## Fool

Win:

Lynched.

---

# Game Session Rules

When host starts:

- Player list freezes
- Roles assigned randomly
- Disconnect does not remove players
- Host sees assigned roles

Reconnect restores session.

---

# Database

## PostgreSQL

Persistent storage:

- Rooms
- Players
- Match history
- Config

---

## Redis

Realtime storage:

- Active game state
- Timers
- Presence
- Votes
- Actions

---

# PostgreSQL Schema

## rooms

```text
id
room_code
host_player_id
status
max_players
created_at
started_at
```

---

## room_players

```text
id
room_id
device_id
display_name
is_host
connected
joined_at
```

---

## game_sessions

```text
id
room_id
current_phase
current_day
phase_started_at
phase_end_at
winner_team
```

---

## game_players

```text
id
game_id
player_id
role
team
alive
protected
previous_protection_target
disconnected
```

---

## role_actions

```text
id
game_id
actor_id
target_id
action_type
night_number
created_at
```

---

## votes

```text
id
game_id
day
voter_id
target_id
```

---

## chat_messages

```text
id
game_id
sender_id
chat_type
receiver_id
message
created_at
```

---

# Redis Keys

```text
room:{code}

actions:{room}:{night}

votes:{room}:{day}

timer:{room}

online:{room}
```

---

# Networking

HTTP:

```text
POST /rooms
POST /rooms/{id}/join
POST /game/start
```

WebSocket:

```text
/room/{id}

/game/{id}

/chat/{id}
```

Example events:

```text
GAME_STATE

CHAT_MESSAGE

COUNTDOWN

PLAYER_DIED

ROLE_RESULT
```

---

# Development Milestones

## Milestone 1

Room system

- Join
- Chat
- Lobby

---

## Milestone 2

Core game engine

- Phase transitions
- Voting
- Win logic

---

## Milestone 3

Roles

Reconnect

Polish

---

# Future Expansion

Planned support:

- More roles
- Spectator mode
- Multiple rooms per server
- Match replay
- Mobile optimization