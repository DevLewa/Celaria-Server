package server.ListenerHandler.ServerEvents;

import server.ListenerHandler.ServerEvent;
import server.ListenerHandler.ServerListener;

public class PlayerLeftEvent implements ServerEvent{

	private int id;
	
	public PlayerLeftEvent(int playerID){
		this.id = playerID;
	}
	
	@Override
	public void execute(ServerListener listener) {
		// TODO Auto-generated method stub
		listener.playerLeft(id);
	}
	
}
