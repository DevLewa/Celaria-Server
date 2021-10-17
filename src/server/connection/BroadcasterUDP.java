package server.connection;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.util.Iterator;
import java.util.concurrent.ConcurrentHashMap;

import debug.DebugIO;
import server.ServerCore;
import server.player.Player;
import server.player.data.PlayerRuntimeUdpData;
import server.util.StreamConverter;

/**
 * This class is responsible for Broadcasting Data to all Players simultaneously.
 * This includes: Server Commands, Player Data, etc...
 * 
 * 
 *
 */

public class BroadcasterUDP implements Runnable{

	
	public class UdpServerMessage {
		public static final int PLAYER_DATA = 1;
		public static final int PINGTEST = 210;
		public static final int UDP_TESTPACKET = 10;
	};
	
	static float lerp(float a, float b, float f)
	{
		return a + f * (b - a);
	}


	public class CompressedFloat {
		//for compressing 32 bit floats into 24 bit integers
		public CompressedFloat(){
			b = 0;
			ext = 0;
		}
		public int b;//byte
		public int ext; //short


		public void compressFloat(float f) {
			f = f*10.0f;
			float divide = (((f))/65535.0f);
			int bytes = (int)Math.floor(divide);//*(int)Math.signum(divide);

			int diff = (int)f-bytes*65535;
			bytes+=128;//bring into the 0-255 range	
			this.b = bytes;
			this.ext = diff;
		}

	}


	boolean run = true;

	ServerCore server;
	DatagramSocket dsocket;


	private long udpStillAliveTimer = System.currentTimeMillis();
	private final long udpStillAliveTimePeriod = 5L*1000L;//5 seconds (interval in which the game repeatedly sends stillAlive UDP packets.)

	private long threadSleepTime = 1000/ServerCore.TICKS_PER_SECOND;//how long the thread sleeps .

	private boolean sendPing = true;


	public final int BUNDLE_PLAYERDATA_COUNT = 32;//max  playerdata packets are bundled together
	public BroadcasterUDP(ServerCore server,DatagramSocket socket){
		this.server = server;
		dsocket = socket;

	}

	public void stop(){
		this.run = false;
	}

