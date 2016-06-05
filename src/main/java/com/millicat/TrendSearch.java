package com.millicat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class TrendSearch {
	
	HashSet<String> hotTrendKeywords;
	HashSet<String> topChartKeywords;
	HashMap<String, String> trendStoryKeywords;
	
	private static String readAll(Reader rd) throws IOException {
		StringBuilder sb = new StringBuilder();
		int cp;
		while ((cp = rd.read()) != -1) {
			sb.append((char) cp);
		}
		return sb.toString();
	}

	private static JSONObject readJsonFromUrl(String url) throws IOException, JSONException {
		InputStream is = new URL(url).openStream();
		try {
			BufferedReader rd = new BufferedReader(new InputStreamReader(is, Charset.forName("UTF-8")));
			String jsonText = readAll(rd);
			jsonText = jsonText.substring(jsonText.indexOf("{"));
			JSONObject json = new JSONObject(jsonText);
			return json;
		} finally {
			is.close();
		}
	}

	public TrendSearch() {
		hotTrendKeywords = new HashSet<>();
		topChartKeywords = new HashSet<>();
		trendStoryKeywords = new HashMap<>();
	}
	
	public void init()
	{
		JSONObject json;
		try {
			//**Google Hot trend init**
			json =  readJsonFromUrl("https://www.google.com/trends/hottrends/visualize/internal/data");
			JSONArray usa_result = json.getJSONArray("united_states");
			for (int i = 0; i < usa_result.length(); i++) {
				hotTrendKeywords.add(usa_result.get(i).toString().toLowerCase());
			}

		
			//**Google top chart init**
			
			//get current day
			Calendar calendar = Calendar.getInstance( );
			int month =  calendar.get(Calendar.MONTH);
			String currentDate = String.valueOf(month);
			if(month < 10)
				currentDate = "0" + currentDate;
			currentDate = calendar.get(Calendar.YEAR) + currentDate;
			
			json =  readJsonFromUrl("https://www.google.com/trends/topcharts/category?date=" + currentDate + "&geo=US");
			JSONArray chartList = json.getJSONObject("data").getJSONArray("chartList");
			
			for(int i = 0; i < chartList.length(); i++) {
				JSONArray entityList = null;
				if(!chartList.getJSONObject(i).isNull("trendingChart"))
					entityList = chartList.getJSONObject(i).getJSONObject("trendingChart").getJSONArray("entityList");
				else if((!chartList.getJSONObject(i).isNull("topChart")))
					entityList = chartList.getJSONObject(i).getJSONObject("topChart").getJSONArray("entityList");
				
				assert entityList != null : "entityList Error";
					
				for(int j = 0; j < entityList.length(); j++) {
					topChartKeywords.add(entityList.getJSONObject(j).get("title").toString().toLowerCase());
				}
			}
			
			
			//**Google trend story init**

			json =  readJsonFromUrl("https://www.google.com/trends/api/stories/latest?hl=en-US&cat=m&fi=15&fs=15&geo=US&ri=300&rs=15&tz=-540");
			JSONArray storyList = json.getJSONObject("storySummaries").getJSONArray("trendingStories");
			
			for(int i = 0; i < storyList.length(); i++) {
				String[] titles = storyList.getJSONObject(i).get("title").toString().split(",");
				String imageUrl = null;
				if(!storyList.getJSONObject(i).getJSONObject("image").isNull("imgUrl"))
					imageUrl = storyList.getJSONObject(i).getJSONObject("image").get("imgUrl").toString();
				for(int j = 0 ; j < titles.length; j++) {
					titles[j].trim();
					trendStoryKeywords.put(titles[j].toLowerCase().trim(), "http:" + imageUrl);
				}
			}
			
		} catch (JSONException | IOException e) {
			e.printStackTrace();
		}	
	}
	
	private boolean stringMatch(String sentence, String keyword) {
		if(sentence.length() < keyword.length() || keyword.length() < 3)
			return false;
		
		String left = sentence.substring(sentence.length() - keyword.length()).toLowerCase();
		String right = keyword.toLowerCase();
		if(left.equals(right))
			return true;
		
		return false;
	}
	
	public String compareSentece(String sentence) {
		
		Iterator<String> it = hotTrendKeywords.iterator();
		while(it.hasNext()) {
			String currentKeyword = it.next();
			if(stringMatch(sentence, currentKeyword))
				return currentKeyword;
		}
		
		it = topChartKeywords.iterator();
		while(it.hasNext()) {
			String currentKeyword = it.next();
			if(stringMatch(sentence, currentKeyword))
				return currentKeyword;
		}
		
		it = trendStoryKeywords.keySet().iterator();
		while(it.hasNext()) {
			String currentKeyword = it.next();
			if(stringMatch(sentence, currentKeyword))
				return currentKeyword;
		}
		
		return null;
	}
	
	public String isTrendStory(String keyword) {
		return trendStoryKeywords.get(keyword.toLowerCase());
	}
	
}
