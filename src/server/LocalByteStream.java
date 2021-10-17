package server;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;


/**
 * ByteStream class which was made in order to simplify message sending/recieving on the server.
 * 
 * 
 *
 */

public class LocalByteStream {
	
	private ByteArrayOutputStream byteOut;
	private DataOutputStream output;
	
	public LocalByteStream(){
		byteOut = new ByteArrayOutputStream();
		output = new DataOutputStream(byteOut);
	}
	
	
	public ByteArrayOutputStream getByteArrayOutputStream(){
		return byteOut;
	}
	
	public DataOutputStream getDataOutputStream(){
		return output;
	}
	
	public byte[] toByteArray(){
		return byteOut.toByteArray();
	}
	

}
