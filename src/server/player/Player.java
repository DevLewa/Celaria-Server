package server.player;//Define the package where the class is located

import java.net.InetAddress;

//diese klasse k�mmert sich um Logik
/**
 * This class handles stores all Playerinformations.
 * Creates a Playerconnectionhandler Instanze for Player-specific communication.
 * 
 * 
 */

import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.io.IOException;
import DataStructures.TcpMessage;
import DataStructures.ThreadedQueue;
import debug.DebugIO;
import server.ChatMessageColors;
import server.ServerCore;
import server.Server.TCPDISCONNECTTYPE;
import server.connection.PlayerTimings;
import server.connection.TcpDisconnectMessage;
import server.player.data.NetworkCountData;
import server.player.data.PlayerRuntimeUdpData;
import server.player.data.PlayerProfileData;
import server.player.data.PlayerSkin;

/**
 * Main player class. Stores data as well as additional logic (seperated in sub-classes).
 * 
 * TODO: Will need proper refractoring as the code (in the current state) is quite messy...
 * 
 * 
 *
 */

public class Player{


	private ServerCore server;

	private AtomicInteger ID;
	SocketChannel socket;

	
	
	private PlayerRuntimeUdpData playerData;
	private PlayerSkin playerSkin;
	private PlayerProfileData playerProfileData;


	//only used for UDP
	private InetAddress internetAdress;
	private AtomicInteger udpPort;

	private AtomicInteger udpKey;//it's an unsigned short (0-65535) but due to the absence of unsigned datatypes in java, we have to use an int to store values in the (positive) range of 0-65535

	private PlayerServerState playerServerData;
	
	//signed= if the player is officially in the game (= was accepted by the server after sending all necessary information which also has been verified)

	private NetworkCountData networkCountData;
	
	private ThreadedQueue<Integer> checkpointTimes;


	private PlayerLocks locks;//various locks for multithreading purposes. (Potential optimization)
	ThreadedQueue<Integer> playerRequestList = new ThreadedQueue<Integer>();//IDs of players that the client requested to get information for.
	private boolean playerClosed;//if the player was closed/destroyed (used for multithreading to prevent calling the closePlayer() function multiple times.)


	private ThreadedQueue<TcpMessage> tcpInputMessages;
	private ThreadedQueue<TcpMessage> tcpOutputMessages;
	private PlayerTimings timings;
	private MapTransmitter mapTransmitter;


	public Player(ServerCore serv ,int id, SocketChannel sck){
		socket=sck;//Set the received socket to the player class
		server = serv;//Server pointer
		locks = new PlayerLocks();
		
		playerProfileData = new PlayerProfileData();
		networkCountData = new NetworkCountData();

		playerServerData = new PlayerServerState();

		timings = new PlayerTimings();


		
		mapTransmitter = new MapTransmitter(this);


		this.ID = new AtomicInteger(id);

		//udp
		internetAdress = null;
		udpPort = new AtomicInteger(-1);//-1 is not a valid port


		applyPlayerData(new PlayerRuntimeUdpData());
		playerSkin = new PlayerSkin();

		playerClosed = false;
		udpKey = new AtomicInteger((int)Math.floor(Math.random()*65536));//to authentificate the player on the udp side (Best way would be to exclude already used up udp keys
		DebugIO.println("UDP KEY SET: "+udpKey);



		tcpInputMessages = new ThreadedQueue<TcpMessage>();
		tcpOutputMessages = new ThreadedQueue<TcpMessage>();

		checkpointTimes = new ThreadedQueue<Integer>();
		
		server.getSocketChannelToPlayerMap().put(getSocketChannel(), this);
		server.getTcpConnector().registerPlayer(this);



	}

	//is concurrent
	public MapTransmitter getMapTransmitter(){
		return mapTransmitter;
	}

	//Is concurrent
	public PlayerServerState getPlayerServerData(){
		return playerServerData;
	}

	//Is concurrent
	public PlayerTimings getTimings(){
		return timings;
	}

	//is concurrent
	public ThreadedQueue<Integer> getCheckpointQueue(){
		return checkpointTimes;
	}
	//is concurrent
	public ThreadedQueue<TcpMessage> getInputMessageQueue(){
		return tcpInputMessages;
	}
	//is concurrent
	public ThreadedQueue<TcpMessage> getOutpoutMessageQueue(){
		return tcpOutputMessages;
	}

	//is concurrent
	public InetAddress getInternetAddress(){
		locks.getInternetAddressLock().readLock().lock();
		try{
			return internetAdress;
		}finally{
			locks.getInternetAddressLock().readLock().unlock();
		}
	}

	//TODO: Fix concurrency
	public SocketChannel getSocketChannel(){
		return socket;
	}
	//is concurrent
	public int getID(){
		return ID.get();
	}



	//is concurrent
	public int getUdpPort(){
		return udpPort.get();

	}
	//is concurrent
	public void setInternetAddress(InetAddress i){
		locks.getInternetAddressLock().writeLock().lock();
		try{
			this.internetAdress = i;
		}finally{
			locks.getInternetAddressLock().writeLock().unlock();
		}
	}
	//is concurrent
	public void setUdpPort(int i){
		this.udpPort.set(i);
	}

