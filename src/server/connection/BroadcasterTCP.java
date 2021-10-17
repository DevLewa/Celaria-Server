package server.connection;


import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


import DataStructures.TcpMessage;
import debug.DebugIO;

import server.ServerCore;
import server.player.Player;



/**
 * This class is responsible for Broadcasting TCP messages to all players.
 * TCP messages are stored in a Queue which can be accessed by other classes/threads.
 * The broadcaster then reads those messages from the Queue und Broadcasts them to all players.
 * 
 * 
 *
 */

public class BroadcasterTCP implements Runnable{

	ServerCore server;
	ConcurrentLinkedQueue<TcpMessage> bufferQueue;

	private boolean run;
	
	ExecutorService tcpWorkers;

	public BroadcasterTCP(ServerCore server){
		run = true;
		this.server = server;
		bufferQueue = new ConcurrentLinkedQueue<TcpMessage>();
		
		tcpWorkers = Executors.newFixedThreadPool(server.getMaxPlayerCount());//thread pool which sends those messages to the specific clients
		//not the most ideal solution performancewise (thread starvation.) Async IO could be a potential optimization path
	}


	@Override
	public void run() {

		while(run){
			DebugIO.printRunning("Broadcaster TCP");
			//send a still-alive message to the client

			//if a buffer waits for the TCP broadcast...
			if(bufferQueue.size()>0){
				TcpMessage message = bufferQueue.poll();//take the buffer element out of the list
				//this buffer needs to be sent as a broadcast to all players via TCP!

				//reads player list with iterator
				ConcurrentHashMap<Integer, Player> map = server.getPlayers();
				Iterator<Integer> it = map.keySet().iterator();

				//loop trough players
				while(it.hasNext()){

					//reads player out of the map
					Integer key = it.next();
					Player player = map.get(key);

					if(player != null){//safety check
						server.sendTcpMessageLocked(player,message.getID(),message.getBuffer());
					}
				}
			}

			try {
				Thread.sleep(1000/ServerCore.TICKS_PER_SECOND);//10
			}
			catch(InterruptedException e) {
			}

		}
	}

	/**
	 * Adds a buffer which will be sent to all players through their individuall TCP connections.
	 * 
	 * @param stream
	 */
	public void addTcpBroadcastMessage(TcpMessage message){
		bufferQueue.offer(message);
	}

	public void stop(){
		run = false;
	}

}
