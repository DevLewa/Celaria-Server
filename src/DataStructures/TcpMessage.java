package DataStructures;

import java.nio.ByteBuffer;

public class TcpMessage {

	private int messageID;
	private ByteBuffer buffer;
	public TcpMessage(int messageID,ByteBuffer buffer){
		this.messageID = messageID;
		this.buffer = buffer;
	}
	
	
	public int getID(){
		return messageID;
	}
	
	public ByteBuffer getBuffer(){
		return buffer;
	}
}