	@Override
	public void run() {

		CompressedFloat c = new CompressedFloat();

		while (run){
			DebugIO.printRunning("Broadcaster UDP ");

			ConcurrentHashMap<Integer, Player> map = server.getPlayers();

			//TODO: modify ticksPerSecond depending on the number of players!
			float interpol = Math.min((float)map.size()/64.0f,1.0f);//Math.max((float)map.size() - 64.0f,0.0f)/64.0f;
			//value starts going towards 1 after 32 players
			threadSleepTime = (long) lerp(1000f/10,1000f/5.0f,interpol);


			Iterator<Integer> it = map.keySet().iterator();
			sendPing = false;
			
			if(udpStillAliveTimer+udpStillAliveTimePeriod <= System.currentTimeMillis()){
				udpStillAliveTimer = System.currentTimeMillis();
				sendPing = true;
			}

			//iterate through the whole playerlist to get the adress and IP information of the player for packet transmission
			while(it.hasNext()){

				Integer key = (Integer) it.next();
				Player player = (Player) map.get(key);


				if(player != null){//safety check

					InetAddress address = player.getInternetAddress();
					int udpPort = player.getUdpPort();
					if(address != null && udpPort != -1){

						//Iterate through the list again
						Iterator<Integer> playerListIterator = map.keySet().iterator();

						//boolean packet_sent = false;

						sendTestPacket(player,address,udpPort);//packet is only sent if UDP connection wasn't verified


						int packetNumber = 1;


						boolean headerInitialised = false;

						//packet
						ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
						DataOutputStream output = new DataOutputStream(byteOut);


						if(map.size() >= 2){//send only UDP position refresh packets if there are at least 2 players online

							boolean firstElement = true;
							//Now we want to send this player the DATA information (xyz,rotation, etc...) from the other players.
							while(playerListIterator.hasNext()){
								//player information which will be transfered via UDP
								Integer playerKey = (Integer) playerListIterator.next();
								Player requestedPlayer = (Player) map.get(playerKey);

								if(requestedPlayer != null && playerKey.equals(key) == false){//safety check AND if the source player isn't the same as the destination player
									//> it would be pointless to send the information of player A to player A.

									boolean requestedPlayerIsReady = requestedPlayer.getPlayerServerData().getPlayerReady();
									PlayerRuntimeUdpData requestedPlayerData = requestedPlayer.getPlayerDataCopy();//get Copy of the playerdata (to avoid thread locking)
									
									//if the server recieved the first UDP testpacket
									if(player.getPlayerServerData().getUdpTestPacketRecieved() == true){//not nessecary to test tbh.
										if(player.getPlayerServerData().getUdpConnectionEstablished() == true && player.getPlayerServerData().getPlayerReady() == true)
										{
											if(requestedPlayerIsReady) {
												try {
													//send this playerpacket to the other players

													if(headerInitialised == false){
														StreamConverter.writePacketHeaderUDP(output, UdpServerMessage.PLAYER_DATA);
														headerInitialised = true;
													}

													if(firstElement == true){
														firstElement = false;
													}else{
														StreamConverter.buffer_write_u8(output,1);//additional player information is attached to this packet
													}

													//segment
													StreamConverter.buffer_write_u8(output, playerKey.intValue());//ID of the player
													StreamConverter.buffer_write_u8(output, requestedPlayerData.getUpdateNumber());
													StreamConverter.buffer_write_u8(output, requestedPlayerData.getRespawnNumber());



													c.compressFloat((float)requestedPlayerData.getX());
													StreamConverter.buffer_write_u8(output,c.b);
													StreamConverter.buffer_write_u16(output,c.ext);

													c.compressFloat((float)requestedPlayerData.getY());
													StreamConverter.buffer_write_u8(output,c.b);
													StreamConverter.buffer_write_u16(output,c.ext);

													c.compressFloat((float)requestedPlayerData.getZ());
													StreamConverter.buffer_write_u8(output,c.b);
													StreamConverter.buffer_write_u16(output,c.ext);


													//NOTE: This SHOULD to be changed to 16 bit floats (buffer_f16) but GM doesn't support this so instead a somewhat hacky solution was used to send
													//this data to the client with as little bytes as possible. (alternative would be to just use 32bit floats but this would unnecessarily bloat the packet size
													//and increase memory bandwidth
													
													//normalize movement vector
													float len = (float) Math.sqrt(requestedPlayerData.getMovX()*requestedPlayerData.getMovX() + requestedPlayerData.getMovY() * requestedPlayerData.getMovY() + requestedPlayerData.getMovZ() * requestedPlayerData.getMovZ());
													float mX,mY,mZ;
													if(len > 0.001) {
														//normalize
														mX = requestedPlayerData.getMovX()/len;
														mY = requestedPlayerData.getMovY()/len;
														mZ = requestedPlayerData.getMovZ()/len;
													}else {
														mX = 0;
														mY = 0;
														mZ = 0;
													}

													//write normal
													StreamConverter.buffer_write_u8(output, (int)(((mX+1.0f)/2.0f) * 255f));
													StreamConverter.buffer_write_u8(output, (int)(((mY+1.0f)/2.0f) * 255f));
													StreamConverter.buffer_write_u8(output, (int)(((mZ+1.0f)/2.0f) * 255f));
													//write vector length
													StreamConverter.buffer_write_u8(output,(int)(((len/6.0f)*255f)));//6.0 is hardcoded in the client.
													StreamConverter.buffer_write_u8(output, (int)((requestedPlayerData.getRotationZ()/360f)*255f));
													StreamConverter.buffer_write_u8(output, requestedPlayerData.getAnimationID());
													StreamConverter.buffer_write_u8(output, (int)(requestedPlayerData.getAnimationStep()*255f));


													if(packetNumber < BUNDLE_PLAYERDATA_COUNT){
														packetNumber++;
													}else{

														StreamConverter.buffer_write_u8(output,0);//no additional information is available (client stops reading this packet)

														//send packet
														output.flush();


														byte[] bytes = byteOut.toByteArray();
														DatagramPacket packet = new DatagramPacket(bytes,bytes.length,address,udpPort);//send packet to source

														dsocket.send(packet);									
														byteOut.reset();
														//---------------------------------

														packetNumber = 0;
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


							//if something was left over, then send it again
							if(packetNumber>0 && headerInitialised == true){
								try {

									StreamConverter.buffer_write_u8(output,0);//no additional information is available (client stops reading this packet)
									output.flush();
									byte[] bytes = byteOut.toByteArray();
									DatagramPacket packet = new DatagramPacket(bytes,bytes.length,address,udpPort);//send packet to source

									dsocket.send(packet);
									

									byteOut.reset();

								} catch (IOException e) {
									// TODO Auto-generated catch block
									e.printStackTrace();
								}

							}
						}


						//Also works as a pingtest
						if(sendPing == true){
							//byteOut.reset();

							try {
								StreamConverter.writePacketHeaderUDP(output, UdpServerMessage.PINGTEST);
								StreamConverter.buffer_write_u8(output, player.getNetworkCounter().getUdpStillAlivePacketNumber());//send packet with a packetnumber
								StreamConverter.buffer_write_u16(output, player.getTimings().getLastPingTime());//send last known pingtime so that the client can look it up

								//output.flush();

								player.getNetworkCounter().setPingTime(player.getNetworkCounter().getUdpStillAlivePacketNumber(), System.currentTimeMillis());//write down to what time this packet with this number was sent
								player.getNetworkCounter().increaseUdpStillAlivePacketNumber();//increase the packetnumber for the next request.

								//send
								output.flush();
								byte[] bytes = byteOut.toByteArray();
								DatagramPacket packet = new DatagramPacket(bytes,bytes.length,address,udpPort);//send packet to source
								dsocket.send(packet);



							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}


						}


					}
				}
			}


			try {
			
				Thread.sleep(threadSleepTime);//1000 millis = 1 second 
				
			}
			catch(InterruptedException e) {
			}
		}

		dsocket.close();
	}



	public void sendTestPacket(Player player,InetAddress address, int udpPort){
		if(player.getPlayerServerData().getUdpTestPacketRecieved() == true){
			if(player.getPlayerServerData().getUdpConnectionEstablished() == false){//if connection wasn't established
				//send a testpacket from server to client. (as the connection from client to server works.
				//now we need to test it the other way around.)
				try {
					ByteArrayOutputStream byteOut = new ByteArrayOutputStream();
					DataOutputStream output = new DataOutputStream(byteOut);
					StreamConverter.writePacketHeaderUDP(output, UdpServerMessage.UDP_TESTPACKET);
					output.flush();

					byte[] bytes = byteOut.toByteArray();
					DatagramPacket packet = new DatagramPacket(bytes,bytes.length,address,udpPort);//send packet to source


					dsocket.send(packet);

				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}	

			}
		}
	}



}
