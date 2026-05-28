# Game Description:

- The game is created to be played for a group of friends that can easily join via a link, avoid hassles of needing to create accounts or download any apps. Players' identification will be based on device/client ID. When opening the web, players are only required to enter display username.

- User can choose to be either a host (Who will customize the game) or a normal player (Who can join a room that was created by a host). Depend on which option the user pick, the GUI will be different for each type of user

- Host can customize the number of players for the room, which roles will be included in the room. How it works: The Host can pick a role from the database that is not added to the room yet, and edit the counter for each one. Only allow hosts to save the room settings when the total selected roles is equal to the room size limit. Host can also change the room size limit after creating the room, it cannot be lower than the current amount of players already joined the room.

- For the first version of the game, I want to implement these roles first:

  + Villager (Village team): Has no special abilities

  + Seer (Village team): Each night, check a player to see their role

  + Bodyguard (Village team): Each night, select a player to protect them. Can select self. Must be a different player from the previous night. This player can't be killed by end of night kill action (Werewolf team or Serial Killer) 
  
    (In the future there might be special roles that can kill players in the middle of the night that can bypass this protection).

  + Werewolf (Werewolf team): Each night, vote for a player to be killed by the werewolf team at the end of that night. The werewolves will have a seperate chat that can be used at night for discussion. If multiple Werewolves vote for different targets at the end of the night, the final target will be selected randomly from the tied targets.

  + Serial Killer (Solo): Each night, select a player to kill at the end of the night. Cannot be killed by werewolves. Wins if they are the last survivor.

  + Fool (Solo): Only wins if you are lynched at the end of a day.

- Game flow: Start from Night 1. Each night has 30s duration. After Night 1 is Day 1 with discussion phase (120s) that everyone can chat to discuss, voting phase (30s) for everyone to select a player to be lynched. A player will be lynched if they receive the most votes and at least half the surviving players vote to kill them (For example, at least 5 votes is required if there are 11 surviving players left). Same logic as werewolves tie votes at night, a random selection is made when there's vote tie during the day. The duration above are all default values and can be changed by the host. The game continue this loop until any team meet their winning conditions:

  + Village: Eliminate all threatening teams: werewolf and solo teams (Fool is exception, they don't have to be killed for village to win)

  + Werewolf: Half of the surviving players are from werewolf team, and Solo teams are dead (For example, 1 villager, 1 fool and 2 werewolves will be counted as werewolves win; 1 villager, 1 serial killer and 2 werewolves will not be counted as a win yet)

  + Solo/3rd party team: They have their own winning condition that is described in the role description

# Additional Info

- The Host can decide to play the game or not before creating the room, and if they decide to play, they will get the same treatment as other players after the game has started.

- When the host create a room, the game will generate a room ID and other players can join the room with that ID. After the host select to start the game, if the host did not select to play the game, the list of the players for that game will be locked and won't change even if any device disconnected, and the host can see the list of players and which role were assigned to each of them. Roles are randomly assigned for every player. There will be a grid (Let's make 4 x 4 grid for now, max 16 players) so that players can interact with other players using their abilities by clicking on the grid. And there will be a shared chat for everyone that can be used during daytime for discussion. There will be a separated chat between each player and the host (The host might need to share information privately with players in some cases)

- If a player is disconnected, we simply treat them as "AFK" players that do not commit any actions or votings, but still count to winning conditions.

- Also display a line in the shared chat at start of each phase, for example: "Night 1 started", "Day 1 started - Discussion time", "Voting time".