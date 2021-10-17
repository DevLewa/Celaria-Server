package server.player;

import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import DataStructures.TcpMessage;
import debug.DebugIO;
import server.ServerCore;
import server.util.BufferConverter;
import server.util.StreamConverter;

/**
 * Class which handles the transfer of the mapdate from the server to the cliet
 * 
 * 
 *
 */

public class MapTransmitter {


	boolean lastpaket = false;
	int mappacket_current_ressource_byte_number = 0;
	
	final int mappacket_chunkSize = 4096;//4KB;
	int mappacket_current_chunkSize = mappacket_chunkSize;


	ServerCore server;
	Player player;
	
	ReentrantReadWriteLock lock;
	
	public MapTransmitter(Player player){
		this.server = player.getServer();
		this.player = player;
		lock = new ReentrantReadWriteLock();
	}



	public void reset(){
		lock.writeLock().lock();
		
		lastpaket = false;
		mappacket_current_ressource_byte_number = 0;
		
		mappacket_current_chunkSize = mappacket_chunkSize;
		
		lock.writeLock().unlock();
	}

	/**
	 * Sends a fragment of the mapdata to the client.
	 * 
	 * TODO: set returntype to BOOLEAN and return TRUE if last packet was sent
	 *
	 * @throws IOException
	 */
	public void sendPacket() throws IOException{




		lock.writeLock().lock();
		
		//send map packet
		if (lastpaket == true){//confirm with packetnumber 5
			player.getPlayerServerData().setMapSended(true);
			DebugIO.println("Last Map Package Send!");
			server.sendTcpMessageLocked(player, PlayerProcessor.TcpServerMessage.LAST_MAP_DATA_PACKET, ByteBuffer.allocate(0));//map finished
		}else{

			server.getRessourceHandler().getMapHandler().getLock().readLock().lock();//LOCK READING!
			byte[] mapArray = server.getRessourceHandler().getMapHandler().getMapByteArray();//copy array
			boolean mapReady = false;
			server.getRessourceHandler().getMapHandler().mapReady();
			server.getRessourceHandler().getMapHandler().getLock().readLock().unlock();//UNLOCK READING!
			

			lock.writeLock().lock();

			if(mappacket_current_ressource_byte_number+mappacket_current_chunkSize > mapArray.length){
				mappacket_current_chunkSize = mapArray.length - mappacket_current_ressource_byte_number;
				lastpaket = true;
			}

			ByteBuffer buffer = ByteBuffer.allocate(mappacket_current_chunkSize+2);


			BufferConverter.buffer_write_u16(buffer,server.getRessourceHandler().getMapHandler().getMapMagicID());

			int t = 0;
			for(int i=0;i<mappacket_current_chunkSize;i++){

				BufferConverter.buffer_write_u8(buffer,mapArray[mappacket_current_ressource_byte_number]);
				mappacket_current_ressource_byte_number+=+1;
				t++;
			}
			DebugIO.println(mappacket_current_chunkSize+"/"+t+ " - " +mappacket_current_ressource_byte_number+"/"+mapArray.length);

			server.sendTcpMessageLocked(player, PlayerProcessor.TcpServerMessage.MAP_DATA_PACKET, buffer);

			lock.writeLock().unlock();
			
		}
				
		lock.writeLock().unlock();

	}


}
