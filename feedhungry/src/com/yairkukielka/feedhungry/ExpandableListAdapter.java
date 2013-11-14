package com.yairkukielka.feedhungry;

import java.util.HashMap;
import java.util.List;

import android.content.Context;
import android.graphics.Typeface;
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.BaseExpandableListAdapter;
import android.widget.TextView;

import com.yairkukielka.feedhungry.feedly.Category;
import com.yairkukielka.feedhungry.feedly.Subscription;

public class ExpandableListAdapter extends BaseExpandableListAdapter {

	private Context _context;
	private List<Category> _listDataHeader; // header titles
	// child data in format of header title, child title
	private HashMap<String, List<Subscription>> _listDataChild;
	private DisplayMetrics _metrics;
	
	public ExpandableListAdapter(Context context, List<Category> listDataHeader,
			HashMap<String, List<Subscription>> listChildData, DisplayMetrics metrics) {
		this._context = context;
		this._listDataHeader = listDataHeader;
		this._listDataChild = listChildData;
		this._metrics = metrics;
	}

	@Override
	public Object getChild(int groupPosition, int childPosititon) {
		return this._listDataChild.get(this._listDataHeader.get(groupPosition).getLabel()).get(childPosititon);
	}

	@Override
	public long getChildId(int groupPosition, int childPosition) {
		return childPosition;
	}

	@Override
	public View getChildView(int groupPosition, final int childPosition,
			boolean isLastChild, View convertView, ViewGroup parent) {

		final Subscription sub = (Subscription) getChild(groupPosition, childPosition);
		final String childText = sub.getTitle();
		final Integer childUnread = sub.getUnread();

		if (convertView == null) {
			LayoutInflater infalInflater = (LayoutInflater) this._context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = infalInflater.inflate(R.layout.drawer_list_item, null);
		}

		// set subscription title
		TextView txtListChild = (TextView) convertView
				.findViewById(R.id.drawer_list_item_text);
		txtListChild.setText(childText);

		// set unread items for this subscription
		TextView txtUnreadListChild = (TextView) convertView
				.findViewById(R.id.drawer_list_item_unread);
		txtUnreadListChild.setText(childUnread.toString());
		
		Animation animation = AnimationUtils.loadAnimation(_context, R.anim.fade_in);
		// animation = AnimationUtils.loadAnimation(context, R.anim.push_up_in);
		animation.setDuration(500);
		convertView.startAnimation(animation);
		animation = null;
		   
		return convertView;
	}

	@Override
	public int getChildrenCount(int groupPosition) {
		return this._listDataChild.get(this._listDataHeader.get(groupPosition).getLabel()).size();
	}

	@Override
	public Object getGroup(int groupPosition) {
		return this._listDataHeader.get(groupPosition);
	}

	@Override
	public int getGroupCount() {
		return this._listDataHeader.size();
	}

	@Override
	public long getGroupId(int groupPosition) {
		return groupPosition;
	}

	@Override
	public View getGroupView(int groupPosition, boolean isExpanded,
			View convertView, ViewGroup parent) {
		String headerTitle = ((Category) getGroup(groupPosition)).getLabel();
		if (convertView == null) {
			LayoutInflater infalInflater = (LayoutInflater) this._context
					.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			convertView = infalInflater.inflate(R.layout.drawer_group_item, null);
		}

		// set category title
		TextView lblListHeader = (TextView) convertView
				.findViewById(R.id.group_item_text);
		lblListHeader.setTypeface(null, Typeface.BOLD);
		lblListHeader.setText(headerTitle);

		// set category unread
		Integer unread = 0;
		for (Subscription sub :_listDataChild.get(headerTitle)) {
			unread = unread + sub.getUnread();
		}
		TextView lblListUnread = (TextView) convertView
				.findViewById(R.id.group_item_unread);
		lblListUnread.setTypeface(null, Typeface.BOLD);
		lblListUnread.setText(unread.toString());
		
		return convertView;
	}

	@Override
	public boolean hasStableIds() {
		return false;
	}

	@Override
	public boolean isChildSelectable(int groupPosition, int childPosition) {
		return true;
	}
}
