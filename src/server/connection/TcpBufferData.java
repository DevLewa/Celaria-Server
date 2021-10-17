package server.connection;

import java.nio.ByteBuffer;

public class TcpBufferData {

	private ByteBuffer storageBuffer;
	
	
	private int numberOfBytes;
	
	public TcpBufferData(){
		storageBuffer = ByteBuffer.allocate(2048);
		storageBuffer.clear();
		storageBuffer.position(0);
		numberOfBytes = 0;
	}
	
	
	public ByteBuffer getStorageBuffer(){
		return storageBuffer;
	}
	
	
	public int byteCount(){
		return numberOfBytes;
	}
	
	public void addByteCount(int add){
		numberOfBytes+=add;
	}
	
	
	public void removeByteCount(int subs){
		numberOfBytes-=subs;
	}
}
