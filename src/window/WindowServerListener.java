package window;

import server.ListenerHandler.ServerListener;

/**
 * ServerListener implementation for the Window GUI.
 * 
 * 
 *
 */

public class WindowServerListener implements ServerListener{

	
	Window window;
	public WindowServerListener(Window win){
		window = win;
	}
	
	//Functions like this SHOULD be synchronized as they can be called from multiple threads at once
	
	@Override
	public synchronized void consoleLinePrint(String message) {
		// TODO Auto-generated method stub
		window.consolePrintln(message);
		
	}

	@Override
	public synchronized void playerJoined(int id, String playerName) {
		// TODO Auto-generated method stub
		window.getPlayerListModel().addElement(new PlayerListEntry(id,playerName));
	}

	@Override
	public synchronized void playerLeft(int id) {
		// TODO Auto-generated method stub
		window.getPlayerListModel().removeElement(id);
		
	}
}
