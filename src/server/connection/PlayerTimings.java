package server.connection;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

public class PlayerTimings {
	

	//UDP TIMEOUT

	private AtomicLong lastUdpConnectionTime;//when a udp packet was recieved/send. If the time difference between system.nanotime and this variable is greater than the set UDP timeout time, then the client will be closed

	private AtomicLong lastTcpConnectionTime;//last time a TCP packet was recieved
	
	private AtomicInteger lastUdpPingTime;//last time a ping packet was sent (if the time runs out, a new ping will be sent) (Should be a short datatype)
	
	public PlayerTimings(){

		lastUdpConnectionTime = new AtomicLong(System.nanoTime());
		lastTcpConnectionTime = new AtomicLong(System.nanoTime());
		lastUdpPingTime = new AtomicInteger(0);

	}

	
	
	public short getLastPingTime(){
		return (short) lastUdpPingTime.get();
	}

	public void setLastUdpPingTime(short time){
		lastUdpPingTime.set(time);
	}
	

	public void resetTcpTimeoutCounter(){
		lastTcpConnectionTime.set(System.nanoTime());
	}
	
	public long getLastTcpTime(){
		return lastTcpConnectionTime.get();
	}

	
	public long getLastUdpTime(){
		return lastUdpConnectionTime.get();
	}

	public void resetUdpTimeoutCounter(){
		lastUdpConnectionTime.set(System.nanoTime());
	}
	


	
}
