package server;

import io.ConfFileReader;
import window.Window;

public class main {
	public static void main(String[] args){
		
		ServerConfig conf = new ServerConfig();
		
		ConfFileReader reader = new ConfFileReader();
		reader.loadFile("./server.conf");
		
		
		conf.apply(reader);
		
		
		Server s = new Server(conf);//create server
		Window window = new Window(s);//create Window and pass server as Reference
		window.setServerHeader("Celaria Server "+s.SERVER_VERSION);//set WIndow hander

	
	}
}
