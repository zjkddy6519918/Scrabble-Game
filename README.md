# Scrabble-Game
This is a classical crossword game based on Java which supports multiple users.<br>  
Specifically, it uses TCP to implement multi-player games and uses multithreading to allow the server side to manipulate the game process without conflicts.<br>  
To start the gane, first run GameServer.java to start a server, of which the IP address is that of your own computer by default. Then you can run GameClient.java to join the game by inputting the IP address of the server. You can also invite players who have logged in but not in the game yet. In the game, if you think you have made a word, you can start a vote and get a score if other players agree with that.
