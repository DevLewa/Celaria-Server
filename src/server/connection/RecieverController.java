package server.connection;

import java.io.IOException;

import server.ServerCore;

/**
 * Class which stores the ConnectionAccepter as well as the UdpReciever.
 * Primarely made to streamline the starting/shutting down process of the server,
 * 
 * 
 *
 */

public class RecieverController {
	
	ServerCore server;
	
	private ConnectionAccepter accepter;
	private Thread accepter_thread;
	
	private UdpReciever udp_reciever;
	private Thread udp_reciever_thread;

	
	public RecieverController(ServerCore server){
		
		this.server = server;
		
		accepter = new ConnectionAccepter(server);//Connects new players via TCP
		accepter_thread = new Thread(accepter,"ConnectionAccepter");
		accepter_thread.start();
		
		udp_reciever = new UdpReciever(server);//checks new UDP packets (player coordinates,etc...)			
		udp_reciever_thread = new Thread(udp_reciever,"UDP reciever");
		udp_reciever_thread.start();
	}
	
	
	public void stop(){
		stopAccepterTCP();
		stopRecieverUDP();
	}

	public void stopAccepterTCP(){
		try {
			accepter.stop();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		accepter = null;
		accepter_thread = null;
		
		
	}
	
	public void stopRecieverUDP(){
		udp_reciever.stop();
		
		udp_reciever = null;
		udp_reciever_thread = null;
	}
}
