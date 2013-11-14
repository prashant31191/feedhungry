package com.yairkukielka.feedhungry.feedly;

import org.json.JSONException;
import org.json.JSONObject;

public class Category {
	private String id;
	private String label;
	
	public Category() {
	}
	public Category(JSONObject jobject) throws JSONException {
		id = jobject.getString("id");
		label = jobject.getString("label");		
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
