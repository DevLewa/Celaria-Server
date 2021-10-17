package io;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;

import debug.DebugIO;


/**
 * Loads configuration file, reads key/value pairs and puts them into a hashmap for easy readback
 * 
 * 
 *
 */

public class ConfFileReader {

	BufferedReader file;

	HashMap<String,String> stringMap;

	public ConfFileReader(){
		stringMap = new HashMap<String,String>();
	}
	



	public boolean loadFile(String filePath){
		try {
			file = new BufferedReader(new FileReader(filePath));

			parseAndFill(file);
			file.close();

			return true;
		} catch (FileNotFoundException e) {
			return false;
		} catch (IOException e) {
			return false;
		}
	}

	private void parseAndFill(BufferedReader reader) throws IOException{
		String line;
		while((line = reader.readLine()) != null)
		{
			parseLine(line);
		}

	}

	private void parseLine(String line){
		DebugIO.println(line);
		String[] parts = line.split("=");
		if(parts.length == 2){
			String key = parts[0].trim();
			String val = parts[1].trim();
			if(key.length() > 0 && val.length() > 0){
				stringMap.put(key,val);
			}
		}
	}

	public String get(String key){
		return stringMap.get(key);
	}
	
	public boolean exists(String key){
		return stringMap.containsKey(key);
	}



}
