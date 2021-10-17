package server.ListenerHandler.ServerEvents;

import server.ListenerHandler.ServerEvent;
import server.ListenerHandler.ServerListener;

public class PlayerJoinedEvent implements ServerEvent{

	private int playerID;
	private String name;
	
	public PlayerJoinedEvent(int id,String name){
		this.playerID = id;
		this.name = name;
	}
	
	@Override
	public void execute(ServerListener listener) {
		// TODO Auto-generated method stub
		listener.playerJoined(playerID, name);
	}

}
