package server.util;


import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class BufferConverter {
	
	public static final int TCPHEADERSIZE = 7;
	public static final int UDPHEADERSIZE = 7;
	
	public static String buffer_read_chars_u8(ByteBuffer in,int size){
		StringBuilder b = new StringBuilder();
		
		for(int i = 0;i<size;i++){
			b.append((char)(in.get() & 0xff));
		}
		
		return b.toString();
}


public static String buffer_read_chars_u16(ByteBuffer in,int size){
	StringBuilder b = new StringBuilder();
	
	for(int i = 0;i<size;i++){
		b.append((char)buffer_read_u16(in));
	}
	
	return b.toString();
}

public static int buffer_read_u8(ByteBuffer in){
		return (in.get() & 0xff);
}

public static byte buffer_read_u8_byte(ByteBuffer in){
	return (byte) (in.get() & 0xff);
}


public static int buffer_read_u16(ByteBuffer in){
		byte[] b = new byte[]{buffer_read_u8_byte(in),buffer_read_u8_byte(in)};
		ByteBuffer buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN); 
    	return buf.getShort()& 0xFFFF;  

}


public static int buffer_read_s16(ByteBuffer in){
		byte[] b = new byte[]{in.get(),in.get()};
		ByteBuffer buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);  
    	return buf.getShort();

}


/**
 * Skipped the UDP header which the GM client included in each packet
 * (was nessecary due to a bugin GM which is now fixed.)
 * @param input
 * @throws IOException
 */
public static void skipHeaderUDP(ByteBuffer input){
		//input.skipBytes(12);
}

public static void skipBytes(ByteBuffer input,int bytes){
	input.position(input.position()+bytes);
}

public static int buffer_read_u32(ByteBuffer in){
		byte[] b = new byte[]{buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in)};
		ByteBuffer buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);  
    	return buf.getInt();  

}

public static long buffer_read_u64(ByteBuffer in){
	byte[] b = new byte[]{buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in)};
	ByteBuffer buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);  
	return buf.getLong();  

}


public static int buffer_read_s32(ByteBuffer in){
		byte[] b = new byte[]{in.get(),in.get(),in.get(),in.get()};
		ByteBuffer buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);  
    	return buf.getInt();  


}

public static float buffer_read_f32(ByteBuffer in){
	byte[] b;
		b = new byte[]{buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in)};
		ByteBuffer buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);  
    	return buf.getFloat();  
}

public static double buffer_read_f64(ByteBuffer in){
	byte[] b;
		b = new byte[]{buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in)};
		ByteBuffer buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);  
    	return buf.getDouble();  

}


public static void buffer_write_u32(ByteBuffer out,int i){
	ByteBuffer b = ByteBuffer.allocate(4);
	b.order(ByteOrder.LITTLE_ENDIAN); 
	b.putInt(i);
	out.put(b.array());
	
}



public static void buffer_write_f32(ByteBuffer out,float i){
	ByteBuffer b = ByteBuffer.allocate(4);
	b.order(ByteOrder.LITTLE_ENDIAN); 
	b.putFloat(i);
	out.put(b.array());
	
}

public static void buffer_write_f64(ByteBuffer out,double i){
	ByteBuffer b = ByteBuffer.allocate(8);
	b.order(ByteOrder.LITTLE_ENDIAN); 
	b.putDouble(i);
	out.put(b.array());
	
}


public static void buffer_write_u16(ByteBuffer out,int i){	
	out.put((byte) i);
	out.put((byte) (i >> 8));
}


public static void buffer_write_chars_u8(ByteBuffer out,String str){
	for(int i = 0;i<str.length();i++){
		char c = str.charAt(i);
		buffer_write_u8(out, (int) c);
	}
}

public static void buffer_write_chars_u16(ByteBuffer out,String str){
	for(int i = 0;i<str.length();i++){
		char c = str.charAt(i);
		buffer_write_u16(out, (int) c);
	}
}

public static void buffer_write_s32(ByteBuffer out,int i){
	ByteBuffer b = ByteBuffer.allocate(4);
	b.order(ByteOrder.LITTLE_ENDIAN); 
	b.putInt(i);
	
	out.put(b.array());
}

public static void buffer_write_u8(ByteBuffer out,int i){
	out.put((byte) i);
}


//---------------------------------------------------
	public static void writePacketHeaderTCP(ByteBuffer out,int packetID,int packetSize){
			buffer_write_u8(out,67);//C
			buffer_write_u8(out,83);//S
			buffer_write_u8(out,80);//P
			buffer_write_u8(out,84);//T
			
			buffer_write_u8(out,packetID);
			
			buffer_write_u16(out,packetSize);
			
	}
	
	public static void writePacketHeaderUDP(ByteBuffer out,int packetID){
			//cspk as message header (celaria server package)
			buffer_write_u8(out,67);//C
			buffer_write_u8(out,83);//S
			buffer_write_u8(out,80);//P
			buffer_write_u8(out,85);//U
			
			buffer_write_u8(out,packetID);
			
	}




}
