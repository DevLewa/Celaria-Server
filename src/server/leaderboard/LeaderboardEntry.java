package server.leaderboard;

public class LeaderboardEntry implements Comparable<LeaderboardEntry>{
	private long time;//the game client uses unsigned 32 bit integers to store the time.
	//in order to have the same value range available in java we have to go above the "int" datatype.
	private byte medal;//medal (possible values between 0-4)
	private int playerID;//is the server ID
	private String playerName;//playername....
	

	byte badge;
	private boolean timeSet;
	
	public LeaderboardEntry(int playerID,String playerName,byte badge){
		this.time = 0;
		this.playerID = playerID;
		this.playerName = playerName;
		
		this.medal = 0;
		this.timeSet = false;
		this.badge = badge;
		
	}
	
	
	public LeaderboardEntry(int playerID,String playerName,long time,byte medal,byte badge){
		this.time = time;
		this.playerName = playerName;
		
		timeSet = true;

		this.medal = medal;
		this.playerID = playerID;
		this.badge = badge;
	}
	
	public LeaderboardEntry(LeaderboardEntry leaderboardEntry) {
		// TODO Auto-generated constructor stub
		this.playerID = leaderboardEntry.playerID;
		this.playerName = leaderboardEntry.playerName;
		this.time = leaderboardEntry.time;
		this.medal = leaderboardEntry.medal;
		this.timeSet = leaderboardEntry.timeSet;
	}

	public byte getBadge() {
		return badge;
	}

	public boolean timeSet(){
		return timeSet;
	}

	public long getTime(){
		return time;
	}
	
	public void resetTime(){
		timeSet = false;
	}
	
	public byte getMedal(){
		return medal;
	}

	public String getPlayername(){
		return playerName;
	}
	
	public int getPlayerID(){
		return playerID;
	}

	@Override
	public int compareTo(LeaderboardEntry h) {
		
		if(this.timeSet() == true && h.timeSet == false){
			return -1;
		}
		
		if(this.timeSet() == false && h.timeSet == true){
			return 1;
		}
		
		if(this.timeSet() == false && h.timeSet == false){
			return this.playerName.compareTo(h.getPlayername());
		}
		
		
		if(this.time < h.getTime()){
			return -1;
		}else{
			if(this.time > h.getTime()){
				return 1;
			}else{
				//means that it is equal!
				//if that's the case, sort by name
				return this.playerName.compareTo(h.getPlayername());
			}
		}

	}


}
