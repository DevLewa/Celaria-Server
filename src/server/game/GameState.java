package server.game;

import debug.DebugIO;
import server.ServerConfig;
import server.leaderboard.LeaderboardState;


public class GameState {

	//time of a round
	long roundTimeMS;
	long roundTargetTimeMS;
	
	//delay before round start
	long delayTimeMS;
	long delayTargetTimeMS;
	
	
	long endRoundTimeMS;
	long endRoundTargetTimeMS;
	
	//flags
	boolean delayStarted;
	boolean roundStarted;
	boolean endRoundTimerStarted;
	
	private int gameMode;
	
	private boolean switchToNextMap;
	
	
	
	private LeaderboardState highscore;
	
	public GameState(ServerConfig config){
		gameMode = 1;
		
		
		roundTimeMS = (long) (config.getRoundTimeSec()*1000L);
		roundTargetTimeMS = System.currentTimeMillis();
		
		delayTimeMS = 10*1000L;//10 second delay before the round starts. (players can't move until then) 
		//>> little delay to wait so that the majority of clients synchronized with the game (maptransfer,etc..)
		delayTargetTimeMS = System.currentTimeMillis();
		

		
		endRoundTimeMS = 15*1000L;//15 second time delay at the end of a round (displaying leaderboards)
		endRoundTargetTimeMS = System.currentTimeMillis();
		
		
		
		//Flags
		endRoundTimerStarted = false;
		delayStarted = false;
		roundStarted = false;
		
		switchToNextMap = false;
		
		highscore = new LeaderboardState();
	}
	
	public void reset(){
		highscore = new LeaderboardState();
	}
	
	public LeaderboardState getHighscoreState(){
		return highscore;
	}

	public void addRoundTime(int seconds) {
		roundTargetTimeMS+=seconds*1000L;
	}
	
	public boolean nextMapRequested(){
		return switchToNextMap;
	}
	
	public void resetNextMapRequest(){
		switchToNextMap = false;
	}
	
	public boolean requestNextMapRotation(){
		if(isDelayFinished() && isRoundFinished() == false && isEndRoundTimerFinished()==true){
			switchToNextMap = true;
			return true;
		}else{
			return false;
		}
	}
	
	
	public int getGameMode(){
		return gameMode;
	}
	
	
	public boolean getEndRoundTimerStarted(){
		return endRoundTimerStarted;
	}
	
	public void setEndRoundTimerStarted(boolean set){
		this.endRoundTimerStarted = set;
	}
	
	
	public void startEndRoundTimer(){
		setEndRoundTimerStarted(true);
		endRoundTargetTimeMS = System.currentTimeMillis()+endRoundTimeMS;
	}
	

	
	public void setRoundRunning(boolean set){
		this.roundStarted = set;
	}
	
	public boolean getRoundRunning(){
		return roundStarted;
	}
	
	public void startNewRoundDelay(){
		setDelayStarted(true);
		
		delayTargetTimeMS = System.currentTimeMillis()+delayTimeMS;
		DebugIO.println("STARTING NEW ROUND DELAY!");
	}

	
	public void startNewRound(){
		setRoundRunning(true);
		roundTargetTimeMS = System.currentTimeMillis()+roundTimeMS;
		DebugIO.println("STARTING NEW ROUND!");
	}
	
	public void finishRoundEarly(){
		roundTargetTimeMS = System.currentTimeMillis()+5000;//5 seconds
	}
	
	public boolean getDelayStarted(){
		return delayStarted;
	}
	
	public void setDelayStarted(boolean set){
		this.delayStarted = set;
	}
	
	public boolean isDelayFinished(){
		if(System.currentTimeMillis() >= delayTargetTimeMS){
			return true;
		}else{
			return false;
		}
	}
	
	public boolean isEndRoundTimerFinished(){
		
		if(System.currentTimeMillis() >= endRoundTargetTimeMS){
			return true;
		}else{
			return false;
		}
	}
	
	/**
	 * Returns the remaining time (in ticks) of the round.
	 * 1 second = 60 ticks.
	 * 
	 * @return
	 */
	public int getRemainingRoundTimeTicks(){
		float diffSeconds = (((roundTargetTimeMS-System.currentTimeMillis()))/1000);
		int ticks = (int) Math.ceil(diffSeconds*100.0f);
		if(ticks <=0){ticks = 0;}
		return ticks;
	}
	
	public boolean isRoundFinished(){
		if(System.currentTimeMillis() >= roundTargetTimeMS){
			return true;
		}else{
			return false;
		}
	}
}
