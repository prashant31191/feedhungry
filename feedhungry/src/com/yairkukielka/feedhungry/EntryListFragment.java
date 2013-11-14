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

import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;

import com.actionbarsherlock.app.SherlockFragment;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.FragmentArg;
import com.googlecode.androidannotations.annotations.ViewById;
import com.yairkukielka.feedhungry.app.MyVolley;
import com.yairkukielka.feedhungry.feedly.ListEntry;
import com.yairkukielka.feedhungry.feedly.Stream;
import com.yairkukielka.feedhungry.settings.PreferencesActivity;
import com.yairkukielka.feedhungry.toolbox.NetworkUtils;

/**
 * Demonstrates: 1. ListView which is populated by HTTP paginated requests; 2.
 * Usage of NetworkImageView; 3. "Endless" ListView pagination with read-ahead
 * 
 * Please note that for production environment you will need to add
 * functionality like handling rotation, showing/hiding (indeterminate) progress
 * indicator while loading, indicating that there are no more records, etc...
 * 
 * @author Ognyan Bankov (ognyan.bankov@bulpros.com)
 * 
 */
@EFragment(R.layout.feed_list_view)
public class EntryListFragment extends SherlockFragment {
	private static final String STREAM_PATH = "/v3/streams/contents?streamId=";
	public static final String STREAM_ID = "stream_id";
	public static final String ACCESS_TOKEN = "accessToken";
	private static final String ENTRY_ID = "entryId";
	public static final String RESULTS_PAGE_SIZE = "RESULTS_PAGE_SIZE";
	private static final String COUNT_PARAM = "&count=";
	private static final String CONTINUATION_PARAM = "&continuation=";
	private static final String ONLY_UNREAD_PARAM = "&unreadOnly=";
	public String continuation = null;
	private DisplayMetrics metrics;
	private Stream stream;

	@ViewById(R.id.lv_picasa)
	ListView mLvPicasa;
	boolean mHasData = false;
	boolean mInError = false;
	ArrayList<ListEntry> mEntries = new ArrayList<ListEntry>();
	ListViewEntryArrayAdapter mAdapter;

	@FragmentArg(STREAM_ID)
	String streamId;
	@FragmentArg(ACCESS_TOKEN)
	String accessToken;

	@AfterViews
	void afterViews() {
		metrics = new DisplayMetrics();
		getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);

		mAdapter = new ListViewEntryArrayAdapter(getActivity(), 0, mEntries, MyVolley.getImageLoader());
		mLvPicasa.setAdapter(mAdapter);
		mLvPicasa.setOnScrollListener(new EndlessScrollListener());

