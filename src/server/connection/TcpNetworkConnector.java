package server.connection;

import java.io.IOException;

import server.ServerCore;
import server.player.Player;

public class TcpNetworkConnector {

	TcpPlayerReader tcpReader;
	
	TcpPlayerWriter tcpWriter;
	
	
	Thread tcpReaderThread;
	Thread tcpWriterThread;
	
	public TcpNetworkConnector(ServerCore core){
		try {
			tcpReader = new TcpPlayerReader(core);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		tcpWriter = new TcpPlayerWriter(core);
		
		tcpReaderThread = new Thread(tcpReader,"TCP reader");
		tcpWriterThread = new Thread(tcpWriter,"TCP writer");
		
		tcpReaderThread.start();
		tcpWriterThread.start();
		
	}
	
	
	public TcpPlayerWriter getTcpWriter(){
		return tcpWriter;
	}
	
	
	public void registerPlayer(Player player){
		tcpReader.registerPlayer(player);
		tcpWriter.registerPlayer(player);
	}
	
	public void removePlayer(Player player){
		tcpReader.removePlayer(player);
		tcpWriter.removePlayer(player);
	}
	
	
	public void stop(){
		try {
			tcpReader.close();
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		tcpWriter.setRunning(false);
		
		try {
			tcpReader.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
	}
	
	
	public TcpPlayerReader getTcpReader(){
		return tcpReader;
	}
	
	public void close(){
		
	}
	
}
