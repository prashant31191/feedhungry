package com.yairkukielka.feedhungry.feedly;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import com.yairkukielka.feedhungry.toolbox.DateUtils;

public class ListEntry {
	private String id;
	private String title;
	private boolean unread;
	private String originTitle = "";
	private List<Category> categories = new ArrayList<Category>();
	private List<Tag> tags = new ArrayList<Tag>();
	private Date published;
	private Date updated;
	private String author;
	private String content;
	private String visual;
	private String streamTitle;
	private String engagement;
	private boolean popular;
	private boolean saved;

	private static final String SAVED_SUFFIX = "global.saved";
	private static final String TAGS = "tags";
	private static final String CATEGORIES = "categories";
	private static final String ENGAGEMENT = "engagement";
	private static final String AUTHOR = "author";
	private static final String ID = "id";
	private static final String TITLE = "title";
	private static final String SUMMARY = "summary";
	private static final String UNREAD = "unread";
	private static final String PUBLISHED = "published";
	private static final String UPDATED = "updated";
	private static final String VISUAL = "visual";
	private static final String CONTENT = "content";
	private static final String URL = "url";
	private static final String ORIGIN = "origin";

	public ListEntry() {
	}

	public ListEntry(JSONObject jobject) throws JSONException {
		id = jobject.getString(ID);
		title = jobject.getString(TITLE);
		unread = jobject.getBoolean(UNREAD);
		if (jobject.has(ORIGIN)) {
			JSONObject originObject = jobject.getJSONObject(ORIGIN);
			if (!originObject.has(TITLE)) {
				originTitle = "";
			} else {
				originTitle = originObject.getString(TITLE);
			}
		}
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
		if (jobject.has(ENGAGEMENT)) {
			engagement = jobject.getString(ENGAGEMENT);
			try {
				Integer engInteger = Integer.valueOf(engagement);
				if (engInteger > 18) {
					popular = true;
				}
			} catch (NumberFormatException e) {
			}
		}
		if (jobject.has(AUTHOR)) {
			author = jobject.getString(AUTHOR);
		}
		if (jobject.has(PUBLISHED)) {
			published = DateUtils.getDateFromJson(jobject.getLong(PUBLISHED));
		}
		if (jobject.has(UPDATED)) {
			updated = DateUtils.getDateFromJson(jobject.getLong(UPDATED));
		}
		if (jobject.has(VISUAL)) {
			JSONObject visualObject = jobject.getJSONObject(VISUAL);
			// if no image, visual = "none"
			if (!"none".equals(visualObject.getString(URL))) {
				visual = visualObject.getString(URL);
			}
		}
		if (jobject.has(SUMMARY)) {
			JSONObject sumObject = jobject.getJSONObject(SUMMARY);
			content = sumObject.getString(CONTENT);
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

	public String getEngagement() {
		return engagement;
	}

	public void setEngagement(String engagement) {
		this.engagement = engagement;
	}

	public boolean isPopular() {
		return popular;
	}

	public void setPopular(boolean popular) {
		this.popular = popular;
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

	public String getOriginTitle() {
		return originTitle;
	}

	public void setOriginTitle(String originTitle) {
		this.originTitle = originTitle;
	}

}
