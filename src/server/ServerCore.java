package server;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.NetworkInterface;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import DataStructures.TcpMessage;
import debug.DebugIO;
import server.ListenerHandler.ListenerHandler;
import server.Server.TCPDISCONNECTTYPE;
import server.ServerProcessor.LeaderboardAdd;
import server.connection.BroadcasterController;
import server.connection.RecieverController;
import server.connection.TcpNetworkConnector;
import server.connection.TcpPlayerWriter;
import server.game.GameState;
import server.leaderboard.LeaderboardEntry;
import server.leaderboard.LeaderboardState;
import server.player.Player;
import server.player.PlayerProcessor;
import server.util.BufferConverter;
import server.util.StreamConverter;
import server.util.Util;
import masterserver.HttpConnection;
import masterserver.HttpConnection.Response;
import masterserver.ServerConnectionTest;

/**
 * Class which implements all the server logic. Not accessible from outside.
 * (everything has to go through the "Server" class.)
 * 
 * 
 *
 */

public class ServerCore {

	// Has to match the same value in the client (for checking if communication protocol is the same)
	public static final int SERVER_PROTOCOL_VERSION = 4;

	public static final boolean MODIFIED = false;

	public static final int TICKS_PER_SECOND = 10;

	// ---------------------------------------------------------

	ServerSocketChannel serverChannel;

	private DatagramSocket socket_udp;// udp socket
	private boolean run = false;// Define the boolean run to keep the server running
	private ConcurrentHashMap<Integer, Player> players;// Create an array list to store all the players

	private ConcurrentHashMap<SocketChannel, Player> socketChannelToPlayerMap;// Create an array list to store all the
																				// players

	private ExecutorService tcpServerFullPool = null;// Executorservice with short lived threads which simply respond to
														// clients with a "server is full" message after connecting with
														// the server.
	// (these threads are writing to the sockets)

	// handles the direct TCP communication with the clients (read and write)
	TcpNetworkConnector tcpConnector;

	private RessourceHandler ressources;

	private BroadcasterController broadCaster;// TCP and UDP broadcaster
	private RecieverController reciever;// TCP and UDP reciever

	public boolean stopped;// if the server is running

	private ListenerHandler listeners;

	// Locks
	// Playermapblock
	/*
	 * Even though the map is already a concurrenthashmap, to avoid any issues in
	 * operations like reading and writing simultaneously, this lock comes into
	 * play. (it secures such operations with one global lock)
	 */
	private ReentrantReadWriteLock playerListLock;
	private Lock readPlayerListLock;
	private Lock writePlayerListLock;

	private ServerConfig serverConfig;

	ServerProcessor serverRealtime;
	Thread serverRealtimeThread;

	PlayerProcessor playerProcessor;
	Thread playerProcessorThread;

	GameState state;

	public ServerCore(ServerConfig conf) {

		serverConfig = conf;

		state = new GameState(conf);

		players = new ConcurrentHashMap<Integer, Player>();

		socketChannelToPlayerMap = new ConcurrentHashMap<SocketChannel, Player>();

		playerListLock = new ReentrantReadWriteLock();
		readPlayerListLock = playerListLock.readLock();
		writePlayerListLock = playerListLock.writeLock();

		listeners = new ListenerHandler();

		ressources = new RessourceHandler(this); // handles map files

		serverRealtime = new ServerProcessor(this);

		playerProcessor = new PlayerProcessor(this);

	}

	public ListenerHandler getListenerHandler() {
		return listeners;
	}

	public GameState getGameState() {
		return state;
	}

	private boolean testOnline() {
		// =============================
		// =========== Handle Online Connectivity ============
		if (serverConfig.onlineMode()) {
			this.consolePrintln("Onlinemode enabled");

			this.consolePrintln("Testing internet connection");
			Response ipResponse = HttpConnection.getIP();
			if (ipResponse.success == false) {
				this.consolePrintln("Error: Unable to obtain IP-address");
				return false;
			} else {

				ServerConnectionTest connectionTest = new ServerConnectionTest(ipResponse.content,
						serverConfig.getTcpPort());

				boolean checkPassed = connectionTest.checkBlocked();
				if (checkPassed == false) {
					this.consolePrintln(
							"Internet connection verification failed. Please check your firewall and/or port-settings.");
					return false;

				} else {

					this.consolePrintln("Connecting to masterserver...");
					this.consolePrintln("Registering server...");
					HttpConnection.ResponseMap response = serverRealtime.sendHTTPregisterTimed();

					if (response.getSuccess() == false) {
						// error
						this.consolePrintln("ERROR: unable to connect to the masterserver.");
						return false;
					} else {
						boolean registerError = serverRealtime.parseRegisterResponse(response);

						if (registerError == false) {
							this.consolePrintln("Registering successful.");
						} else {
							this.consolePrintln("Registration error.");

							return false;
						}
					}
				}

			}

		}

		return true;
	}

