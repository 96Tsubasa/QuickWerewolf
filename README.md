# QuickWerewolf

QuickWerewolf is a mobile-first, browser-based social deduction game inspired by Wolvesville. It is designed to be played by a group of friends who can easily join a room via a link or room code, without the hassle of creating accounts or downloading any apps.

## Features
- **No accounts required:** Instant access via device/client ID.
- **Mobile-first design:** Optimized for mobile browsers but playable on desktop.
- **Real-time multiplayer:** Powered by WebSockets for seamless game flow.
- **Host controls:** A dedicated host can customize the game settings, number of players, and roles.

## Tech Stack
- **Frontend:** React, Vite, TypeScript, Zustand, `@stomp/stompjs`
- **Backend:** Java 17, Spring Boot, Spring WebSocket, Spring Data JPA
- **Storage:** PostgreSQL (Persistent), Redis (Realtime state)

---

## Getting Started

Follow these instructions to get a copy of the project up and running on your local machine for development and testing purposes.

### Prerequisites

Make sure you have the following installed on your system:
- [Docker](https://www.docker.com/) and Docker Compose (for running PostgreSQL and Redis)
- [Java 17](https://adoptium.net/) (or higher)
- [Maven](https://maven.apache.org/) (optional, you can use the wrapper)
- [Node.js](https://nodejs.org/) (v18 or higher recommended)

### 1. Start the Databases
The project relies on PostgreSQL and Redis. You can easily start them using the provided `docker-compose.yml`.

Navigate to the project root and run:
```bash
docker-compose up -d
```
*(This will start a PostgreSQL instance on port `5432` and a Redis instance on port `6379`)*

### 2. Start the Backend (Spring Boot)
Open a new terminal and navigate to the `backend` directory. Run the Spring Boot application using Maven:

```bash
cd backend
mvn spring-boot:run
```
*(The backend will start and listen on port `8080`)*

### 3. Start the Frontend (Vite + React)
Open another terminal and navigate to the `frontend` directory. Install the dependencies and start the development server:

```bash
cd frontend
npm install
npm run dev
```
*(The frontend will start, usually on `http://localhost:5173`)*

---

## Contributing
Please read the [AGENTS.md](./AGENTS.md) and [GameDescription.md](./GameDescription.md) files for deeper insights into the project architecture, event-driven systems, and game rules.
