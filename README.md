# Celaria Server

This is the source code of the standalone multiplayer server for the Game "Celaria". (https://www.celaria.com)
of modding the server sided multiplayer functionality of the game 

This server source code was released for the purposes of modding the server sided multiplayer functionality of the game.

### Note

This code was one of my first attempts at writing a multithreaded codebase and was refactored
multiple times (partially and/or fully) each time with different approaches during development. Hence it's a mix of different codingstyles
with multiple different solutions to the same issue (like thread locking) and thus probably an example how to (not) write a game server.

Nowadays i would've approached the coding structure differently (due to more experience compared to when this was written).

If you intend on modding and hosting your modified server, make sure to set the "MODIFIED" variable in the "ServerCore.java" file to "true".
This way your server will recieve a special icon in the serverbrowser as well as indicate the game client that the server may have altered behaviour.
```
public static final boolean MODIFIED = true;
```
-----------


### Dependencies
There are sections of the source code wich are dependend on "Gson" so you will have to link the project against a build of this library:
https://github.com/google/gson