	public void startServer() {

		this.consolePrintln("=== Starting Server ===");// Display the message that the server is starting

		boolean mapListOK = false;
		if (serverConfig.getMapList() != null) {
			if (serverConfig.getMapList().length > 0) {
				mapListOK = true;
			}
		}
		if (mapListOK == false) {

			this.consolePrintln("Error: no mapfiles defined!");
			this.consolePrintln("");
			return;
		}

		state.reset();

		if (serverConfig.onlineMode()) {
			boolean success = testOnline();
			if (success == false) {
				return;
			}
		}

		serverRealtime.setRunning(true);
		serverRealtimeThread = new Thread(serverRealtime, "Server Realtime");
		serverRealtimeThread.start();

		playerProcessor.setRunning(true);
		playerProcessorThread = new Thread(playerProcessor, "Player Processor");
		playerProcessorThread.start();

		try {
			socket_udp = new DatagramSocket(serverConfig.getUdpPort());

		} catch (SocketException e1) {
			// TODO Auto-generated catch block
			this.consolePrintln("Unable to create UDP socket on port: " + serverConfig.getUdpPort()
					+ ". Please check if this port is not already used.");
			closeServer();
			return;
		} // udp

		run = true;
		stopped = false;

		players.clear();

		socketChannelToPlayerMap.clear();

		tcpServerFullPool = Executors.newFixedThreadPool(4);

		// create a new serversocketchannel. The channel is unbound.
		try {
			serverChannel = ServerSocketChannel.open();

		} catch (IOException e) {
			// TODO Auto-generated catch block
			this.consolePrintln("Unable to open serverChannel.");
			closeServer();
			return;
		}

		InetSocketAddress socketAddress = null;
		int socketPort = serverConfig.getTcpPort();
		if (this.serverConfig.getHostAddress() != null
				&& this.serverConfig.getHostAddress().equals("default") == false) {
			socketAddress = new InetSocketAddress(this.serverConfig.getHostAddress(), socketPort);
		} else {
			socketAddress = new InetSocketAddress(socketPort);// uses wildcard address (binds to all ports)
		}

		try {
			serverChannel.bind(socketAddress);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			this.consolePrintln(
					"Unable to bind TCP port: " + socketPort + ". Please check if this port is not already used.");
			closeServer();
			return;
		}

		// mark the serversocketchannel as non blocking
		try {
			serverChannel.configureBlocking(false);

		} catch (IOException e) {
			// TODO Auto-generated catch block
			this.consolePrintln("Error: cannot configure serversocket to non-blocking I/O");
			closeServer();
			return;
		}

		broadCaster = new BroadcasterController(this, socket_udp);

		reciever = new RecieverController(this);

		tcpConnector = new TcpNetworkConnector(this);

		this.consolePrintln("=== Server Started ===");// Display the message that the server started

	}

	public void addLeaderboardEntry(LeaderboardEntry e) {
		// ------ Used for calculating if a player set a new record in the given round
		serverRealtime.getNewLeaderboardEntryList()
				.offerConcurrent(new LeaderboardAdd(LeaderboardState.LEADERBOARDADDTYPE.REPLACE, e));
		// ------
	}

	public void addLeaderboardEntry(LeaderboardState.LEADERBOARDADDTYPE type, LeaderboardEntry e) {
		// ------ Used for calculating if a player set a new record in the given round
		serverRealtime.getNewLeaderboardEntryList().offerConcurrent(new LeaderboardAdd(type, e));
		// ------

	}

	public ConcurrentHashMap<SocketChannel, Player> getSocketChannelToPlayerMap() {
		return socketChannelToPlayerMap;
	}

	public boolean getRunning() {
		return run;
	}

