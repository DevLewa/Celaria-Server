package server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import server.util.BufferConverter;

public class GameMap {
	ByteBuffer map;//bytearray of the map
	volatile public int medalTime_Bronze;
	volatile public int medalTime_Silver;
	volatile public int medalTime_Gold;
	volatile public int medalTime_Platin;

	volatile int mapFileMagicID;
	volatile boolean mapReady;//if ressources are ready. Server only accepts players if this is set to true.

	private ReentrantReadWriteLock mapLock;

	public GameMap(){
		mapReady = false;
		map = null;
		mapFileMagicID = 0;

		medalTime_Platin = 0;
		medalTime_Gold = 0;
		medalTime_Silver = 0;
		medalTime_Bronze = 0;

		mapLock = new ReentrantReadWriteLock();
	}

	/**
	 * Sets the bytearray as the new map and returns true if the map is valid.
	 * False is returned if the map is invalid (missing bytes, wrong mapformat, unknown entity IDs, etc...)
	 * 
	 * @param mapData
	 * @return
	 * @throws IOException 
	 */
	public boolean setMap(ByteBuffer mapData) throws IOException{
		mapLock.writeLock().lock();

		mapReady = false;

		boolean valid = false;

		if(isMapFormatValid(mapData)){
			this.map = mapData;

			refreshMedalTimes();

			incMapMagicID();
			mapReady = true;

			valid = true;
		}else{
			this.map = null;
		}

		mapLock.writeLock().unlock();

		return valid;

	}


	public byte getMedalIDfromTime(long time){


		
		if(time<=medalTime_Platin){
			return 4;//platin erreicht
		}else{
			if(time<=medalTime_Gold){
				//gold erreicht
				return 3;
			}else{
				if(time<=medalTime_Silver){
					//silber
					return 2;
				}else{
					if(time<=medalTime_Bronze){
						//bronze
						return 1;
					}else{
						//no medal
						return 0;
					}
				}
			}
		}
	}

	private void refreshMedalTimes() throws IOException{

		map.position(0);
		BufferConverter.skipBytes(map, 11);
		int version = BufferConverter.buffer_read_u8(map);

		int nLength;
		nLength = BufferConverter.buffer_read_u8(map);

		BufferConverter.skipBytes(map, nLength);

		BufferConverter.skipBytes(map, 1);//gamemode
		
		if(version == 0){
			BufferConverter.skipBytes(map, 1);//floortype
		}

		int timeCount = BufferConverter.buffer_read_u8(map);

		int timeSkip = 4*(timeCount-1);
		BufferConverter.skipBytes(map,timeSkip);
		medalTime_Platin = BufferConverter.buffer_read_u32(map);
		BufferConverter.skipBytes(map,timeSkip);
		medalTime_Gold = BufferConverter.buffer_read_u32(map);
		BufferConverter.skipBytes(map,timeSkip);
		medalTime_Silver = BufferConverter.buffer_read_u32(map);
		BufferConverter.skipBytes(map,timeSkip);
		medalTime_Bronze = BufferConverter.buffer_read_u32(map);



	}

