package com.yairkukielka.feedhungry.feedly;

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Subscription {
	private String id;
	private String title;
	private ArrayList<Category> categories = new ArrayList<Category>();
	private String sortid;
	private String updated;
	private String website;
	private int unread;
	
	public Subscription() {
	}
	public Subscription(JSONObject jobject) throws JSONException {
		id = jobject.getString("id");
		title = jobject.getString("title");
		JSONArray jsonCategories = jobject.getJSONArray("categories");
		for (int i = 0; i < jsonCategories.length(); i++) {
			Category c = new Category((JSONObject) jsonCategories.get(i));	
			categories.add(c);
		}
		if (jobject.has("sortid")) {
			sortid = jobject.getString("sortid");
		}
		if (jobject.has("updated")) {
			updated = jobject.getString("updated");
		}
		if (jobject.has("website")) {
			website = jobject.getString("website");
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
	public ArrayList<Category> getCategories() {
		return categories;
	}
	public void setCategories(ArrayList<Category> categories) {
		this.categories = categories;
	}
	public String getSortid() {
		return sortid;
	}
	public void setSortid(String sortid) {
		this.sortid = sortid;
	}
	public String getUpdated() {
		return updated;
	}
	public void setUpdated(String updated) {
		this.updated = updated;
	}
	public String getWebsite() {
		return website;
	}
	public void setWebsite(String website) {
		this.website = website;
	}
	public int getUnread() {
		return unread;
	}
	public void setUnread(int unread) {
		this.unread = unread;
	}
	@Override
	public String toString() {
		return "Subscription [id=" + id + ", title=" + title + "]";
	}
	
}
