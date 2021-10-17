package server.player.data;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class PlayerProfileData {
	String playerName;
	private ReentrantReadWriteLock playerNameLock;
	
	private AtomicReference<String> steamID;
	
	private AtomicReference<Integer> experience;
	private AtomicReference<Byte> badgeID;
	
	public PlayerProfileData() {
		playerName = "";
		playerNameLock = new ReentrantReadWriteLock();
		steamID = new AtomicReference<String>("0");
		
		experience = new AtomicReference<Integer>(0);
		badgeID = new AtomicReference<Byte>((byte) 0);
	}
	
	
	public void setPlayerName(String name) {
		playerNameLock.writeLock().lock();
		this.playerName = name;
		playerNameLock.writeLock().unlock();
	}
	
	public String getPlayerName() {
		String ret;
		playerNameLock.writeLock().lock();
		ret = this.playerName;
		playerNameLock.writeLock().unlock();
		
		return ret;
	}
	
	public void setExperience(int experience) {
		this.experience.set(experience);
	}
	
	public int getExperience() {
		return experience.get();
	}
	
	public void setBadgeID(byte badgeID) {
		this.badgeID.set(badgeID);
	}
	
	public byte getBadgeID() {
		return badgeID.get();
	}
	
	public String getSteamID() {
		return new String(steamID.get());
	}
	
	public void setSteamID(String steamID) {
		this.steamID.set(new String(steamID));
	}
}
