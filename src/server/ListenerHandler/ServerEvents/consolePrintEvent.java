package server.ListenerHandler.ServerEvents;

import server.ListenerHandler.ServerEvent;
import server.ListenerHandler.ServerListener;

public class consolePrintEvent implements ServerEvent{

	private String message;
	
	public consolePrintEvent(String message){
		this.message = message;
	}
	
	@Override
	public void execute(ServerListener listener) {
		// TODO Auto-generated method stub
		listener.consoleLinePrint(message);
	}

}
