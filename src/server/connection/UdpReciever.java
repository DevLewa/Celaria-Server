package server.connection;


import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.Socket;

import debug.DebugIO;
import server.ServerCore;
import server.player.Player;
import server.player.data.PlayerRuntimeUdpData;
import server.util.StreamConverter;


/**
 * Class which recieves UDP packes from the game clients.
 * The sender of ths message can be identified by the playerID which is included in each UDP packet.
 * 
 * 
 *
 */

public class UdpReciever implements Runnable{

	private ServerCore server;
	boolean running;



	public UdpReciever(ServerCore s){
		server = s;
		running = true;

	}


	@Override
	public void run() {
		while(running){//Start a do while loop while run is true
			DebugIO.printRunning("UDP Reciever");

			checkForMessagesUDP();
			
			
			try {
				Thread.sleep(10);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public void checkForMessagesUDP(){		
		try {
			DatagramPacket packet = new DatagramPacket( new byte[1024], 1024 );

			server.getServerSocketUDP().receive( packet );

			/*InetAddress address = packet.getAddress();
		      int         port    = packet.getPort();
		      int         len     = packet.getLength();
		      byte[]      data    = packet.getData();*/
			byte[] data = packet.getData();


			ByteArrayInputStream byteOut = new ByteArrayInputStream(data);
			DataInputStream input = new DataInputStream(byteOut);

			int packetID = -1;
			int player_id = -1;
			int player_udpKey = -1;

			boolean isValidPacket = false;
			//check header
			int c1 = StreamConverter.buffer_read_u8(input);//C
			int c2 = StreamConverter.buffer_read_u8(input);//C
			int c3 = StreamConverter.buffer_read_u8(input);//P
			int c4 = StreamConverter.buffer_read_u8(input);//U

			if(c1 == 67 && c2 == 67 && c3 == 80 && c4 == 85){

				player_id = StreamConverter.buffer_read_u8(input);
				packetID = StreamConverter.buffer_read_u8(input);
				player_udpKey = StreamConverter.buffer_read_u16(input);

				isValidPacket = true;

			}else{

				DebugIO.println("WRONG HEADER UDP! "+c1+"/"+c2+"/"+c3+"/"+c4);

			}



			boolean packetFromValidSource = false;

			if(isValidPacket){



				packet.getAddress();
				packet.getPort();

				//TODO: change hashmap for own implementation for possible performanceimprovements during reads!
				if(server.getPlayers().containsKey(new Integer(player_id))){//check if the player_id exists in the database

					//update the udp internetadress and port if they don't match

					Player p = server.getPlayers().get(new Integer(player_id));
					//check if player has set InternetAddress
					if(p.getInternetAddress() != null){
						//if the player has an InternetAddress
						if(player_udpKey == p.getUdpKey()){//check the keys
							packetFromValidSource = true;
						}


					}else{
						//if the player doesn't have an internetAddress
						if(player_udpKey == p.getUdpKey()){//check keys
							p.setInternetAddress(packet.getAddress());//set the adress of the new player
							packetFromValidSource = true;
						}else{
							DebugIO.println("WRONG KEY: "+player_udpKey+"/"+p.getUdpKey());
						}
					}


					if(packetFromValidSource == true){//Proceed only if the packet was validated from the right player!

						if(p.getUdpPort() != packet.getPort()){
							p.setUdpPort(packet.getPort());
						}
						//------------------------------------------

						switch(packetID){
						case 1:
							int updateNumber = StreamConverter.buffer_read_u8(input);
							int respawnNumber = StreamConverter.buffer_read_u8(input);


							double x = StreamConverter.buffer_read_f32(input);
							double y = StreamConverter.buffer_read_f32(input);
							double z = StreamConverter.buffer_read_f32(input);

							float nx =  ((((float)(StreamConverter.buffer_read_u8(input)))/255.0f)*2.0f)-1.0f;
							float ny =  ((((float)(StreamConverter.buffer_read_u8(input)))/255.0f)*2.0f)-1.0f;
							float nz =  ((((float)(StreamConverter.buffer_read_u8(input)))/255.0f)*2.0f)-1.0f;
							
							float nLen =  (((float)(StreamConverter.buffer_read_u8(input)))/255.0f)*3.0f;
							
							float movX = nx*nLen;
							float movY = ny*nLen;
							float movZ = nz*nLen;

							float rotationZ = (StreamConverter.buffer_read_u8(input)/255f)*360f;

							int animationID = StreamConverter.buffer_read_u8(input);


							float animationStep =(float)(StreamConverter.buffer_read_u8(input))/255f;// /65535f
							
							PlayerRuntimeUdpData playerData = p.getPlayerDataCopy();
							
							int lastUpdateNumber = playerData.getUpdateNumber();
							
							boolean applyRegardless = false;
							if((updateNumber>>7) != (lastUpdateNumber>>7)) {
								applyRegardless = true;
							}
							
							if(lastUpdateNumber<updateNumber || applyRegardless) {


								playerData.setRespawnNumber(respawnNumber);
								playerData.setX(x);
								playerData.setY(y);
								playerData.setZ(z);
								playerData.setMovX(movX);
								playerData.setMovY(movY);
								playerData.setMovZ(movZ);
								playerData.setRotationZ(rotationZ);
								playerData.setAnimationID(animationID);
								playerData.setAnimationStep(animationStep);
								playerData.setNewUpdateNumber(updateNumber);
								
								p.applyPlayerData(playerData);
																
							}else{

								DebugIO.println("WRONG UDP update packet");
							}


							break;


						case 10:
							//if testpacket recieved, set the variable to true.
							//now the server hast to send a testpacket back (happens in the UDPbroadcaster)
							p.getPlayerServerData().setUdpTestPacketRecieved(true);
							DebugIO.println("TESTPACKET RECIEVED");
							break;

						case 210:
							
							long currentTime = System.currentTimeMillis();
							
							int pingPacketID = StreamConverter.buffer_read_u8(input);
							long lastTime = p.getNetworkCounter().getPingTime(pingPacketID);


							long diff = (currentTime-lastTime);
							if(diff < 0){diff = 0;}
							diff = Math.min(diff, 999);
							p.getTimings().setLastUdpPingTime((short)diff);
							
							break;

						}

						//RESET UDP TIMEOUT
						p.getTimings().resetUdpTimeoutCounter();
					}

				}
			}

		} catch (IOException e) {
			// TODO Auto-generated catch block
			DebugIO.errPrintln("UDP Reciever closed");
			stop();

		}
	}

	public void stop(){
		running = false;
	}

}