		mLvPicasa.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Intent intent = new Intent(EntryListFragment.this.getActivity(), FeedEntryActivity_.class);
				ListEntry e = mEntries.get(position);
				Bundle b = new Bundle();
				b.putString(ACCESS_TOKEN, accessToken);
				b.putString(ENTRY_ID, e.getId());
				intent.putExtras(b);
				startActivity(intent);
			}
		});

		if (!mHasData && !mInError) {
			loadPage();
		}
	}

	private void loadPage() {
		RequestQueue queue = MyVolley.getRequestQueue();
		JsonObjectRequest myReq = NetworkUtils.getJsonObjectRequest(MainActivity.ROOT_URL + STREAM_PATH + streamId
				+ getPageSizeParameter() + getContinuationParameter() + getOnlyUnreadParameter(),
				createMyReqSuccessListener(), createMyReqErrorListener(), accessToken);
		queue.add(myReq);
	}

	private Response.Listener<JSONObject> createMyReqSuccessListener() {
		return new Response.Listener<JSONObject>() {
			@Override
			public void onResponse(JSONObject response) {
				try {
					stream = new Stream();
					stream.setId(response.getString("id"));
					if (response.has("title")) {
						// global.all has no title
						stream.setTitle(response.getString("title"));
					}
					if (response.has("summary")) {
						// global.all has no title
						JSONObject summary = response.getJSONObject("summary");
						stream.setSummary(summary.getString("content"));
					}
					if (response.has("continuation")) {
						continuation = response.getString("continuation");
					}
					JSONArray items = response.getJSONArray("items");
					for (int i = 0; i < items.length(); i++) {
						ListEntry e = new ListEntry((JSONObject) items.get(i));
						stream.add(e);
						mEntries.add(e);
					}
					mAdapter.notifyDataSetChanged();
				} catch (JSONException e) {
					showErrorDialog(e.getMessage());
				}
			}
		};
	}

	private Response.ErrorListener createMyReqErrorListener() {
		return new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				showErrorDialog(error.getMessage());
			}
		};
	}

	private void showErrorDialog(String error) {
		mInError = true;
		AlertDialog.Builder b = new AlertDialog.Builder(getActivity());
		b.setMessage("Error occured: " + error);
		b.show();
	}

	/**
	 * Used to page through the contents in the feedly api
	 * 
	 * @return parameter continuation if necessary (it's not necessary the first
	 *         load)
	 */
	private String getContinuationParameter() {
		if (continuation != null) {
			return CONTINUATION_PARAM + continuation;
		}
		return "";
	}

	/**
	 * Used to page through the contents in the feedly api
	 * 
	 * @return parameter continuation if necessary (it's not necessary the first
	 *         load)
	 */
	private String getOnlyUnreadParameter() {
		if (getPrefOnlyUnread(getActivity())) {
			return ONLY_UNREAD_PARAM + true;
		}
		return "";
	}

	private String getPageSizeParameter() {
		Integer pageSize = Integer.parseInt(getPrefPageSize(getActivity()));
		return COUNT_PARAM + pageSize;
	}

	/**
	 * Get only unread articles setting from preferences
	 * 
	 * @param context
	 *            context
	 * @return true if only unread articles
	 */
	public static Boolean getPrefOnlyUnread(Context context) {
		if (context != null) {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
			return sharedPref.getBoolean(PreferencesActivity.KEY_ONLY_UNREAD, false);
		}
		return null;
	}

	public static String getPrefPageSize(Context context) {
		if (context != null) {
			SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
			return sharedPref.getString(PreferencesActivity.KEY_PAGE_SIZE, "30");
		}
		return null;
	}
	/**
	 * Detects when user is close to the end of the current page and starts
	 * loading the next page so the user will not have to wait (that much) for
	 * the next entries.
	 * 
	 * @author Ognyan Bankov (ognyan.bankov@bulpros.com)
	 */
	public class EndlessScrollListener implements OnScrollListener {
		// how many entries earlier to start loading next page
		private int visibleThreshold = 5;
		private int currentPage = 0;
		private int previousTotal = 0;
		private boolean loading = true;
		Fragment loadingFragment = new LoadingFragment_();

		public EndlessScrollListener() {
		}

		public EndlessScrollListener(int visibleThreshold) {
			this.visibleThreshold = visibleThreshold;
		}

		@Override
		public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
			if (loading) {
				if (totalItemCount > previousTotal) {
					loading = false;

					FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
					fragmentManager.beginTransaction().remove(loadingFragment).commit();

					previousTotal = totalItemCount;
					currentPage++;
				}
			}
			if (!loading && (totalItemCount - visibleItemCount) <= (firstVisibleItem + visibleThreshold)) {
				// I load the next page of gigs using a background task,
				// but you can call any function here.
				loadPage();
				loading = true;
				FragmentManager fragmentManager = getActivity().getSupportFragmentManager();
				fragmentManager.beginTransaction().add(R.id.content_frame, loadingFragment).commit();
			}
		}

		@Override
		public void onScrollStateChanged(AbsListView view, int scrollState) {

		}

		public int getCurrentPage() {
			return currentPage;
		}
	}
}
