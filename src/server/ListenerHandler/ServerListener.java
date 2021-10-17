package server.ListenerHandler;

/**
 * Listener interface. A class can implement this interface and
 * register itself on the server to recieve messages from the server.
 * 
 * As an example, the "consoleLinePrint()" method will be called if the Server prints
 * a console line and "playerJoined()" will be called if a new player joins the server.
 * 
 * NOTE: All calls CAN happen asynchronously while the server is running (multithreaded!).
 * So be aware of concurrency issues.
 * 
 * 
 *
 */

public interface ServerListener {
	public void consoleLinePrint(String message);
	
	public void playerJoined(int id,String playerName);
	
	public void playerLeft(int id);
}
