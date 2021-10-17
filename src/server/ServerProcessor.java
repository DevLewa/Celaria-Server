package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import DataStructures.TcpMessage;
import DataStructures.ThreadedQueue;
import masterserver.HttpConnection;
import server.game.GameState;
import server.leaderboard.LeaderboardEntry;
import server.leaderboard.LeaderboardState;
import server.player.Player;
import server.player.PlayerProcessor.TcpServerMessage;
import server.util.BufferConverter;
import server.util.FileUtil;
import server.util.Util;

public class ServerProcessor implements Runnable{
	
	


	public static class LeaderboardAdd{
		private LeaderboardState.LEADERBOARDADDTYPE type;
		private LeaderboardEntry entry;
		public LeaderboardAdd(LeaderboardState.LEADERBOARDADDTYPE type,LeaderboardEntry entry) {
			this.type = type;
			this.entry = entry;
		}
		
		public LeaderboardState.LEADERBOARDADDTYPE getType() {
			return type;
		}
		
		public LeaderboardEntry getEntry() {
			return entry;
		}
	};
	
	public boolean run;

	private int sleepTime = 1000/ServerCore.TICKS_PER_SECOND;//10;
	ServerCore core;

	String mapDirectory = "./maps/";
	int currentMapIndex = -1;

	int lastPlayerCount;
	long refreshPlayerCountTime;


	
	public long httpRefreshTimeStep = 90L*1000L;//60 second HTTP refresh. DO NOT MODIFY!
	
	public long playerCountHTTPsendDelay = 6L*1000;
	
	long lastHTTPrefresh;

	private ThreadedQueue<LeaderboardAdd> newLeaderboardEntryList;//only used for checking and displaying chatmessages for the leaderboardstate

	private ThreadedQueue<Integer> extendTimeRequest;
	


	boolean sendRefreshPlayerCount;
	
	

	
	public ServerProcessor(ServerCore core){
		run = true;

		this.core = core;

		lastPlayerCount = 0;
		
		newLeaderboardEntryList = new ThreadedQueue<LeaderboardAdd>();
		extendTimeRequest = new ThreadedQueue<Integer>();


		sendRefreshPlayerCount = false;

		refreshPlayerCountTime = 0;
	}
	
	
	//=============== HTTP ==============
	private synchronized HttpConnection.ResponseMap sendHTTPregister() {
		
		lastHTTPrefresh = System.currentTimeMillis();

		ServerConfig serverConfig = core.getServerConfig();
		return HttpConnection.registerOnline(serverConfig.getServer_online_name(), serverConfig.getUdpPort(),core.getPlayerCount(),serverConfig.getMaxPlayerCount(),serverConfig.getPassword() != null,ServerCore.SERVER_PROTOCOL_VERSION);
	}


	public synchronized HttpConnection.ResponseMap sendHTTPregisterTimed() {
		return sendHTTPregister();
	}

	
	public boolean parseRegisterResponse(HttpConnection.ResponseMap response) {
		boolean error = false;
		if(response.hasKey("refreshTimestep") && response.hasKey("playerCountRefreshDelay")){
			int seconds = (Integer.parseInt(response.getValue("refreshTimestep")));
			//hardcoded limit (DO NOT MODIFY)
			seconds = Math.max(seconds,45);
			httpRefreshTimeStep = seconds*1000L;
			
			
			int delay = Integer.parseInt(response.getValue("playerCountRefreshDelay"));
			delay = Math.max(delay, 3);//DO NOT MODIFY
			playerCountHTTPsendDelay = delay*1000L;
			
			
		}else {
			error = true;
		}
		return error;
	}
	//==========================================
	
	
	

	void extendTime(int seconds) {
		extendTimeRequest.offerConcurrent(seconds);
	}
	
	public ThreadedQueue<LeaderboardAdd> getNewLeaderboardEntryList(){
		return newLeaderboardEntryList;
	}


	public void reset(){
		currentMapIndex = -1;
	}

	public void setRunning(boolean run){
		this.run = run;
	}


