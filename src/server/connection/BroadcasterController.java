package server.connection;

import java.net.DatagramSocket;

import server.ServerCore;

/**
 * This class is responsible for Broadcasting Data to all Players simultaneously.
 * This includes: Server Commands, Player Data, Chat Messages, etc...
 * 
 * 
 *
 */
public class BroadcasterController {
	
	private BroadcasterUDP broadcasterUDP;
	private Thread broadcasterUDP_thread;
	
	private BroadcasterTCP broadcasterTCP;
	private Thread broadcasterTCP_thread;
	
	public BroadcasterController(ServerCore server, DatagramSocket udpSocket){
		broadcasterUDP = new BroadcasterUDP(server,udpSocket);
		broadcasterUDP_thread = new Thread(broadcasterUDP,"UDP Broadcaster");
		broadcasterUDP_thread.start();
		
		broadcasterTCP = new BroadcasterTCP(server);
		broadcasterTCP_thread = new Thread(broadcasterTCP,"TCP Broadcaster");
		broadcasterTCP_thread.start();
		
	}
	
	public BroadcasterTCP getTcpBroadcaster(){
		return broadcasterTCP;
	}
	
	public void stop(){
		broadcasterUDP.stop();
		broadcasterTCP.stop();
	}
	
}
