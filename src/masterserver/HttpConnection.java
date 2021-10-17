package masterserver;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import server.ServerCore;


public class HttpConnection {

	private static final int CLIENT_HTTP_VERSION = 1;

	
	public static final String URL_BASE = "https://serverapi.celaria.com/"+CLIENT_HTTP_VERSION+"/";


	public static final String URL_REGISTER = URL_BASE+"register.php";
	public static final String URL_REMOVE = URL_BASE+"remove.php";


	public static final String URL_CHECK_IP = "https://serverapi.celaria.com/checkIP.php";


	
	public static class Response{
		
		public boolean success;//if the response was successful
		public String errorMessage;//errormessage (is filled if success == false);
		
		public String content;
	}
	

	public static class ResponseMap{

		boolean success;//if the response was successful
		String errorMessage;//errormessage (is filled if success == false);

		Map<String,String> values;//other values
		
		public ResponseMap(boolean success){
			this.success = success;
			values = new HashMap<String,String>();
		}
		void setErrorMessage(String errorMessage) {
			this.errorMessage = errorMessage;
		}

		
		void addEntry(String key,String value) {
			values.put(key, value);
		}
		
		public boolean hasKey(String key) {
			return values.get(key) != null;
		}
		
		public String getValue(String key) {
			return values.get(key);
		}
		
		
		public boolean getSuccess() {
			return success;
		}


		public String getError() {
			return errorMessage;
		}
	}


	/**
	 * converts map with parameters into a UTF8 byte sequence
	 * 
	 * @param parameters
	 * @return
	 * @throws UnsupportedEncodingException
	 */
	public static byte[] toParamBytes(Map<String,String> parameters) throws UnsupportedEncodingException {

		StringJoiner combined = new StringJoiner("&");
		for(Map.Entry<String,String> entry : parameters.entrySet()) {
			combined.add(
					URLEncoder.encode(entry.getKey(), "UTF-8")
					+ "=" + 
					URLEncoder.encode(entry.getValue(), "UTF-8")
					);
		}


		byte[] ret = combined.toString().getBytes(StandardCharsets.UTF_8);

		return ret;
	}


	/**
	 * executes HTTP post request
	 * @param targetURL
	 * @param parameters
	 * @return http request output
	 */

	//from https://stackoverflow.com/questions/1359689/how-to-send-http-request-in-java
	public static String executePost(String targetURL, Map<String,String> parameters){
		HttpURLConnection connection = null;


		byte[] parameter = new byte[0];
		if(parameters != null) {
			try {
				parameter = toParamBytes(parameters);
			} catch (UnsupportedEncodingException e1) {
				return null;
			}
		}

		try {
			//Create connection
			URL url = new URL(targetURL);
			connection = (HttpURLConnection) url.openConnection();
			connection.setRequestMethod("POST");
			connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

			connection.setRequestProperty("Content-Length", Integer.toString(parameter.length));
			connection.setRequestProperty("Content-Language", "en-US");  

			connection.setUseCaches(false);
			connection.setDoOutput(true);

			//Send request
			DataOutputStream wr = new DataOutputStream (
					connection.getOutputStream());
			wr.write(parameter);
			wr.flush();
			wr.close();

			//Get Response  
			InputStream is = connection.getInputStream();
			BufferedReader rd = new BufferedReader(new InputStreamReader(is));
			StringBuilder response = new StringBuilder();

			String line = rd.readLine();
			if(line != null) {
				response.append(line);
				
				while ((line = rd.readLine()) != null) {
					response.append('\r');
					response.append(line);
				}
			}
			
			
			rd.close();
			return response.toString();
		} catch (Exception e) {
			//e.printStackTrace();
			return null;
		} finally {
			if (connection != null) {
				connection.disconnect();
			}
		}
	}


	//if input is not a valid json, it returns null
	private static JsonObject getJsonObject(String json) {

		try {
			JsonObject obj = new Gson().fromJson(json, JsonObject.class);
			return obj;
		} catch(com.google.gson.JsonSyntaxException ex) { 
			return null;
		}

	}

	private static ResponseMap jsonToResponse(JsonObject jsonObj) {
		if(jsonObj != null) {
			if(jsonObj.has("version")) {
				
				if(jsonObj.has("error")) {
					ResponseMap re = new ResponseMap(false);
					re.setErrorMessage(jsonObj.get("error").toString());
					return re;
				}else {
					HttpConnection.ResponseMap response = new HttpConnection.ResponseMap(true);
					
					for (Map.Entry<String, JsonElement> entry : jsonObj.entrySet()) {
						response.addEntry(entry.getKey(), entry.getValue().getAsString());
					}
					
					return response;
				}
			}
		}

		ResponseMap r = new ResponseMap(false);
		r.setErrorMessage("Unknown failure");
		return r;
	}

	/**
	 * registers server online
	 * @param serverName
	 * @param PORT
	 * @return
	 */
	public static ResponseMap registerOnline(String serverName,int PORT,int currentPlayerCount,int maxPlayerCount,boolean hasPassword,int version) {

		Map<String,String> parameters = new HashMap<String,String>();


		parameters.put("SERVERNAME", serverName);
		parameters.put("PORT", Integer.toString(PORT));

		parameters.put("PLAYERCOUNT", Integer.toString(currentPlayerCount));
		parameters.put("MAXPLAYERCOUNT", Integer.toString(maxPlayerCount));


		parameters.put("VERSION", Integer.toString(version));//SERVER VERSION! (not serverAPI version.)
		parameters.put("PASSWORD", Integer.toString(hasPassword?1:0));
		parameters.put("MODDED", Integer.toString(ServerCore.MODIFIED?1:0));
		

		String response = executePost(URL_REGISTER,parameters);

		JsonObject jobj = getJsonObject(response);
		

		
		
		
		if(jobj != null) {
			return jsonToResponse(jobj);
		}
		
		ResponseMap r = new ResponseMap(false);
		r.setErrorMessage("HTTP Error");
		return r;
	}

	
	public static Response getIP() {
		Response r = new Response();
		
		String response = executePost(URL_CHECK_IP,null);
		if(response != null) {
			r.success = true;
			r.content = response;
		}
		
		return r;
		
	}

	public static ResponseMap removeFromOnline(int PORT) {

		Map<String,String> parameters = new HashMap<String,String>();

		parameters.put("PORT", Integer.toString(PORT));
		
		String response = executePost(URL_REMOVE,parameters);

		JsonObject jobj = getJsonObject(response);
		if(jobj != null) {
			return jsonToResponse(jobj);
		}

		ResponseMap r = new ResponseMap(false);
		r.setErrorMessage("HTTP Error");
		return r;
	}



}
