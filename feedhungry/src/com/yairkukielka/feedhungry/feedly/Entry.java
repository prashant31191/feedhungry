package com.yairkukielka.feedhungry.feedly;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;

import com.yairkukielka.feedhungry.R;
import com.yairkukielka.feedhungry.toolbox.DateUtils;

public class Entry {
	private String id;
	private String originId;
	private String originTitle = "";
	private String title;
	private boolean unread;
	private List<Category> categories = new ArrayList<Category>();
	private List<Tag> tags = new ArrayList<Tag>();
	private Date published;
	private Date crawled;
	private String author;
	private String content;
	private String visual;
	private String streamTitle;
	private String url;
	private boolean saved;

	private static final String SAVED_SUFFIX = "global.saved";
	private static final String TAGS = "tags";
	private static final String CATEGORIES = "categories";
	private static final String AUTHOR = "author";
	private static final String ID = "id";
	private static final String TITLE = "title";
	private static final String SUMMARY = "summary";
	private static final String UNREAD = "unread";
	private static final String PUBLISHED = "published";
	private static final String VISUAL = "visual";
	private static final String CONTENT = "content";
	private static final String URL = "url";
	private static final String ORIGIN = "origin";
	private static final String ORIGIN_ID = "originId";
	private static final String CRAWLED = "crawled";
	private static final String ALTERNATE = "alternate";
	private static final String HREF = "href";
	
	public Entry() {
	}
	public Entry(JSONObject jobject, Context context) throws JSONException {
		id = jobject.getString(ID);
		if (jobject.has(ORIGIN)) {
			JSONObject originObject = jobject.getJSONObject(ORIGIN);
			if (originObject.has(TITLE)) {
				originTitle = originObject.getString(TITLE);
			} else {
				originTitle = "";
			}
		}
		if (jobject.has(ORIGIN_ID)) {
			originId = jobject.getString(ORIGIN_ID);	
		}
		title = jobject.getString(TITLE);	
		unread = jobject.getBoolean(UNREAD);
		if (jobject.has(CATEGORIES)) {
			JSONArray jsonCategories = jobject.getJSONArray(CATEGORIES);
			for (int i = 0; i < jsonCategories.length(); i++) {
				Category c = new Category((JSONObject) jsonCategories.get(i));	
				categories.add(c);
			}
		}
		if (jobject.has(TAGS)) {
			JSONArray jsonTags = jobject.getJSONArray(TAGS);
			for (int i = 0; i < jsonTags.length(); i++) {
				Tag t = new Tag((JSONObject) jsonTags.get(i));	
				tags.add(t);
				if (t.getId().endsWith(SAVED_SUFFIX)) {
					saved = true;
				}
			}
		}
		if (jobject.has(PUBLISHED)) {
			published = DateUtils.getDateFromJson(jobject.getLong(PUBLISHED));	
		}
		if (jobject.has(CRAWLED)) {
			crawled = DateUtils.getDateFromJson(jobject.getLong(CRAWLED));	
		}
		if (jobject.has(AUTHOR)) {
			author = jobject.getString(AUTHOR);
		}	
		if (jobject.has(VISUAL)) {
			JSONObject visualObject = jobject.getJSONObject(VISUAL);
			// if no image, visual = "none"
			if (!"none".equals(visualObject.getString(URL))) {
				visual = visualObject.getString(URL);
			}
		} 
		if (jobject.has(ALTERNATE)) {
			JSONArray canonicalArray = jobject.getJSONArray(ALTERNATE);
			if (canonicalArray.length() > 0) {
				JSONObject canObject = (JSONObject) canonicalArray.get(0);
				url = canObject.getString(HREF);	
			}
		}
		if (jobject.has(CONTENT)) {
			JSONObject contObject = jobject.getJSONObject(CONTENT);
			content = contObject.getString(CONTENT);			
		}
		// if no content, summary is used
		if (content == null) {
			if (jobject.has(SUMMARY)) {
				JSONObject summObject = jobject.getJSONObject(SUMMARY);
				content = summObject.getString(CONTENT);				
			} else {
				content = context.getResources().getString(R.string.entry_without_content);
			}
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

	public List<Tag> getTags() {
		return tags;
	}
	public void setTags(List<Tag> tags) {
		this.tags = tags;
	}

	public boolean isSaved() {
		return saved;
	}
	public void setSaved(boolean saved) {
		this.saved = saved;
	}
	public String getStreamTitle() {
		return streamTitle;
	}
	public void setStreamTitle(String streamTitle) {
		this.streamTitle = streamTitle;
	}
}