	/**
	 * checks if map (loaded into a buffer) is valid
	 * @param mapData
	 * @return
	 */
	private boolean isMapFormatValid(ByteBuffer mapData){
		mapData.position(0);

		String id = BufferConverter.buffer_read_chars_u8(mapData, 11);
		if(id.equals("celaria_map")){
			int version = BufferConverter.buffer_read_u8(mapData);

			if(!(version == 0 || version == 1 || version == 2)){
				return false;
			}



			int nameLength = BufferConverter.buffer_read_u8(mapData);
			BufferConverter.skipBytes(mapData, nameLength);

			if(version == 0){
				BufferConverter.skipBytes(mapData, 1);
			}

			int mode =  BufferConverter.buffer_read_u8(mapData);
			if(mode == 1){
				int times = BufferConverter.buffer_read_u8(mapData);
				BufferConverter.skipBytes(mapData, (times*4));
				BufferConverter.skipBytes(mapData, (times*4));
				BufferConverter.skipBytes(mapData, (times*4));
				BufferConverter.skipBytes(mapData, (times*4));

				BufferConverter.skipBytes(mapData, 4);
				BufferConverter.skipBytes(mapData, 4);

				BufferConverter.skipBytes(mapData, 8);
				BufferConverter.skipBytes(mapData, 8);
				BufferConverter.skipBytes(mapData, 8);

				BufferConverter.skipBytes(mapData, 8);
				BufferConverter.skipBytes(mapData, 8);
				BufferConverter.skipBytes(mapData, 8);


				int entNr =  BufferConverter.buffer_read_u32(mapData);

				for(int i = 0;i<entNr;i++){
					int type = BufferConverter.buffer_read_u8(mapData);
					switch(type){
					case 0:
						byte eType = BufferConverter.buffer_read_u8_byte(mapData);

						if(version <= 1) {
							BufferConverter.skipBytes(mapData, 4);
							BufferConverter.skipBytes(mapData, 4);
							BufferConverter.skipBytes(mapData, 4);
	
							BufferConverter.skipBytes(mapData, 4);
							BufferConverter.skipBytes(mapData, 4);
							BufferConverter.skipBytes(mapData, 4);
							
						}else {
							BufferConverter.skipBytes(mapData, 8);
							BufferConverter.skipBytes(mapData, 8);
							BufferConverter.skipBytes(mapData, 8);
	
							BufferConverter.skipBytes(mapData, 8);
							BufferConverter.skipBytes(mapData, 8);
							BufferConverter.skipBytes(mapData, 8);
						}

						BufferConverter.skipBytes(mapData, 4);


						if(eType == 5){
							BufferConverter.skipBytes(mapData, 1);
						}

						if(version == 0){
							BufferConverter.skipBytes(mapData, 1);
						}


						break;
					case 1:
						if(version <= 1) {
							BufferConverter.skipBytes(mapData, 4*3);
						}else {
							BufferConverter.skipBytes(mapData, 8*3);
						}

						break;

					case 2:
						BufferConverter.skipBytes(mapData, 1);

						if(version <= 1) {
							BufferConverter.skipBytes(mapData, 4);
							BufferConverter.skipBytes(mapData, 4);
							BufferConverter.skipBytes(mapData, 4);
						}else {
							BufferConverter.skipBytes(mapData, 8);
							BufferConverter.skipBytes(mapData, 8);
							BufferConverter.skipBytes(mapData, 8);
						}

						BufferConverter.skipBytes(mapData, 4);

						break;
						
					case 3:
						BufferConverter.skipBytes(mapData, 1);
						
						BufferConverter.skipBytes(mapData, 8);
						BufferConverter.skipBytes(mapData, 8);
						BufferConverter.skipBytes(mapData, 8);
						BufferConverter.skipBytes(mapData, 8);
						BufferConverter.skipBytes(mapData, 8);
						
						BufferConverter.skipBytes(mapData, 4);
						break;
						
					case 4:
						BufferConverter.skipBytes(mapData, 1);
						
						BufferConverter.skipBytes(mapData, 8);
						BufferConverter.skipBytes(mapData, 8);
						BufferConverter.skipBytes(mapData, 8);
						BufferConverter.skipBytes(mapData, 8);
						BufferConverter.skipBytes(mapData, 8);
						
						BufferConverter.skipBytes(mapData, 4);
						break;

					case 128:
						
						BufferConverter.skipBytes(mapData, 1);

						if(version <= 1) {
							BufferConverter.skipBytes(mapData, 4);
							BufferConverter.skipBytes(mapData, 4);
							BufferConverter.skipBytes(mapData, 4);
	
							BufferConverter.skipBytes(mapData, 4);
							BufferConverter.skipBytes(mapData, 4);
							BufferConverter.skipBytes(mapData, 4);
						}else {
							BufferConverter.skipBytes(mapData, 8);
							BufferConverter.skipBytes(mapData, 8);
							BufferConverter.skipBytes(mapData, 8);

							BufferConverter.skipBytes(mapData, 8);
							BufferConverter.skipBytes(mapData, 8);
							BufferConverter.skipBytes(mapData, 8);
						}

						BufferConverter.skipBytes(mapData, 4);

						break;

					default:
						
						return false;//returns false if a invalid/unknown ID was detected



					}
				}
			}

		}




		return true;
	}


	public boolean mapDataIsNull(){
		if(map == null){
			return true;
		}else{
			return false;
		}
	}

	public ReentrantReadWriteLock getLock(){
		return mapLock;
	}

	public int getMapMagicID(){
		return mapFileMagicID;
	}

	private void incMapMagicID(){
		mapFileMagicID++;
		if(mapFileMagicID>65535){ //u16 integer
			mapFileMagicID = 0;
		}
	}

	public void reset(){
		mapLock.writeLock().lock();
		mapFileMagicID = 0;
		map = null;
		mapReady = false;
		mapLock.writeLock().unlock();
	}

	/**
	 * returns true if the map finished loading and can be read via the getMap() method
	 * @return
	 */
	public boolean mapReady(){
		return mapReady;
	}

	/**
	 * NOT THREAD-SAFE (for reading use the getLock() method and lock it manually!
	 * @return
	 */
	public byte[] getMapByteArray(){
		mapLock.readLock().lock();
		byte[] array = map.array().clone();
		mapLock.readLock().unlock();
		return array;
	}

}
