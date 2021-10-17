package server.player;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import DataStructures.ThreadedQueue;

public class PlayerServerState {
	
	static class LeaderboardRequest{

		public int requestType;
		public int offset;
		public int size;
		public boolean addOwnEntry;
		LeaderboardRequest(int requestType,int offset,int size,boolean addOwnEntry){
			this.requestType = requestType;
			this.offset = offset;
			this.size = size;
			this.addOwnEntry = addOwnEntry;
		}
		public LeaderboardRequest(LeaderboardRequest leaderboardRequestData) {

			this.addOwnEntry = leaderboardRequestData.addOwnEntry;
			this.requestType = leaderboardRequestData.requestType;
			this.offset = leaderboardRequestData.offset;
			this.size = leaderboardRequestData.size;
		}
	}
	
	private AtomicBoolean playerIsOnline;
	
	private AtomicBoolean signed;//if the player logged in and passed all validation checks
	private AtomicBoolean playerReady;//if theplayer finished loading the map on the client side and is ready to play.
	
	private AtomicBoolean udpTestPacketRecieved;//temporary variable which checks if the UDP testpacked was recieved
	private AtomicBoolean udpConnectionEstablished;//if the udp connection was tested and verified

	private AtomicBoolean mapSended; //was the map completely sent to the player?
	
	private AtomicBoolean sendNewMapInit;//if a new map has to be sended to the player
	
	private AtomicBoolean sendStartRoundMessage; //if a message was sent which tells the player that he can move around
	//used 
	
	private AtomicBoolean sendEndRoundMessage;
	
	
	private AtomicBoolean isAdmin;
	
	private AtomicLong lastTcpSendTime;
	
	private ThreadedQueue<LeaderboardRequest> leaderboardRequestData;

	
	private AtomicBoolean udpConfirmPacketSend;
	
	private AtomicBoolean serverPasswordPassed;
	
	private AtomicReference<String> passwordSalt;
	

	
	public PlayerServerState(){
		

		leaderboardRequestData = new ThreadedQueue<LeaderboardRequest>();
		
		udpTestPacketRecieved = new AtomicBoolean(false);//temporary variable which checks if the UDP testpacked was recieved
		udpConnectionEstablished = new AtomicBoolean(false);//if the udp connection was tested and verified
		
		sendNewMapInit = new AtomicBoolean(true);//
		mapSended = new AtomicBoolean(false);
		signed = new AtomicBoolean(false);
		playerReady = new AtomicBoolean(false);
		sendStartRoundMessage = new AtomicBoolean(false);
		
		sendEndRoundMessage = new AtomicBoolean(false);
				
		isAdmin = new AtomicBoolean(false);
		
		lastTcpSendTime = new AtomicLong(System.currentTimeMillis());
		
		udpConfirmPacketSend = new AtomicBoolean(false);
		
		serverPasswordPassed = new AtomicBoolean(false);
		
		playerIsOnline = new AtomicBoolean(false);
		
		passwordSalt = new AtomicReference<String>("");
	}
	
	public void addLeaderboardRequest(LeaderboardRequest request){
		leaderboardRequestData.offerConcurrent(new LeaderboardRequest(request));
	}
	
	
	public boolean isAdmin(){
		return isAdmin.get();
	}
	
	public void setAdmin(boolean val){
		isAdmin.set(val);
	}
	
	public long getLastTcpSendTimeMS(){
		return lastTcpSendTime.get();
	}
	
	
	public void resetLastTcpSendTime(){
		lastTcpSendTime.set(System.currentTimeMillis());
	}
	
	public LeaderboardRequest pollLeaderboardRequested(){
		return leaderboardRequestData.pollConcurrent();
	}
	
	public void setSendEndRoundMessage(boolean set){
		sendEndRoundMessage.set(set);
	}
	
	public boolean getSendEndRoundMessage(){
		return sendEndRoundMessage.get();
	}
	
	
	public void setSendStartRoundMessage(boolean send){
		sendStartRoundMessage.set(send);
	}
	
	public boolean getSendStartRoundMessage(){
		return sendStartRoundMessage.get();
	}
	
	public boolean getPlayerReady(){
		return playerReady.get();
	}
	
	public void setPlayerReady(boolean ready){
		this.playerReady.set(ready);
	}
	
	public void setMapSended(boolean sended){
		this.mapSended.set(sended);
	}

	public boolean getMapSended(){
		return mapSended.get();
	}
	
	public boolean isSigned(){
		return signed.get();
	}

	public void setSigned(boolean signed){
		this.signed.set(signed);
	}
	
	
	public boolean requestedNewMapInit(){
		return sendNewMapInit.get();
	}
	
	public String getPasswordSalt() {
		return new String(passwordSalt.get());
	}
	
	public void setPasswordSalt(String salt) {
		this.passwordSalt.set(salt);
	}
	
	/**
	 * Method which sends a "reload map" command to the client.
	 * (Is being called when the server switches maps/gamemodes.)
	 */
	public void startNewMapInit(){
		sendNewMapInit.set(true);
	}
	
	
	public void newMapInitDone(){
		sendNewMapInit.set(false);
	}
	
	public boolean getUdpTestPacketRecieved() {
		return udpTestPacketRecieved.get();
	}

	public void setUdpTestPacketRecieved(boolean udpTestPacketRecieved) {
		this.udpTestPacketRecieved.set(udpTestPacketRecieved);
	}

	public boolean getUdpConnectionEstablished() {
		return udpConnectionEstablished.get();
	}

	public void setUdpConnectionEstablished(boolean udpConnectionEstablished) {
		this.udpConnectionEstablished.set(udpConnectionEstablished);
	}

	public boolean getUdpConfirmPacketSend(){
		return udpConfirmPacketSend.get();
	}

	public void setUdpConfirmPacketSend(boolean bool){
		this.udpConfirmPacketSend.set(bool);
	}

	public boolean getServerPasswordPassed() {
		return serverPasswordPassed.get();
	}

	public void setServerPasswordPassed(boolean serverPasswordPassed) {
		this.serverPasswordPassed.set(serverPasswordPassed);
	}
	
	public void setPlayerIsOnline(boolean playerIsOnline) {
		this.playerIsOnline.set(playerIsOnline);
	}
	
	public boolean getPlayerIsOnline() {
		return this.playerIsOnline.get();
	}


}
