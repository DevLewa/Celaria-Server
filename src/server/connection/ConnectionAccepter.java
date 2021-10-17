package server.connection;

import java.io.IOException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import java.nio.channels.SocketChannel;

import java.util.Iterator;

import server.Server.TCPDISCONNECTTYPE;
import server.ServerCore;
import server.player.Player;



/**
 * This class accepts new Connections from Players.
 * Creates new Player objects and starts their respective threads
 * 
 * 
 */

public class ConnectionAccepter implements Runnable {

	private ServerCore server;
	private boolean run = true;//Define the boolean run to keep the server running
	Selector selector;


	public ConnectionAccepter(ServerCore s){
		server = s;

		try {
			selector = Selector.open();

			s.getServerSocketChannel().register(selector,
					SelectionKey.OP_ACCEPT);


		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while(run){//Start a do while loop while run is true

			try {
				selector.select();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				//e.printStackTrace();

			}

			/**
			 * If we are here, it is because an operation happened (or the TIMEOUT expired).
			 * We need to get the SelectionKeys from the selector to see what operations are available.
			 * We use an iterator for this. 
			 */


			Iterator<SelectionKey> keys = null;
			try{
				keys = selector.selectedKeys().iterator();
			}catch(ClosedSelectorException e){

			}

			if(keys != null){
				while (keys.hasNext()){
					SelectionKey key = keys.next();
					// remove the key so that we don't process this OPERATION again.
					keys.remove();

					// key could be invalid if for example, the client closed the connection.
					if (!key.isValid()){
						continue;
					}


					if (key.isAcceptable()){

						// accept connection 
						SocketChannel client;

						try {
							client = server.getServerSocketChannel().accept();


							
							client.socket().setTcpNoDelay(true); 


							if(server.getPlayers().size() < server.getMaxPlayerCount()){
								//server is not full. > player is officially added to the game
								client.configureBlocking(false); 
								int newID = server.getNewPlayerID();//Select an unique name for this player

								Player p = new Player(server,newID, client);//Create a new Player instance and set the socket to it

								server.consolePrintln("New client connected.");//Display the message that we got a new player

								server.getPlayers().put(newID, p);//Add the player instance to the player HashMap
							}else{
								//server is full. Send a TCP message back to the client ("server is full message") and disconnect/close the socket.
		
								client.configureBlocking(true); 
								TcpDisconnectMessage disconnectMessage = new TcpDisconnectMessage(client,TCPDISCONNECTTYPE.SERVERFULL,"");

								server.getTcpServerFullPool().execute(disconnectMessage);

							}

						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}

					}

				}

			}
		}
	}


	public void stop() throws IOException{
		run = false;
		selector.close();
	}
	public boolean getRunning(){
		return this.run;
	}


}