	public void closeServer() {
		run = false;

		consolePrintln("=== Closing server ===");

		stopReciever();// stops UDP reciever

		stopBroadcaster();

		stopAllPlayerThreads();// also clears the hashmap

		try {
			stopSockets();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			this.consolePrintln("Error encountered while closing sockets.");
		}

		if (tcpConnector != null) {
			tcpConnector.close();
			tcpConnector = null;
		}

		tcpServerFullPool = null;

		if (serverRealtime != null) {
			serverRealtime.setRunning(false);
			serverRealtime.reset();
		}

		if (serverRealtimeThread != null) {
			try {
				serverRealtimeThread.join();
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			serverRealtimeThread = null;
		}

		ressources.reset();// resets loaded maps and any other types of ressoruces

		// remove server from online database
		if (serverConfig.onlineMode()) {
			HttpConnection.removeFromOnline(serverConfig.getTcpPort());
		}

		consolePrintln("=== Server closed ===");
		consolePrintln("");

	}

	public void stopReciever() {
		if (reciever != null) {
			reciever.stop();
			reciever = null;
		}
	}

	public void stopBroadcaster() {
		if (broadCaster != null) {
			broadCaster.stop();
			broadCaster = null;
		}
	}

	public void stopAllPlayerThreads() {

		writePlayerListLock.lock();

		ConcurrentHashMap<Integer, Player> pl = getPlayers();
		for (int i = 0; i < getMaxPlayerCount(); i++) {

			int id = i;
			if (pl.containsKey(id) == true) {
				Player p = pl.get(id);

				p.closePlayer(TCPDISCONNECTTYPE.SERVERCLOSED);

			}
		}
		writePlayerListLock.unlock();

		if (tcpConnector != null) {
			tcpConnector.stop();
		}

		playerProcessor.setRunning(false);
		try {
			playerProcessorThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		players.clear();

	}

	public ServerConfig getServerConfig() {
		return serverConfig;
	}

	public void consolePrintln(String string) {
		listeners.getEventCaller().consolePrintln(string);
	}

	public void stopSockets() throws IOException {

		if (serverChannel != null) {
			serverChannel.socket().close();
			serverChannel.close();
			serverChannel = null;
		}

		// socket_udp.disconnect();
		if (socket_udp != null) {
			socket_udp.close();
			socket_udp = null;
		}

		DebugIO.println("SocketsStopped");

	}

	// returns a available playerID
	public int getNewPlayerID() {
		int newKey = 0;
		readPlayerListLock.lock();

		int i = 0;

		while (players.containsKey(i) == true) {
			i++;
		}
		newKey = i;
		readPlayerListLock.unlock();

		return newKey;
	}

	public void removePlayer(int id) {

		// removes player from highscore
		getGameState().getHighscoreState().remove(id);

		writePlayerListLock.lock();// Using a lock in order to make sure that nothing accesses the playerHashmap
									// between containskey() and remove().

		if (players.containsKey(id)) {
			players.remove(id);

		} else {
			DebugIO.println("player with ID " + id + " not removed as it doesnt exist?!");
		}
		writePlayerListLock.unlock();
	}

	public ServerSocketChannel getServerSocketChannel() {
		return serverChannel;

	}

	public DatagramSocket getServerSocketUDP() {
		return socket_udp;
	}

	public TcpNetworkConnector getTcpConnector() {
		return tcpConnector;
	}

	public ExecutorService getTcpServerFullPool() {
		return tcpServerFullPool;

	}

	public ConcurrentHashMap<Integer, Player> getPlayers() {
		return players;

	}

	public void sendTcpBroadcast(TcpMessage message) {
		if (broadCaster != null) {
			broadCaster.getTcpBroadcaster().addTcpBroadcastMessage(message);
		}
	}

	public void sendTcpBroadcastChatMessage(String string) {
		sendTcpBroadcastChatMessage(ChatMessageColors.defaultColor, string);
	}

	public void extendTime(int seconds) {
		serverRealtime.extendTime(seconds);
	}

	public void sendTcpChatMessage(Player player, Color color, String string) {

		if (broadCaster != null) {

			ByteBuffer b = ByteBuffer.allocate((string.length() * 2) + 4);

			// color of the text
			BufferConverter.buffer_write_u8(b, (byte) color.getRed());
			BufferConverter.buffer_write_u8(b, (byte) color.getGreen());
			BufferConverter.buffer_write_u8(b, (byte) color.getBlue());

			// string
			BufferConverter.buffer_write_u8(b, (byte) string.length());
			BufferConverter.buffer_write_chars_u16(b, string);

			sendTcpMessageLocked(player, PlayerProcessor.TcpServerMessage.CHATMESSAGE, b);

		}
	}

	public void sendTcpBroadcastChatMessage(Color color, String string) {

		if (broadCaster != null) {

			ByteBuffer b = ByteBuffer.allocate((string.length() * 2) + 4);

			// color of the text
			BufferConverter.buffer_write_u8(b, (byte) color.getRed());
			BufferConverter.buffer_write_u8(b, (byte) color.getGreen());
			BufferConverter.buffer_write_u8(b, (byte) color.getBlue());

			// string
			BufferConverter.buffer_write_u8(b, (byte) string.length());
			BufferConverter.buffer_write_chars_u16(b, string);

			broadCaster.getTcpBroadcaster()
					.addTcpBroadcastMessage(new TcpMessage(PlayerProcessor.TcpServerMessage.CHATMESSAGE, b));

		}
	}

	public String[] getPlayerArray() {
		String[] players = new String[] { "test" };
		return players;
	}

	public void sendTcpMessageLocked(Player p, int messageID, ByteBuffer buffer) {
		p.getOutpoutMessageQueue().offerConcurrent(new TcpMessage(messageID, buffer));
	}

	public int getPlayerCount() {
		int s = 0;
		readPlayerListLock.lock();

		if (players != null) {
			s = getPlayers().size();
		}
		readPlayerListLock.unlock();
		return s;
	}

	public int getMaxPlayerCount() {
		return serverConfig.getMaxPlayerCount();
	}

	public RessourceHandler getRessourceHandler() {
		return ressources;
	}

}