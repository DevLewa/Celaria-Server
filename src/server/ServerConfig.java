package server;

import io.ConfFileReader;

public class ServerConfig {
	private int maxPlayers;
	private float playerUdpTimeoutSec;
	private float playerTcpTimeoutSec;

	private int networkPort;
	
	private String server_online_name;

	private String hostAddress;


	String mapDirectory;

	private double roundTimeSec;

	private String[] mapList;//example: "test.cmap";


	private String adminPassword;

	private boolean server_online;

	private final long tcpStillAliveTimePeriodMS = 15L*1000L;//10 seconds


	private final int client_UDP_refreshrate = 10;//how many times a second the player updates it's data with the server (client data like position,animation,rotation, etc...)
	//this is mererly a suggestion. The client can still ignore this.
	
	private String password;

	public ServerConfig(){
		mapDirectory = "./maps/";
		maxPlayers = 1;
		playerUdpTimeoutSec = 30f;
		playerTcpTimeoutSec = 30f;

		networkPort = 6510;
		
		roundTimeSec = 60*15;//10 minutes;

		hostAddress = null;

		adminPassword = null;

		mapList = null;

	
		
		password = null;

		

		setServer_online_name("<default>");
		setServer_online(false);
	}


	public void apply(ConfFileReader reader){
		if(reader.exists("maxPlayerCount")){
			setMaxPlayerCount(Integer.parseInt(reader.get("maxPlayerCount")));
		}

		if(reader.exists("roundTime")){
			setRoundTimeSec(Integer.parseInt(reader.get("roundTime")));
		}

		if(reader.exists("port")){
			setPort(Integer.parseInt(reader.get("port")));
		}
		
		if(reader.exists("mapDirectory")){
			setMapDirectory(reader.get("mapDirectory"));
		}

		if(reader.exists("adminPassword")){
			setAdminPassword(reader.get("adminPassword"));
		}


		
		if(reader.exists("password")){
			String key = reader.get("password");
			if(key.equals("") == false) {
				setPassword(reader.get("password"));
			}
		}
		
		if(reader.exists("serverList_register")){
			String onlineVal = reader.get("serverList_register");

			onlineVal = onlineVal.toLowerCase();
			if(onlineVal.equals("true")) {
				setServer_online(true);				
			}else {
				setServer_online(false);
			}
		}
		if(reader.exists("onlineName")){
			setServer_online_name(reader.get("onlineName"));
		}




	}
	
	public void setPort(int port) {
		this.networkPort = port;
	}

	public int getClientUdpRefreshRate() {
		return client_UDP_refreshrate;
	}

	//TCP and UDP use always the same port (for simplicity purposes)
	public int getUdpPort() {
		return networkPort;
	}

	public int getTcpPort() {
		return networkPort;
	}

	public String getAdminPassword(){
		return adminPassword;
	}

	public void setAdminPassword(String password){
		adminPassword = password;
	}
	
	
	public boolean hasPassword() {
		return password != null;
	}

	public String getPassword(){
		return password;
	}

	public void setPassword(String password){
		this.password = password;
	}




	public String getMapDirectory(){
		return mapDirectory;
	}

	public void setHostAddress(String address){
		this.hostAddress = address;
	}

	public String getHostAddress(){
		return this.hostAddress;
	}

	public float getUdpTimeout(){
		return playerUdpTimeoutSec;
	}

	public float getTcpTimeout(){
		return playerTcpTimeoutSec; 
	}


	public long getTcpStillAliveIntervallMS(){
		return tcpStillAliveTimePeriodMS;
	}


	public void setMapDirectory(String directory){
		mapDirectory = directory;
	}

	public void setTcpTimeout(float seconds){
		playerTcpTimeoutSec = seconds;
	}

	public void setUdpTimeout(float seconds){
		playerUdpTimeoutSec = seconds;
	}

	public void setMapList(String[] maplist){
		this.mapList = maplist;
	}

	public void setMaxPlayerCount(int playercount){
		this.maxPlayers = playercount;
		if(this.maxPlayers>=32){
			this.maxPlayers = 32;
		}
		if(this.maxPlayers<=1){
			this.maxPlayers = 1;
		}
	}

	public int getMaxPlayerCount(){
		return maxPlayers;
	}

	public double getRoundTimeSec(){
		return roundTimeSec;
	}

	public void setRoundTimeSec(double seconds){
		this.roundTimeSec = seconds;
	}

	public String[] getMapList(){
		return mapList;
	}


	public boolean onlineMode() {
		return server_online;
	}


	public void setServer_online(boolean server_online) {
		this.server_online = server_online;
	}


	public String getServer_online_name() {
		return server_online_name;
	}


	public void setServer_online_name(String server_online_name) {
		this.server_online_name = server_online_name;
	}

}
