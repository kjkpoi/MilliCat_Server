package com.millicat.data;

import java.util.Vector;

public class TrendStory {

	Vector<String> titles;
	String imageUrl;
	
	public TrendStory() {
		titles = new Vector<>();
	}
	
	public TrendStory(String title, String imageUrl) {
		titles = new Vector<>();
		setTitles(title);
		setImageUrl(imageUrl);
	}

	public Vector<String> getTitles() {
		return titles;
	}

	public void setTitles(String title) {
		String[] titleArray = title.split(",");
		for(int i = 0; i < titleArray.length; i++) {
			titles.add(titleArray[i].trim());
		}
	}

	public String getImageUrl() {
		return imageUrl;
	}

	public void setImageUrl(String imageUrl) {
		this.imageUrl = imageUrl;
	}
	
	
}
