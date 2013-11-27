package com.yairkukielka.feedhungry.feedly;

import org.json.JSONException;
import org.json.JSONObject;

public class Tag {
	private static final String ID = "id";
	private static final String LABEL = "label";
	private String id;
	private String label;
	
	public Tag() {
	}
	public Tag(JSONObject jobject) throws JSONException {
		id = jobject.getString(ID);
		label = jobject.getString(LABEL);		
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	@Override
	public String toString() {
		return "Category [id=" + id + ", label=" + label + "]";
	}
	
}
