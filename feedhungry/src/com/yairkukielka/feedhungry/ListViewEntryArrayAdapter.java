/**
 * Copyright 2013 Ognyan Bankov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.yairkukielka.feedhungry;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.yairkukielka.feedhungry.app.MyVolley;
import com.yairkukielka.feedhungry.feedly.ListEntry;
import com.yairkukielka.feedhungry.settings.PreferencesActivity;
import com.yairkukielka.feedhungry.toolbox.DateUtils;
import com.yairkukielka.feedhungry.toolbox.NetworkUtils;

public class ListViewEntryArrayAdapter extends ArrayAdapter<ListEntry> {
	private static final String TAG = ListViewEntryArrayAdapter.class.getSimpleName();
	private static final String ACTION = "action";
	private static final String TYPE = "type";
	private static final String ENTRIES = "entries";
	private static final String ENTRIES_IDS = "entryIds";
	private static final String HTML_OPEN_MARK = "<";
	private static final String MARK_AS_READ = "markAsRead";
	private static final String EMPTY_STRING = "";
	private static final String MARK_AS_UNREAD = "keepUnread";
	private ImageLoader mImageLoader;
	private boolean cards;
	private Activity context;
	Animation animation;

	public ListViewEntryArrayAdapter(Activity context, int textViewResourceId, List<ListEntry> objects,
			ImageLoader imageLoader) {
		super(context, textViewResourceId, objects);
		mImageLoader = imageLoader;
		this.context = context;
		animation = AnimationUtils.loadAnimation(context, R.anim.wave_scale);
		this.cards = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(
				PreferencesActivity.KEY_LIST_WITH_CARDS, false);
	}

	@Override
	public View getView(int position, View convertView, ViewGroup parent) {
		View v = convertView;
		if (v == null) {
			LayoutInflater vi = (LayoutInflater) this.getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
			if (cards) {
				// cards style
				v = vi.inflate(R.layout.feed_list_row_big, null);
			} else {
				v = vi.inflate(R.layout.feed_list_row, null);
			}
		}

		ViewHolder holder = (ViewHolder) v.getTag(R.id.id_holder);

		if (holder == null) {
			holder = new ViewHolder(v);
			v.setTag(R.id.id_holder, holder);
		}

		ListEntry entry = getItem(position);
		if (entry.getVisual() != null) {
			holder.image.setImageUrl(entry.getVisual(), mImageLoader);
		} else {
			holder.image.setLayoutParams(new RelativeLayout.LayoutParams(0, 0));
		}

		String summary = getSummaryWithoutHTML(entry.getContent());
		// Spanned summary = Html.fromHtml(entry.getContent());
		// SpannableStringBuilder spanstr = new
		// SpannableStringBuilder(entry.getTitle());
		// spanstr.setSpan(new StyleSpan(Typeface.BOLD),0,
		// entry.getTitle().length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
		// spanstr.append(". ");
		// spanstr.append(summary);
		// holder.title.setText(spanstr);

		if (entry.isPopular()) {
			holder.popular.setVisibility(View.VISIBLE);
		} else {
			holder.popular.setVisibility(View.INVISIBLE);
		}

		holder.title.setText(entry.getTitle());
		if (EMPTY_STRING.equals(summary)) {
			holder.summary.setVisibility(View.GONE);
		} else {
			holder.summary.setText(summary);
		}
		holder.date.setText(DateUtils.dateToString(entry.getPublished()));

		holder.checkRead.setOnCheckedChangeListener(null);
		if (!entry.isUnread()) {
			holder.checkRead.setChecked(true);
		} else {
			holder.checkRead.setChecked(false);
		}
		holder.checkRead.setOnCheckedChangeListener(new MyCheckReadListener(entry));
		holder.streamName.setText(entry.getOriginTitle());

		holder.saved.setOnClickListener(null);
		if (entry.isSaved()) {
			holder.saved.setImageResource(R.drawable.star_on);
			holder.saved.setTag(R.drawable.star_on);
		} else {
			holder.saved.setImageResource(R.drawable.star_off);
			holder.saved.setTag(R.drawable.star_off);
		}
		holder.saved.setOnClickListener(new MySaveListener(entry));
		if (v != null) {
			animation.setDuration(400);
			v.startAnimation(animation);
		}
		return v;
	}

	private class ViewHolder {
		NetworkImageView image;
		TextView title;
		TextView summary;
		TextView date;
		TextView popular;
		TextView streamName;
		CheckBox checkRead;
		ImageView saved;

		public ViewHolder(View v) {
			image = (NetworkImageView) v.findViewById(R.id.image_list_thumb);
			title = (TextView) v.findViewById(R.id.tv_list_title);
			summary = (TextView) v.findViewById(R.id.tv_list_summary);
			date = (TextView) v.findViewById(R.id.tv_list_date);
			popular = (TextView) v.findViewById(R.id.tv_list_popular);
			streamName = (TextView) v.findViewById(R.id.tv_list_stream_name);
			checkRead = (CheckBox) v.findViewById(R.id.check_list_read);
			saved = (ImageView) v.findViewById(R.id.image_list_saved);
			v.setTag(this);
		}
	}

	private String getSummaryWithoutHTML(String s) {
		if (!(s == null || EMPTY_STRING.equals(s) || s.startsWith(HTML_OPEN_MARK))) {
			int index = s.indexOf(HTML_OPEN_MARK);
			if (index != -1) {
				return s.substring(0, index);
			}
		}
		return EMPTY_STRING;
	}

	/************ CHECKBOX LISTENER *******************/

	/**
	 * Listener for the read/unread checkbox
	 */
	public class MyCheckReadListener implements OnCheckedChangeListener {
		ListEntry entry;

		public MyCheckReadListener(ListEntry entry) {
			this.entry = entry;
		}

		@Override
		public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
			if (isChecked) {
				markEntryAs(MARK_AS_READ, entry, context.getResources().getString(R.string.marked_as_read));
			} else {
				markEntryAs(MARK_AS_UNREAD, entry, context.getResources().getString(R.string.marked_as_unread));
			}
		}
	}

	private void markEntryAs(String mark, ListEntry entry, String successMessage) {
		RequestQueue queue = MyVolley.getRequestQueue();
		try {
			JSONObject jsonRequest = new JSONObject();
			jsonRequest.put(ACTION, mark);
			jsonRequest.put(TYPE, ENTRIES);
			JSONArray entries = new JSONArray();
			entries.put(entry.getId());
			jsonRequest.put(ENTRIES_IDS, entries);
			String accessToken = context.getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE)
					.getString(MainActivity.SHPREF_KEY_ACCESS_TOKEN, null);
			JsonObjectRequest myReq = NetworkUtils.getJsonPostRequest(
					MainActivity.ROOT_URL + MainActivity.MARKERS_PATH, jsonRequest,
					getActionListenerEntrySuccessListener(successMessage), createMyReqErrorListener(), accessToken);
			queue.add(myReq);
		} catch (JSONException uex) {
			uex.printStackTrace();
			Log.e(TAG, "JSONException marking as read/unread");
		}
	}

	private Response.Listener<JSONObject> getActionListenerEntrySuccessListener(final String successMessage) {
		return new Response.Listener<JSONObject>() {
			@Override
			public void onResponse(JSONObject response) {
				Toast.makeText(context, successMessage, Toast.LENGTH_SHORT).show();
			}
		};
	}

	private Response.ErrorListener createMyReqErrorListener() {
		return new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				Log.e(TAG, "Error marking entry in stream list" + error.getMessage());
				// showErrorDialog(error.getMessage());
			}
		};
	}

	/************ SAVE LISTENER *******************/

	/**
	 * Listener for the read/unread checkbox
	 */
	public class MySaveListener implements OnClickListener {
		ListEntry entry;

		public MySaveListener(ListEntry entry) {
			this.entry = entry;
		}

		@Override
		public void onClick(View v) {
			ImageView iView = (ImageView) v;
			int tag = ((Integer) iView.getTag()).intValue();
			if (tag == R.drawable.star_off) {
				iView.setImageResource(R.drawable.star_on);
				iView.setTag(R.drawable.star_on);
				saveOrUnsaveEntry(Method.PUT, entry, context.getResources().getString(R.string.saved_article));
			} else {
				iView.setImageResource(R.drawable.star_off);
				iView.setTag(R.drawable.star_off);
				saveOrUnsaveEntry(Method.DELETE, entry, context.getResources().getString(R.string.unsaved_article));
			}
		}

	}

	private void saveOrUnsaveEntry(int method, ListEntry entry, String successMessage) {
		RequestQueue queue = MyVolley.getRequestQueue();
		try {
			JSONObject jsonRequest = new JSONObject();
			JSONArray entries = new JSONArray();
			entries.put(entry.getId());
			jsonRequest.put(ENTRIES_IDS, entries);
			SharedPreferences sprPreferences = context.getSharedPreferences(MainActivity.APP_PREFERENCES,
					Context.MODE_PRIVATE);
			String accessToken = sprPreferences.getString(MainActivity.SHPREF_KEY_ACCESS_TOKEN, null);
			String userId = sprPreferences.getString(MainActivity.SHPREF_KEY_USERID_TOKEN, null);
			if (accessToken != null && userId != null) {
				String userIdEncoded = URLEncoder.encode("/" + userId.toString() + "/tag/", MainActivity.UTF_8);
				StringBuilder url = new StringBuilder();
				url.append(MainActivity.ROOT_URL).append(MainActivity.TAGS_PATH).append("/user").append(userIdEncoded)
						.append("global.saved");
				JsonObjectRequest myReq = NetworkUtils.getJsonRequestWithMethod(method, url.toString(), jsonRequest,
						getActionListenerEntrySuccessListener(successMessage), createMyReqErrorListener(), accessToken);
				queue.add(myReq);
			}
		} catch (JSONException uex) {
			Log.e(TAG, "JSONException marking as read/unread");
		} catch (UnsupportedEncodingException uex) {
			Log.e(TAG, "Error encoding URL when saving/unsaving");
		}
	}

}
