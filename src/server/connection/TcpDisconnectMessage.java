package server.connection;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import server.Server.TCPDISCONNECTTYPE;

public class TcpDisconnectMessage implements Runnable{

	SocketChannel socket;
	TCPDISCONNECTTYPE disconnectType;
	String message;

	public TcpDisconnectMessage(SocketChannel responseSocket,TCPDISCONNECTTYPE disconnectType,String message){
		this.socket = responseSocket;
		this.disconnectType = disconnectType;
		this.message = message;
	}

	@Override
	public void run() {
		try {
			TcpPlayerWriter.sendDirectDisconnectTCPMessage(socket, disconnectType,this.message);

			socket.socket().close();
			socket.close();//close socket
				
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
