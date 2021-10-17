package server;

import java.io.IOException;

import server.ListenerHandler.ServerListener;
import server.util.FileUtil;

/**
 * Main Server class which can be accessed from external classes.
 * 
 * 
 *
 */

public class Server {

	public enum TCPDISCONNECTTYPE {
		DEFAULT, KICK, SERVERFULL, SERVERCLOSED, INCOMPATIBLESERVERVERSION, UDPTIMEOUT, TCPTIMEOUT, WRONGPASSWORD,
		PROCESSINGERROR, PROTOCOL_VIOLATION, ONLINEMODE_REQUIRED
	}

	private ServerCore core;// the actual Server(core) which implements all of the logic
	private ServerConfig config; // configuration class

	public final String SERVER_VERSION = "1.0.8";

	public Server() {
		this(new ServerConfig());
	}

	public Server(ServerConfig config) {

		this.config = config;
		config.setMapList(FileUtil.getMapFileList(config.getMapDirectory()));

		core = new ServerCore(this.config);
	}

	public void addServerListener(ServerListener listener) {
		core.getListenerHandler().addServerListener(listener);
	}

	/**
	 * If the serverthread is currently running
	 * 
	 * @return is the server running
	 */
	public boolean isRunning() {
		return core.getRunning();
	}

	/**
	 * starts the server
	 */
	public void start() {
		core.startServer();
	}

	/**
	 * Closes the server
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		core.closeServer();
	}

	/**
	 * gets the number of currently connected players
	 * 
	 * @return
	 */
	public int getPlayerCount() {
		return core.getPlayerCount();
	}
}
