package server.connection;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.NotYetConnectedException;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import DataStructures.TcpMessage;
import debug.DebugIO;
import server.ServerCore;
import server.Server.TCPDISCONNECTTYPE;
import server.player.Player;
import server.player.PlayerProcessor;
import server.util.BufferConverter;

public class TcpPlayerWriter implements Runnable{


	private boolean run;

	private ServerCore core;

	ConcurrentHashMap<Player,TcpPacketData> lastPacketData;


	//TODO: Make this class per-player (like TCPplayerReader) instead of global to bypass thread blocking issues
	public TcpPlayerWriter(ServerCore core){

		lastPacketData = new ConcurrentHashMap<Player,TcpPacketData>();
		run = true;
		this.core = core;
	}


	//needs to be called if a player is removed!
	public void removePlayer(Player p){
		lastPacketData.remove(p);
	}

	public void registerPlayer(Player p){
		lastPacketData.put(p, new TcpPacketData());
	}

	public void setRunning(boolean bool){
		this.run = bool;
	}

	@Override
	public void run() {
		// TODO Auto-generated method stub

		while(run){


			ConcurrentHashMap<Integer, Player> map = core.getPlayers();
			Iterator<Integer> it = map.keySet().iterator();

			//iterate through the whole playerlist to get the adress and IP information of the player for packet transmission
			while(it.hasNext()){

				Integer key = (Integer) it.next();
				Player player = (Player) map.get(key);


				if(player != null){//safety check

					try {
						sendQueuedTcpMessages(player);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			}


			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block

				e.printStackTrace();
				setRunning(false);
			}
		}

	}



	public void sendQueuedTcpMessages(Player player) throws IOException{

		TcpMessage m = null;

		//CAN BE NULL!
		TcpPacketData playerTcpData = lastPacketData.get(player);

		//check if a previous message was not completely send over to the client. If that's the case, handle this message in this loop iteration
		if(playerTcpData != null){
			if(playerTcpData.getRemainingTcpMessage() != null){
				m = playerTcpData.getRemainingTcpMessage();
			}

			//if the previous message was completely sent over, then take a new message from the queue
			if(m == null){
				m = player.getOutpoutMessageQueue().pollConcurrent();			
			}

			if(m != null){
				ByteBuffer b = m.getBuffer();
				b.position(0);

				//merge
				ByteBuffer message = ByteBuffer.allocate(BufferConverter.TCPHEADERSIZE+b.capacity());//7 = byte count of TCP header
				message.position(0);

				BufferConverter.writePacketHeaderTCP(message, m.getID(), b.capacity());//header
				message.put(b);//message
				message.position(playerTcpData.getLastPosition());

				int total = 0;

				try {
					total = player.getSocketChannel().write(message);					
				} catch (IOException e) {
					//throws error if the player disconnects before this thread was able to write/send a message
					System.out.println("Connection lost TcpPlayerWriter");				
				}finally{
					//player.getLocks().getTcpOutputLock().writeLock().unlock();
				}

				if(playerTcpData.getLastPosition()+total == message.capacity()){//message was completely sent
					playerTcpData.reset();
				}else{
					playerTcpData.setRemainingTcpMessage(m);
					playerTcpData.setLastPosition(playerTcpData.getLastPosition()+total);

					System.out.println("NOT WRITTEN EVERYTHING: "+total);
				}

			}

		}

	}


	public static void sendDirectDisconnectTCPMessage(SocketChannel channel,TCPDISCONNECTTYPE type) throws IOException{
		sendDirectDisconnectTCPMessage(channel,type);
	}
	
	public static void sendDirectDisconnectTCPMessage(SocketChannel channel,TCPDISCONNECTTYPE type,String string) throws IOException{
		byte val = 0;

		switch(type){
		case DEFAULT:
			val = 0;
			break;

		case KICK:
			val = 1;
			break;

		case SERVERFULL:
			val = 2;
			break;

		case SERVERCLOSED:
			val = 3;
			break;

		case INCOMPATIBLESERVERVERSION:
			val = 4;
			break;

		case TCPTIMEOUT:
			val = 5;
			break;

		case UDPTIMEOUT:
			val = 6;
			break;
			
		case WRONGPASSWORD:
			val = 7;
			break;

		case PROCESSINGERROR:
			val = 8;
			break;
			
		case PROTOCOL_VIOLATION:
			val = 9;
			break;

		case ONLINEMODE_REQUIRED:
			val = 10;
			break;
			
		default:
			val = 0;
			break;
		}


		var mSize = 1+2+string.length()*2;
		ByteBuffer message = ByteBuffer.allocate(BufferConverter.TCPHEADERSIZE+mSize);//7 = byte count of TCP header


		BufferConverter.writePacketHeaderTCP(message,PlayerProcessor.TcpServerMessage.SERVER_DISCONNECT,mSize);
		BufferConverter.buffer_write_u8(message, val);
		BufferConverter.buffer_write_u16(message, string.length());
		BufferConverter.buffer_write_chars_u16(message,string);
		
		message.position(0);

		int total = 0;
		try {
			total = channel.write(message);
		} catch (IOException e) {
			System.out.println("throwException sendDirectDisconnectTCPMessage");
		}


	}

	public static void sendDirectDisconnectTCPMessage(Player player,TCPDISCONNECTTYPE type) throws IOException{
		sendDirectDisconnectTCPMessage(player.getSocketChannel(),type);
	}




	class TcpPacketData{
		TcpMessage remainingTcpMessage;

		int lastPosition;

		public TcpPacketData(){
			remainingTcpMessage = null;
			lastPosition = 0;
		}

		public void reset(){
			setLastPosition(0);
			setRemainingTcpMessage(null);
		}

		public int getLastPosition(){
			return lastPosition;
		}

		public void setLastPosition(int pos){
			this.lastPosition = pos;
		}

		public TcpMessage getRemainingTcpMessage(){
			return remainingTcpMessage;
		}

		public void setRemainingTcpMessage(TcpMessage message){
			this.remainingTcpMessage = message;
		}
	}



}
