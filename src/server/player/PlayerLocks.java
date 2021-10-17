package server.player;

import java.util.concurrent.locks.ReentrantReadWriteLock;



/**
 * Class which implements Locks for ressources which can be accessed from multiple threads
 * 
 * 
 *
 */

public class PlayerLocks {
	
	//Lock for userdata (coordinates,animationdata, etc...)
	private ReentrantReadWriteLock playerDataLock;
	
	//Lock for the playerSkin
	private ReentrantReadWriteLock playerSkinLock;
	
	//Lock for the internetaddress of the player
	private ReentrantReadWriteLock internetAddressLock;
	
	public PlayerLocks(){
		//PlayerData
		playerDataLock = new ReentrantReadWriteLock();
		
		//skin
		playerSkinLock = new ReentrantReadWriteLock();
		
		//udp
		internetAddressLock = new ReentrantReadWriteLock();

		//---------

	}
	

	public ReentrantReadWriteLock getPlayerDataLock(){
		return playerDataLock;
	}
	
	public ReentrantReadWriteLock getInternetAddressLock(){
		return internetAddressLock;
	}

	public ReentrantReadWriteLock getPlayerSkinLock(){
		return playerSkinLock;
	}
	
}
