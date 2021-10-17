package server.connection;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.SocketTimeoutException;
import java.nio.ByteBuffer;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import DataStructures.TcpMessage;
import DataStructures.ThreadedQueue;
import debug.DebugIO;
import server.ServerCore;
import server.Server.TCPDISCONNECTTYPE;
import server.leaderboard.LeaderboardEntry;
import server.player.Player;
import server.util.BufferConverter;
import server.util.StreamConverter;

public class TcpPlayerReader implements Runnable{
	
	private class RegisterEntry{
		private SocketChannel socket;
		private Player player;
		
		public RegisterEntry(SocketChannel socket,Player player) {
			this.socket = socket;
			this.player = player;
		}
		
		SocketChannel getSocket() {
			return socket;
		}
		
		Player getPlayer() {
			return player;
		}
	}
	

	private boolean alive = true;//Define a boolean to keep the player class running

	ServerCore core;


	Selector selector;


	ConcurrentHashMap<Player,TcpBufferData> playerBufferData;
	ConcurrentHashMap<Player,SelectionKey> playerSelectionKey;

	Queue<RegisterEntry> registerChannel;


	public TcpPlayerReader(ServerCore core) throws IOException{
		this.core = core;


		registerChannel = new ConcurrentLinkedQueue<RegisterEntry>();

		playerBufferData = new ConcurrentHashMap<Player,TcpBufferData>();
		
		playerSelectionKey = new ConcurrentHashMap<Player,SelectionKey>();

		selector = Selector.open();




	}

	//needs to be called if a player is removed!
	public void removePlayer(Player p){
		playerBufferData.remove(p);
		
		SelectionKey key = playerSelectionKey.get(p);
		if(key != null) {
			playerSelectionKey.remove(p);
			key.cancel();
		}
	}

	public void registerPlayer(Player p){
		playerBufferData.put(p, new TcpBufferData());
		registerChannel.add(new RegisterEntry(p.getSocketChannel(),p));

		selector.wakeup();



	}

	@Override
	public void run() {

		while(alive){//Start a do while loop while alive is true
			RegisterEntry r = registerChannel.poll();
			
			if(r != null){
				try {
					SocketChannel c = r.getSocket();
	
					SelectionKey k = c.register(selector, SelectionKey.OP_READ);
					playerSelectionKey.put(r.getPlayer(),k);
				} catch (ClosedChannelException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}



			try {
				selector.select();
			} catch (IOException e2) {
				// TODO Auto-generated catch block
				e2.printStackTrace();
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

					SocketChannel channel = (SocketChannel) key.channel();
					Player player = core.getSocketChannelToPlayerMap().get(channel);

					try{
						if(key.isValid()){
							if(key.isReadable()){

								if(player != null){

									try {
										processMessage(channel,player);
									} catch (IOException e) {
										// TODO Auto-generated catch block
										player.closePlayer(TCPDISCONNECTTYPE.PROCESSINGERROR);

									}


								}

							}
						}

					}catch (CancelledKeyException e) {
						if(player != null){
							
							player.closePlayer(TCPDISCONNECTTYPE.PROCESSINGERROR);

						}
					}

				}

			}
		}
	}


	private void processMessage(SocketChannel channel,Player player) throws IOException {

		ByteBuffer readBuffer = ByteBuffer.allocate(2048);

		readBuffer.clear();

		int read = channel.read(readBuffer);


		if(read > 0){

			TcpBufferData data = playerBufferData.get(player);

			int offset = data.byteCount();

			//copy the readBuffer into the databuffer at the according position
			data.getStorageBuffer().position(offset);
			for(int i = 0;i<read;i++){
				data.getStorageBuffer().put(readBuffer.get(i));
			}

			data.addByteCount(read);

			//now check if a packet is valid
			boolean sent = true;
			while(sent == true && data.byteCount()>=BufferConverter.TCPHEADERSIZE){
				try {
					sent = checkForMessages(data,player);
				} catch (TcpHeaderException e) {
					// TODO Auto-generated catch block
					player.closePlayer(TCPDISCONNECTTYPE.PROCESSINGERROR);
					System.out.println("CLOSING 2");
					break;
				}

			}
			player.getTimings().resetTcpTimeoutCounter();
		}
	}

	private boolean checkForMessages(TcpBufferData data,Player player) throws IOException, TcpHeaderException{

		boolean messageRecieved = false;


		ByteBuffer input = data.getStorageBuffer();
		input.position(0);//reset position to 0.

		int packetID = -1;
		int packetSize = -1;
		boolean isValidPacket = false;


		int c1=BufferConverter.buffer_read_u8(input);
		int c2=BufferConverter.buffer_read_u8(input);
		int c3=BufferConverter.buffer_read_u8(input);
		int c4=BufferConverter.buffer_read_u8(input);

		if(c1 == 67 && c2 == 67 && c3 == 80 && c4 == 84){
			packetID = BufferConverter.buffer_read_u8(input);
			packetSize = BufferConverter.buffer_read_u16(input);

			isValidPacket = true;
		}else{
			DebugIO.errPrintln("WRONG HEADER TCP! : "+c1+"/"+c2+"/"+c3+"/"+c4);

			//close player
			throw new TcpHeaderException();
		}



		if (isValidPacket == true) {// Header start

			if(data.byteCount()-input.position()>= packetSize){

				//read bytes
				ByteBuffer b = ByteBuffer.allocate(packetSize);
				b.clear();

				for(int i = 0; i < packetSize;i++){
					b.put((byte)input.get());	
				}

				//shift byteBuffer
				int currentPos = input.position();
				//input.compact();
				removeBytesFromStart(input,currentPos);
				data.removeByteCount(currentPos);

				//send message

				TcpMessage m = new TcpMessage(packetID,b);

				ThreadedQueue<TcpMessage> queue = player.getInputMessageQueue();

				queue.offerConcurrent(m);

				messageRecieved = true;
			}

		}


		return messageRecieved;
	}

	public void removeBytesFromStart(ByteBuffer bf, int n) {

		int index = 0;
		for(int i = bf.position(); i < bf.capacity(); i++) {
			bf.put(index++, bf.get(i));
			bf.put(i, (byte)0); 
		}

	}

	public void close() throws IOException{
		this.alive = false;
		selector.close();
	}


	class TcpHeaderException extends Exception{
		public TcpHeaderException(){
			super();
		}

	}

}
