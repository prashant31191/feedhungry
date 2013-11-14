package com.yairkukielka.feedhungry.feedly;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.yairkukielka.feedhungry.toolbox.DateUtils;

public class Entry {
	private String id;
	private String originId;
	private String originTitle;
	private String title;
	private boolean unread;
	private List<Category> categories = new ArrayList<Category>();
	// TODO private List<Category> tags;
	private Date published;
	private Date crawled;
	private String author;
	private String content;
	private String visual;
	private String url;
	
	public Entry() {
	}
	public Entry(JSONObject jobject) throws JSONException {
		id = jobject.getString("id");
		if (jobject.has("origin")) {
			JSONObject originObject = jobject.getJSONObject("origin");
			originTitle = originObject.getString("title");	
		}
		if (jobject.has("originId")) {
			originId = jobject.getString("originId");	
		}
		title = jobject.getString("title");	
		unread = jobject.getBoolean("unread");
		JSONArray jsonCategories = jobject.getJSONArray("categories");
		for (int i = 0; i < jsonCategories.length(); i++) {
			Category c = new Category((JSONObject) jsonCategories.get(i));	
			categories.add(c);
		}
		if (jobject.has("published")) {
			published = DateUtils.getDateFromJson(jobject.getLong("published"));	
		}
		if (jobject.has("crawled")) {
			crawled = DateUtils.getDateFromJson(jobject.getLong("crawled"));	
		}
		if (jobject.has("author")) {
			author = jobject.getString("author");
		}	
		if (jobject.has("visual")) {
			JSONObject visualObject = jobject.getJSONObject("visual");
			// if no image, visual = "none"
			if (!"none".equals(visualObject.getString("url"))) {
				visual = visualObject.getString("url");
			}
		} 
		if (jobject.has("alternate")) {
			JSONArray canonicalArray = jobject.getJSONArray("alternate");
			if (canonicalArray.length() > 0) {
				JSONObject canObject = (JSONObject) canonicalArray.get(0);
				url = canObject.getString("href");	
			}
		}
		if (jobject.has("content")) {
			JSONObject contObject = jobject.getJSONObject("content");
			content = contObject.getString("content");			
		}
		// if no content, summary is used
		if (content == null) {
			JSONObject summObject = jobject.getJSONObject("summary");
			content = summObject.getString("content");				
		}
	}
	
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public boolean isUnread() {
		return unread;
	}
	public void setUnread(boolean unread) {
		this.unread = unread;
	}
	public List<Category> getCategories() {
		return categories;
	}
	public void setCategories(List<Category> categories) {
		this.categories = categories;
	}
	public Date getPublished() {
		return published;
	}
	public void setPublished(Date published) {
		this.published = published;
	}
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}
	public String getContent() {
		return content;
	}
	public void setContent(String content) {
		this.content = content;
	}
	public String getVisual() {
		return visual;
	}
	public void setVisual(String visual) {
		this.visual = visual;
	}
	public String getOriginTitle() {
		return originTitle;
	}
	public void setOriginTitle(String originTitle) {
		this.originTitle = originTitle;
	}
	public String getOriginId() {
		return originId;
	}
	public void setOriginId(String originId) {
		this.originId = originId;
	}
	public Date getCrawled() {
		return crawled;
	}
	public void setCrawled(Date crawled) {
		this.crawled = crawled;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	
}
