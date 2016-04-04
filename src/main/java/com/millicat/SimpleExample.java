package com.millicat;

import static spark.Spark.get;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONObject;

import cmu.arktweetnlp.RunTagger;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

/**
 * A simple example just showing some basic functionality
 *
 * @author Per Wendel
 */


public class SimpleExample {
	
	public static String makeJson(ArrayList<String> imagePathList) {
		
		//String reuslt = "{\"images\":["jupiter/Júpiter.png","jupiter/jupiter.jpg","jupiter/opo9113a.jpg"]}"
		String result = "{\"images\":[";
		for(int i = 0; i < imagePathList.size(); i++) {
			result += "\"image/" + imagePathList.get(i).replace("\\", "/") + "\",";
		}
		if(result.charAt(result.length() - 1) == ',')
			result = result.substring(0, result.length() - 1);
		result += "]}";
		return result;
	}
	
	public static void download(String url, String path) {
		try { 
			URL downloadUrl = new URL(url);
			InputStream in = new BufferedInputStream(downloadUrl.openStream());
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buf = new byte[1024];
			int n = 0;
			while (-1!=(n=in.read(buf))) {
			   out.write(buf, 0, n);
			}
			out.close();
			in.close();
			byte[] response = out.toByteArray();
			FileOutputStream fos = new FileOutputStream(path);
			fos.write(response);
			fos.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static ArrayList<String> getImage(String keyword) {
		ArrayList<String> result = new ArrayList<>();
		
		File dir = new File("image/" + keyword);

		if(dir.exists()){ 
			//String path="C:\";
			File []fileList=dir.listFiles();
			for(File tempFile : fileList)
			  result.add(tempFile.toString());
			 return result;
		}
		dir.mkdirs();
		final String accountKey = "vpAvo/qUI0Ov+6FqoORBC/w4Z1MZu9kpy2T3Jjd9oy0";
        final String bingUrlPattern = "https://api.datamarket.azure.com/Bing/Search/v1/Image?Query=%%27%s%%27&$format=JSON";

        try {
	        final String query = URLEncoder.encode(keyword, Charset.defaultCharset().name());
	        final String bingUrl = String.format(bingUrlPattern, query);
	
	        final String accountKeyEnc = Base64.getEncoder().encodeToString((accountKey + ":" + accountKey).getBytes());
	
	        final URL url = new URL(bingUrl);
	        final URLConnection connection = url.openConnection();
	        connection.setRequestProperty("Authorization", "Basic " + accountKeyEnc);
	
	        try (final BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
	            String inputLine;
	            final StringBuilder response = new StringBuilder();
	            while ((inputLine = in.readLine()) != null) {
	                response.append(inputLine);
	            }
	            final JSONObject json = new JSONObject(response.toString());
	            final JSONObject d = json.getJSONObject("d");
	            final JSONArray results = d.getJSONArray("results");
	            int resultsLength = results.length();
	            if(resultsLength > 3)
	            	resultsLength = 3;
	            
	            for (int i = 0; i < resultsLength; i++) {
	                final JSONObject aResult = results.getJSONObject(i);
	                final JSONObject thumbnail = aResult.getJSONObject("Thumbnail");
	                //String title =  aResult.get("Title").toString().replace(" ", "");
	                download(thumbnail.get("MediaUrl").toString(), "image/" + keyword + "/" + i + ".jpg");
	                result.add("image/" + keyword + "/" + i + ".jpg");
	                /*Spark.get("/test", new Route() {
	    				@Override
	    				public Object handle(Request request, Response response) throws Exception {
	    					return "hello";
	    				}
	                });*/
	            }
	        } catch (Exception e) {
	        	e.printStackTrace();
			}
        } catch (Exception e) {
        	e.printStackTrace();
        }
        return result;
	}
	
	
    public static void main(String[] args) {
		get("/hello", (req, res) -> "Hello World");
		//getImage("apple");
		
		RunTagger tagger = null;
		try {
			tagger = new RunTagger();
		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		tagger.outputFormat = "conll";
		tagger.inputFilename = "test.txt";
		try {
			tagger.finalizeOptions();
			tagger.taggerInitialize();
			tagger.isLastwordNoun("hello, i'm joon gyum kim");
			//tagger.getTaggingInformation("hello, i'm bat man in chicago.");
			//tagger.getTaggingInformation("hello, i'm catholic.");
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
		
		
		Spark.get("/image/image/*", new Route() {
			@Override
			public Object handle(Request request, Response response) throws Exception {
				String filePath = "image/" + request.splat()[0];
				File f = new File(filePath);
				if(!f.exists() || f.isDirectory()) { 
					return null;
				}
				
				HttpServletResponse raw = response.raw();
				BufferedImage bufferedImage = ImageIO.read(Files.newInputStream(Paths.get(filePath)));
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(bufferedImage, "jpg", baos);
				baos.flush();
				byte[] imageInByte = baos.toByteArray();
				baos.close();
				raw.getOutputStream().write(imageInByte);
				raw.getOutputStream().flush();
				raw.getOutputStream().close();
				response.type("image/jpeg");
				response.header("Content-Length", Integer.toString(imageInByte.length));
				System.out.println("/image/image/" + filePath);
				return response.raw();
			}
        });
		
		Spark.get("/test", new Route() {
			@Override
			public Object handle(Request request, Response response) throws Exception {
				HttpServletResponse raw = response.raw();
				BufferedImage bufferedImage = ImageIO.read(Files.newInputStream(Paths.get("image/apple/0.jpg")));
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				ImageIO.write(bufferedImage, "jpg", baos );
				baos.flush();
				byte[] imageInByte = baos.toByteArray();
				baos.close();
				raw.getOutputStream().write(imageInByte);
				raw.getOutputStream().flush();
				raw.getOutputStream().close();
				return response.raw();
			}
        });
		
		Spark.get("/image/list/*", new Route() {
			@Override
			public Object handle(Request request, Response response) throws Exception {
				String keyword = request.splat()[0];
				String json = makeJson(getImage(keyword));
				response.type("application/json");
				response.header("Content-Length", Integer.toString(json.length()));
				response.header("Connection", "keep-alive");
				System.out.println("/image/list/" + keyword + " -> " + json);
				return json;
			}
        });	
		
		Spark.post("/chat/*", new Route() {
			@Override
			public Object handle(Request request, Response response) throws Exception {
				String keyword = request.splat()[0];
				String json = makeJson(getImage(keyword));
				response.type("application/json");
				response.header("Content-Length", Integer.toString(json.length()));
				response.header("Connection", "keep-alive");
				System.out.println("/image/list/" + keyword + " -> " + json);
				return json;
			}
        });
    }
    
}
