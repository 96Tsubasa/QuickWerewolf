# Project Progress

## Current Status
- **Milestone 1**: Room system (Join, Lobby, Basic Chat) - Mostly implemented.
- **Milestone 2**: Core game engine - In Progress.

## Technical Decisions
- **Realtime Storage (Redis vs PostgreSQL)**: For the initial version and Milestone 2, we are using PostgreSQL (via Spring Data JPA) for both persistent storage and active real-time game state (timers, active votes, actions). This is to simplify the initial engine development. Redis can be introduced later as a performance optimization if necessary.

## Completed Tasks
- Defined basic Room and RoomPlayer entities.
- Implemented RoomService for basic lobby management.
