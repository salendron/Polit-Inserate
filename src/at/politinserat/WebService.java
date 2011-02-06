package at.politinserat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class WebService {

	public static JSONObject buildLoginJson(String username, String password) throws JSONException{
		JSONObject json = new JSONObject();
		JSONArray params = new JSONArray();
    	
		params.put(username);
		params.put(password);
		
		json.put("method", "login"); 
		json.put("params", params);
		json.put("id", 99);
    	
    	return json;
	}
	
	public static JSONObject buildUploadJson(String username, String password, String imagePath) throws JSONException, IOException{
		JSONObject json = new JSONObject();
		JSONArray params = new JSONArray();
    	
		params.put(username);
		params.put(password);
		params.put(encodeFile(imagePath));
		params.put(4); //4 identifies android - {4->Android, 5->iPhone, 6->Nokia}
		
		json.put("method", "submit"); 
		json.put("params", params);
		json.put("id", 99);
    	
    	return json;
	}
	
	private static String encodeFile(String path) throws IOException{
    	File f = new File(path);
    	FileInputStream fs = new FileInputStream(f);
    	
    	byte[] b = new byte[(int) f.length()];
    	fs.read(b);
    	
    	return Base64.encodeBytes(b);
    }
	
	public static String Post(String url, String body) throws IOException{
		URL url1;
        URLConnection urlConnection;
        DataOutputStream outStream;
        DataInputStream inStream;
        
        // Create connection
        url1 = new URL(url);
        urlConnection = url1.openConnection();
        ((HttpURLConnection)urlConnection).setRequestMethod("POST");
        urlConnection.setDoInput(true);
        urlConnection.setDoOutput(true);
        urlConnection.setUseCaches(false);
        urlConnection.setRequestProperty("Content-Type", "application/json");
        urlConnection.setRequestProperty("Content-Length", ""+ body.length());
 
        
        // Create I/O streams
        outStream = new DataOutputStream(urlConnection.getOutputStream());
 
        outStream.writeBytes(body);
        //outStream.writeBytes();
        outStream.flush();
        outStream.close();
 
        inStream = new DataInputStream(urlConnection.getInputStream()); 
        
        // Get Response  
        String buffer;  
        StringBuilder response = new StringBuilder();
        while((buffer = inStream.readLine()) != null) {  
            response.append(buffer);
        }  
        
        // Close I/O streams
        inStream.close();
        
        return response.toString();  
    	
    }
	
	public static boolean doLogin(String url,String body) throws Exception{
		//{"result":false,"id":"99","error":null}
		String response = Post(url, body);
		
		JSONObject jsonResponse = new JSONObject(response);
		
		if(jsonResponse.has("result")){
			return jsonResponse.getBoolean("result");
		} else {
			throw new Exception(response);
		}
	}
	
	public static JSONObject doUpload(String url,String body) throws Exception{
		String response = Post(url, body);
		
		JSONObject jsonResponse = new JSONObject(response);
		if(jsonResponse.has("result")){
			return jsonResponse.getJSONObject("result");
		} else {
			throw new Exception(response);
		}
	}
	
}
