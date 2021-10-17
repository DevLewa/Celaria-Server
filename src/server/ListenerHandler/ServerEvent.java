package server.ListenerHandler;

/**
 * An instance of this class represents an Serverevent (like player joined, player left, chatmessage sent, etc...)
 * A class with this interface has to implement the "execute" method which calls the appropiate functions
 * of the serverListener.
 * 
 * 
 *
 */

public interface ServerEvent {
	
	/**
	 * Executes the serverevent on the given serverListener
	 * @param listener
	 */
	public void execute(ServerListener listener);

}
