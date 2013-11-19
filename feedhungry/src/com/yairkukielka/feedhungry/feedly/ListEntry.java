package com.yairkukielka.feedhungry.feedly;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.yairkukielka.feedhungry.toolbox.DateUtils;

public class ListEntry {
	private String id;
	private String title;
	private boolean unread;
	private List<Category> categories = new ArrayList<Category>();
	// TODO private List<Category> tags;
	private Date published;
	private Date updated;
	private String author;
	private String content;
	private String visual;
	
	public ListEntry() {
	}
	public ListEntry(JSONObject jobject) throws JSONException {
		id = jobject.getString("id");
		title = jobject.getString("title");	
		unread = jobject.getBoolean("unread");
		if (jobject.has("categories")) {
			JSONArray jsonCategories = jobject.getJSONArray("categories");
			for (int i = 0; i < jsonCategories.length(); i++) {
				Category c = new Category((JSONObject) jsonCategories.get(i));	
				categories.add(c);
			}
		}
		if (jobject.has("published")) {
			published = DateUtils.getDateFromJson(jobject.getLong("published"));	
		}
		if (jobject.has("updated")) {
			updated = DateUtils.getDateFromJson(jobject.getLong("updated"));	
		}
		if (jobject.has("visual")) {
			JSONObject visualObject = jobject.getJSONObject("visual");
			// if no image, visual = "none"
			if (!"none".equals(visualObject.getString("url"))) {
				visual = visualObject.getString("url");
			}
		} 
		if (jobject.has("summary")) {
			JSONObject sumObject = jobject.getJSONObject("summary");
			content = sumObject.getString("content");
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
	public Date getUpdated() {
		return updated;
	}
	public void setUpdated(Date updated) {
		this.updated = updated;
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
	
}