	public int getUdpKey(){
		return udpKey.get();
	}

	public void initNewMapLoading(){
		this.getPlayerServerData().setMapSended(false);
		this.getMapTransmitter().reset();
		this.getPlayerServerData().startNewMapInit();
	}

	/**
	 * request the leaderboardData for the client
	 */

	public void sendRoundEnded(){
		this.getPlayerServerData().setSendEndRoundMessage(false);
	}


	public PlayerRuntimeUdpData getPlayerDataCopy() {
		locks.getPlayerDataLock().readLock().lock();
		try{
			return new PlayerRuntimeUdpData(playerData);//playerData.getCopy();
		}finally{
			locks.getPlayerDataLock().readLock().unlock();
		}
	}
	
	
	public PlayerProfileData getPlayerProfileData() {
		return playerProfileData;
	}
	
	public NetworkCountData getNetworkCounter() {
		return networkCountData;
	}


	public PlayerSkin getPlayerSkinCopy(){
		locks.getPlayerSkinLock().readLock().lock();
		try{
			return new PlayerSkin(playerSkin);
		}finally{
			locks.getPlayerSkinLock().readLock().unlock();
		}

	}

	public void applyPlayerSkin(PlayerSkin skin){
		locks.getPlayerSkinLock().writeLock().lock();
		try{
			this.playerSkin = skin;
		}finally{
			locks.getPlayerSkinLock().writeLock().unlock();
		}

	}
	
	
	public void applyPlayerData(PlayerRuntimeUdpData playerData) {
		locks.getPlayerDataLock().writeLock().lock();
		try{
			this.playerData = playerData;
		}finally{
			locks.getPlayerDataLock().writeLock().unlock();
		}
	}

	public void setPlayerDataValues(PlayerRuntimeUdpData playerData) {
		locks.getPlayerDataLock().writeLock().lock();
		try{
			this.playerData.apply(playerData);//applies variables instead of making a new copy of the instance (more heap friendly?)
		}finally{
			locks.getPlayerDataLock().writeLock().unlock();
		}
	}

	public void sendPlayerOfflineBroadcast() throws IOException{
		//send packet (BROADCAST!)
		ByteBuffer b = ByteBuffer.allocate(1);
		b.put((byte) ID.get());
		server.sendTcpBroadcast(new TcpMessage(PlayerProcessor.TcpServerMessage.PLAYER_DISCONNECT,b));

	}

	public ServerCore getServer(){
		return server;
	}

	public void sendPlayerJoinedToListener(){
		server.getListenerHandler().getEventCaller().playerJoined(getID(),playerProfileData.getPlayerName());
	}

	/* Shuts this thread down and closes all underlying ressources (also the second thread)
	 * has to be synchronized in order to avoud concurrency issues 
	 * as this function can be called by multiple instances
	 */
	//TODO: FIX CONCURRENCY ISSUES!
	
	public void closePlayer() {
		closePlayer(TCPDISCONNECTTYPE.DEFAULT,false,"");//doesn't send disconnect message
	}
	
	public void closePlayer(TCPDISCONNECTTYPE disconnectType) {
		closePlayer(disconnectType,true,"");//doesn't send disconnect message
	}
	
	public void closePlayer(TCPDISCONNECTTYPE disconnectType,String message) {
		closePlayer(disconnectType,true,message);//doesn't send disconnect message
	}
	
	private void closePlayer(TCPDISCONNECTTYPE disconnectType,boolean sendDisconnect,String message){

		if(playerClosed == false){//avoids calling this method multiple times
			DebugIO.println("Closing  player ID: "+ID);
			playerClosed = true;//set to true to avoid a second call

			
			server.getSocketChannelToPlayerMap().remove(getSocketChannel());
			server.getTcpConnector().removePlayer(this);

			server.getListenerHandler().getEventCaller().playerLeft(getID());


			server.consolePrintln("Player-ID: "+ID+" disconnect");

			//send disconnectMessage. (Seperate threadpool.) also closes socket.
			if(sendDisconnect) {
				TcpDisconnectMessage disconnectMessage = new TcpDisconnectMessage(socket,disconnectType,message);
				server.getTcpServerFullPool().execute(disconnectMessage);
			}

			if(this.playerServerData.isSigned() == true){//send disconnect information only if the player was originally verified.
				try {
					sendPlayerOfflineBroadcast();
				} catch (IOException e1) {
					// TODO Auto-generated catch block
					e1.printStackTrace();
				}




				locks.getPlayerDataLock().readLock().lock();
				String name = playerProfileData.getPlayerName();
				locks.getPlayerDataLock().readLock().unlock();


				server.sendTcpBroadcastChatMessage(ChatMessageColors.serverNotificationColor,"Player "+'"'+name+'"'+" disconnected.");
			}		
			server.removePlayer(ID.get());
			DebugIO.println("PLAYER CLOSED");
		}
	}



	public ThreadedQueue<Integer> getRequestedPlayerIDs(){
		return playerRequestList;
	}

}

