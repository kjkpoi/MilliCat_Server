package com.millicat;

import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.Vector;

import javax.imageio.ImageIO;
import javax.servlet.http.HttpServletResponse;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import cmu.arktweetnlp.RunTagger;
import edu.stanford.nlp.util.Pair;
import spark.Request;
import spark.Response;
import spark.Route;
import spark.Spark;

/**
 * A simple example just showing some basic functionality
 *
 * @author Per Wendel
 */


public class Main {
	
	static RunTagger tagger = null;
	static HashMap<String, String> nounTypeList;
	static BufferedWriter unknown_list_writer;
	static TrendSearch trendSearch;
	
	public static String makeJson(ArrayList<String> imagePathList) {
		if(imagePathList == null)
			return null;
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
	
	public static ArrayList<String> getImage(String sentence) {
		System.out.println("Sentence -> " + sentence);
		ArrayList<String> result = new ArrayList<>();
		Vector<Pair<String, String> > tagInfo = null;
		String lastWord = null;
		
		
		//google trend search
		/*lastWord = trendSearch.compareSentece(sentence);
		if(lastWord != null) {
			System.out.println("Keyword from Google Trend -> " + lastWord);
		}*/
		
		if(lastWord == null) {
			lastWord = "";
			try {
				tagInfo = tagger.getTaggingInformation(sentence);
			} catch (ClassNotFoundException | IOException e2) {
				e2.printStackTrace();
			}
		
			for(int i = tagInfo.size() - 1; i >= 0; i--) {
				String currentWord = tagInfo.get(i).first;
				String currentType = tagInfo.get(i).second;
				
				if(currentType.equals("A") && lastWord.length() > 0) {
					lastWord = currentWord + " " + lastWord;
				} else if(currentType.equals("N") || currentType.equals("^")) {
					if(lastWord.length() <= 0){
						if(nounTypeList.containsKey(currentWord)) {
							if(nounTypeList.get(currentWord).equals("A")) {
								break;
							}
						} else {
							writeUnknownNoun(currentWord);
						}
					}
					
					lastWord = currentWord + " " + lastWord;
				} else {
					break;
				}
			}
			
			for(int i = 0; i < tagInfo.size(); i++) {
				System.out.println(tagInfo.get(i).first + ", " + tagInfo.get(i).second);
			}
			
			System.out.println("lastword: " + lastWord);
			if(lastWord.length() <= 0)
				return null;
			lastWord = lastWord.trim();
			
		}
		//lastWord = sentence.substring(sentence.lastIndexOf(" ")+1);
		
		File dir = new File("image/" + lastWord);
		
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
	        final String query = URLEncoder.encode(lastWord, Charset.defaultCharset().name());
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
	            
	            String googleTrendImageUrl = trendSearch.isTrendStory(lastWord);
	            if(googleTrendImageUrl != null) {
	            	resultsLength--;
	            	download(trendSearch.isTrendStory(lastWord), "image/" + lastWord + "/" + 2 + ".jpg");
	            	result.add("image/" + lastWord + "/" + 2 + ".jpg");
	            }
	            	
	            for (int i = 0; i < resultsLength; i++) {
	                final JSONObject aResult = results.getJSONObject(i);
	                final JSONObject thumbnail = aResult.getJSONObject("Thumbnail");
	                //String title =  aResult.get("Title").toString().replace(" ", "");
	                download(thumbnail.get("MediaUrl").toString(), "image/" + lastWord + "/" + i + ".jpg");
	                result.add("image/" + lastWord + "/" + i + ".jpg");
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
	
	public static void writeUnknownNoun(String noun) {
		try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("unknown_noun_list.txt", true)))) {
		    out.println(noun);
		}catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	//C: concrete, A: abstract, U: unknown
	public static void getNounList() {
		nounTypeList = new HashMap<>();
		trendSearch = new TrendSearch();
		trendSearch.init();
		BufferedReader in;
		try {
			String s;
			
			in = new BufferedReader(new FileReader("concrete_noun_list.txt"));
			while ((s = in.readLine()) != null) {
				nounTypeList.put(s.trim(), "C");
			}
			in.close();
			
			in = new BufferedReader(new FileReader("abstract_noun_list.txt"));
			while ((s = in.readLine()) != null) {
				nounTypeList.put(s.trim(), "A");
			}

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	public static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}
	
	public static void webp(String imagePath) { 
		
	}
	
    public static void main(String[] args) {
    	String command = "webp/cwebp";
    	try {
	        Process child = Runtime.getRuntime().exec(command);
	
	        BufferedReader in = new BufferedReader(
                    new InputStreamReader(child.getInputStream()));
			String line = null;
			while ((line = in.readLine()) != null) {
			    System.out.println(line);
			}    	
    	} catch(Exception e) {
    		e.printStackTrace();
    	}
    	
    	
    	
    	getNounList();
    	
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
		
		/*Spark.get("/image/list/*", new Route() {
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
        });*/	
		
		Spark.get("/image/list/*", new Route() {
			@Override
			public Object handle(Request request, Response response) throws Exception {
				String sentence = request.splat()[0].trim();
				String lastWord = sentence.substring(sentence.lastIndexOf(" ")+1);
				
				String json = makeJson(getImage(sentence.trim()));
				response.type("application/json");
				if(json == null)
					json = "{\"images\":[]}";
				String length = Integer.toString(json.length());
				response.header("Content-Length", length);
				response.header("Connection", "keep-alive");
				System.out.println("/image/list/" + lastWord + " -> " + json);
				return json;
			}
        });
	
    }
    
}
