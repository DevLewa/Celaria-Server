package server;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

import server.util.FileUtil;

/**
 * This class is responsible for loading external Ressources. (primarly the map)
 * 
 * 
 *
 */


public class RessourceHandler {
	
	
	String[] mapFileList;
	GameMap mapFile;//Manages the mapfile
	
	ServerCore server;
	public RessourceHandler(ServerCore server){
		
		
		mapFileList = null;
		
		this.server = server;
		
		mapFile = new GameMap();
		
	}
	
	public void reset(){
		
		mapFileList = null;
		mapFile.reset();
	}
	

	public GameMap getMapHandler(){
		return mapFile;
	}
	
	public void setMapFileList(String[] list){
		this.mapFileList = list;
	}
	
	public String[] getMapFileList(){
		return mapFileList;
	}
	
}
