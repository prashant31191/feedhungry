package com.yairkukielka.feedhungry.feedly;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Stream {
	private String id;
	private String title;
	private String summary;
	private List<ListEntry> entries = new ArrayList<ListEntry>();
	
	public Stream() {
	}
	public Stream(JSONObject jobject) throws JSONException {
		id = jobject.getString("id");
		title = jobject.getString("title");		

		JSONArray jsonEntries = jobject.getJSONArray("entries");
		for (int i = 0; i < jsonEntries.length(); i++) {
			ListEntry e = new ListEntry((JSONObject) jsonEntries.get(i));	
			entries.add(e);
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
	public List<ListEntry> getEntries() {
		return entries;
	}
	public void setEntries(List<ListEntry> entries) {
		this.entries = entries;
	}

	public String getSummary() {
		return summary;
	}
	public void setSummary(String summary) {
		this.summary = summary;
	}
	public void add(ListEntry e) {
		entries.add(e);
	}
}
