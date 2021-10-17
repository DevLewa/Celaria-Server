package server.util;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Game Makers endianess is different compared to javas default endianess.
 * This class provides functions for reading and writing of inputstreams, while converting
 * between BIG_ENDIAN and LITTLE_ENDIAN to ensure a working data transport between the server and the game 
 * without changes in the information.
 *  
 * TODO: Look into possible optimizations. (Some of the functions allocate objects on the heap
 * for the conversion process which could hinder performance.)
 * 
 * 
 *
 */


public class StreamConverter {

//---------------------------------------------------
	public static void writePacketHeaderTCP(DataOutputStream out,int packetID,int packetSize) throws IOException{
			//cspk as message header (celaria server package)
			buffer_write_u8(out,67);//C
			buffer_write_u8(out,83);//S
			buffer_write_u8(out,80);//P
			buffer_write_u8(out,84);//T
			
			buffer_write_u8(out,packetID);
			
			buffer_write_u16(out,packetSize);
			
	}
	
	public static void writePacketHeaderUDP(DataOutputStream out,int packetID) throws IOException{
			//cspk as message header (celaria server package)
			buffer_write_u8(out,67);//C
			buffer_write_u8(out,83);//S
			buffer_write_u8(out,80);//P
			buffer_write_u8(out,85);//U
			
			buffer_write_u8(out,packetID);
			
			

	}
	
	
	public static String buffer_read_chars_u8(DataInputStream in,int size) throws IOException{
			StringBuilder b = new StringBuilder();
			
			for(int i = 0;i<size;i++){
				b.append((char)(in.readByte() & 0xff));
			}
			
			return b.toString();
	}

	
	public static String buffer_read_chars_u16(DataInputStream in,int size) throws IOException{
		StringBuilder b = new StringBuilder();
		
		for(int i = 0;i<size;i++){
			b.append((char)buffer_read_u16(in));
		}
		
		return b.toString();
}
	
	public static int buffer_read_u8(DataInputStream in) throws IOException{
			return (in.readByte() & 0xff);
	}

	public static byte buffer_read_u8_byte(DataInputStream in) throws IOException{
		return (byte) (in.readByte() & 0xff);
}

	
	public static int buffer_read_u16(DataInputStream in) throws IOException{
			byte[] b = new byte[]{buffer_read_u8_byte(in),buffer_read_u8_byte(in)};
			ByteBuffer buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN); 
        	return buf.getShort()& 0xFFFF;  

	}
	
	
	public static int buffer_read_s16(DataInputStream in) throws IOException{
			byte[] b = new byte[]{in.readByte(),in.readByte()};
			ByteBuffer buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);  
        	return buf.getShort();

	}
	
	
	/**
	 * Skipped the UDP header which the GM client included in each packet
	 * (was nessecary due to a bug which is now fixed.)
	 * @param input
	 * @throws IOException
	 */
	public static void skipHeaderUDP(DataInputStream input) throws IOException{
			//input.skipBytes(12);
	}
	
	public static int buffer_read_u32(DataInputStream in) throws IOException{
			byte[] b = new byte[]{buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in)};
			ByteBuffer buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);  
        	return buf.getInt();  

	}
	
	
	public static int buffer_read_s32(DataInputStream in) throws IOException{
			byte[] b = new byte[]{in.readByte(),in.readByte(),in.readByte(),in.readByte()};
			ByteBuffer buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);  
        	return buf.getInt();  


	}
	
	public static float buffer_read_f32(DataInputStream in) throws IOException{
		byte[] b;
			b = new byte[]{buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in)};
			ByteBuffer buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);  
        	return buf.getFloat();  
	}

	public static double buffer_read_f64(DataInputStream in) throws IOException{
		byte[] b;
			b = new byte[]{buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in),buffer_read_u8_byte(in)};
			ByteBuffer buf = ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN);  
        	return buf.getDouble();  

	}
	
	
	public static void buffer_write_u32(DataOutputStream out,int i) throws IOException{
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN); 
		b.putInt(i);
		

			out.write(b.array());
		
	}
	
	public static void buffer_write_f32(DataOutputStream out,float i) throws IOException{
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN); 
		b.putFloat(i);
		out.write(b.array());
		
	}
	
	public static void buffer_write_f64(DataOutputStream out,double i) throws IOException{
		ByteBuffer b = ByteBuffer.allocate(8);
		b.order(ByteOrder.LITTLE_ENDIAN); 
		b.putDouble(i);
		out.write(b.array());
		
	}
	
	
	public static void buffer_write_u16(DataOutputStream out,int i) throws IOException{
		out.write((byte) i);
		out.write((byte) (i >> 8));
	}
	
	
	public static void buffer_write_chars_u8(DataOutputStream out,String str) throws IOException{
		for(int i = 0;i<str.length();i++){
			char c = str.charAt(i);
			buffer_write_u8(out, (int) c);
		}
	}
	
	public static void buffer_write_s32(DataOutputStream out,int i) throws IOException{
		ByteBuffer b = ByteBuffer.allocate(4);
		b.order(ByteOrder.LITTLE_ENDIAN); 
		b.putInt(i);
		

			out.write(b.array());
	}
	
	
	public static void buffer_write_s16(DataOutputStream out,int i) throws IOException{
		ByteBuffer b = ByteBuffer.allocate(2);
		b.order(ByteOrder.LITTLE_ENDIAN); 
		b.putShort((short)i);
		

			out.write(b.array());
	}
	
	public static void buffer_write_u8(DataOutputStream out,int i) throws IOException{
		out.writeByte((byte) i);
	}

}

