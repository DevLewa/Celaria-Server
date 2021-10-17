package window;

import server.ListenerHandler.ListenerHandler;
import server.ListenerHandler.ServerEvent;
import server.ListenerHandler.ServerEvents.PlayerJoinedEvent;
import server.ListenerHandler.ServerEvents.PlayerLeftEvent;
import server.ListenerHandler.ServerEvents.consolePrintEvent;

public class ListenerEventCalls {

	public ListenerHandler listenerHandler;
	
	public ListenerEventCalls(ListenerHandler handler){
		listenerHandler = handler;
	}
	
	public void playerJoined(int id,String name){
		ServerEvent e = new PlayerJoinedEvent(id,name);
		listenerHandler.addEventToTempList(e);
		
	}
	
	public void playerLeft(int id){
		ServerEvent e = new PlayerLeftEvent(id);
		listenerHandler.addEventToTempList(e);
	}
	
	public void consolePrintln(String message){
		ServerEvent e = new consolePrintEvent(message);
		listenerHandler.addEventToTempList(e);
	}
	
}
