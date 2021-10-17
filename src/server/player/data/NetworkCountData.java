package server.player.data;


import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class NetworkCountData {

	private ReentrantReadWriteLock udpStillAlivePacketNumberLock;
	private int udpStillAlivePacketNumber = 0;

	private AtomicLong[] udpPingTimeTable = new AtomicLong[256];

	public NetworkCountData(){
		udpStillAlivePacketNumberLock = new ReentrantReadWriteLock();
		for(int i = 0;i<udpPingTimeTable.length;i++) {
			udpPingTimeTable[i] = new AtomicLong(0);
		}
	}

	public int getUdpStillAlivePacketNumber(){
		udpStillAlivePacketNumberLock.readLock().lock();
		try {
			return udpStillAlivePacketNumber;
		}finally {
			udpStillAlivePacketNumberLock.readLock().unlock();
		}
	}
	

	public long getPingTime(int packetNumber){
		return udpPingTimeTable[packetNumber].get();
	}

	public void setPingTime(int packetNumber,long time){
		udpPingTimeTable[packetNumber].set(time);
	}

	public void increaseUdpStillAlivePacketNumber(){
		udpStillAlivePacketNumberLock.writeLock().lock();
		udpStillAlivePacketNumber++;
		if(udpStillAlivePacketNumber>=255){
			udpStillAlivePacketNumber = 0;
		}
		udpStillAlivePacketNumberLock.writeLock().unlock();
	}

}