	@Override
	public void run() {

		while(run == true){
			GameState state = core.getGameState();
			//sends HTTP refresh in intervals to the server

			if(core.getServerConfig().onlineMode() == true){

				int currentPlayerCount = core.getPlayerCount();
				if(lastPlayerCount != currentPlayerCount) {
					lastPlayerCount = currentPlayerCount;

					sendRefreshPlayerCount = true;

					if(refreshPlayerCountTime == 0) {
						refreshPlayerCountTime = System.currentTimeMillis() + playerCountHTTPsendDelay;//DO NOT MODIFY!
					}
				}
				
				
				if((lastHTTPrefresh+httpRefreshTimeStep) <System.currentTimeMillis()) {
					HttpConnection.ResponseMap response = sendHTTPregister();
					if(response != null) {
						if(response.getSuccess() == false) {
							parseRegisterResponse(response);
							
							sendRefreshPlayerCount = false;
							refreshPlayerCountTime = 0;
						}
					}
				}
							
				if(sendRefreshPlayerCount) {
					if(refreshPlayerCountTime <= System.currentTimeMillis()) {
						sendHTTPregister();
						sendRefreshPlayerCount = false;
						refreshPlayerCountTime = 0;
					}
				}
				
				
			}
			
			
			//process extend time refresh
			if(extendTimeRequest.isEmptyConcurrent() == false) {
				int timeAdd = extendTimeRequest.pollConcurrent();//retrieves and removes
				core.getGameState().addRoundTime(timeAdd);
				
				core.sendTcpBroadcastChatMessage(ChatMessageColors.gamestateColor, "Extended time by "+timeAdd+" seconds.");
				
				//send visual time extend TCP message!
				
				ByteBuffer b = ByteBuffer.allocate(4);
				BufferConverter.buffer_write_u32(b, timeAdd);
				
				core.sendTcpBroadcast(new TcpMessage(TcpServerMessage.ROUNDTIME_EXTEND,b));
				
				
				
			}

			//searches the mapdirectory if maplist doesnt exist
			if(core.getRessourceHandler().getMapFileList() == null){
				core.consolePrintln("Searching map-directory...");
				String[] mapFileList = FileUtil.getMapFileList(mapDirectory);

				core.getRessourceHandler().setMapFileList(mapFileList);
				core.consolePrintln("Search complete. Found "+mapFileList.length+" .cmap files.");

			}

			//loads a new map
			if(core.getRessourceHandler().getMapFileList() != null){
				if(core.getRessourceHandler().getMapFileList().length>0){

					if(core.getRessourceHandler().getMapHandler().mapDataIsNull() == true || (state.isEndRoundTimerFinished()==true && state.getEndRoundTimerStarted() == true)){			
						currentMapIndex++;
						if(currentMapIndex >= core.getRessourceHandler().getMapFileList().length){
							currentMapIndex = 0;
						}

						core.consolePrintln("Loading Map with index ["+currentMapIndex+"] "+'"'+core.getRessourceHandler().getMapFileList()[currentMapIndex]+'"'+" ...");

						try {

							byte[] mapData = FileUtil.loadBinaryFile(mapDirectory+core.getRessourceHandler().getMapFileList()[currentMapIndex]);

							ByteBuffer mapBuffer = ByteBuffer.wrap(mapData);
							boolean success = core.getRessourceHandler().getMapHandler().setMap(mapBuffer);

							if(success == true){

								//Iterate through all players and initialise new map loading
								ConcurrentHashMap<Integer, Player> map = core.getPlayers();
								Iterator<Integer> it = map.keySet().iterator();

								while(it.hasNext()){
									Integer key = (Integer) it.next();
									Player player = (Player) map.get(key);
									if(player != null){//safety check
										player.initNewMapLoading();
										//clear checkpoints

										player.getCheckpointQueue().clearConcurrent();

									}
								}

								state.setRoundRunning(false);
								state.setEndRoundTimerStarted(false);


								it = map.keySet().iterator();

								while(it.hasNext()){
									Integer key = (Integer) it.next();
									Player player = (Player) map.get(key);
									if(player != null){//safety check
										player.getPlayerServerData().setSendStartRoundMessage(false);//will be sent/set to true from the playerRealtimeHandler

									}
								}

								//starts end round timer which displays the highscore on the client for the given time before the new round starts
								state.startNewRoundDelay();

								core.consolePrintln("Map Loaded ...");

							}else{
								core.consolePrintln("Unable to load map: "+'"'+core.getRessourceHandler().getMapFileList()[currentMapIndex]+'"'+" : invalid map format!");
							}

						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}

					//process map switch request
					if(state.nextMapRequested()){
						state.resetNextMapRequest();
						state.finishRoundEarly();
					}

					//Ends a round if the time is over (gameplay will stop for around 10-15 seconds in order to examine the leaderboard)
					if(state.isRoundFinished() ==true && state.getRoundRunning() == true){

						state.setRoundRunning(false);
						state.startEndRoundTimer();



						ConcurrentHashMap<Integer, Player> map = core.getPlayers();
						Iterator<Integer> it = map.keySet().iterator();

						while(it.hasNext()){
							Integer key = (Integer) it.next();
							Player player = (Player) map.get(key);
							if(player != null){//safety check
								player.sendRoundEnded();
							}
						}

						//clients will send leaderboard requests to the server in order to display the leaderboard on the client

					}

					//sort highscoreState

					if(newLeaderboardEntryList.isEmptyConcurrent() == false) {//prevent unnessecary write lock in poll() if list is empty
						
						LeaderboardAdd entryAdd = newLeaderboardEntryList.pollConcurrent();
						LeaderboardEntry entry = entryAdd.getEntry();
						while(entryAdd != null){

							if(entry.timeSet() == true){
								LeaderboardEntry topEntry = core.getGameState().getHighscoreState().getTopEntry();


								if((entry.timeSet() == true && topEntry.timeSet() == true && entry.getTime() < topEntry.getTime())
										|| (entry.timeSet() == true && topEntry.timeSet() == false)
										){
									core.sendTcpBroadcastChatMessage(ChatMessageColors.gamestateColor, "Player ["+entry.getPlayername()+"] is now leading! Time to beat: "+Util.timeFormat(entry.getTime()));
								}
							}
							
							
							core.getGameState().getHighscoreState().addEntry(entryAdd.type,entry);

							entryAdd = newLeaderboardEntryList.pollConcurrent();
						}
					}


					if(state.isDelayFinished() ==true && state.getDelayStarted() == true){
						state.setDelayStarted(false);
						core.getGameState().getHighscoreState().resetTimes();
						state.startNewRound();
					}


				}
			}


			try {
				Thread.sleep(sleepTime);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
