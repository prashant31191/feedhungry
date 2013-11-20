package com.yairkukielka.feedhungry;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Field;
import java.net.URLEncoder;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.R.color;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.NetworkImageView;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.Extra;
import com.googlecode.androidannotations.annotations.ViewById;
import com.yairkukielka.feedhungry.app.MyVolley;
import com.yairkukielka.feedhungry.feedly.Entry;
import com.yairkukielka.feedhungry.toolbox.NetworkUtils;

@EActivity(R.layout.feed_entry_layout)
public class FeedEntryActivity extends SherlockFragmentActivity {
	private static final String TAG = FeedEntryActivity.class.getSimpleName();
	private static final String ENTRY_PATH = "/v3/entries/";
	private static final String MARKERS_PATH = "/v3/markers";
	private static final String MARK_READ = "markAsRead";
	private static final String MARK_KEEP_UNREAD = "keepUnread";
	private static final String ENTRY_ID = "entryId";
	private static final String ENTRY_TITLE = "entryTitle";
	private static final String ENTRY_CONTENT = "entryContent";
	private static final String ENTRY_AUTHOR = "entryAuthor";
	private static final String ENTRY_DATE = "entryDate";
	public static final String ACCESS_TOKEN = "accessToken";
	private static final String STREAM_TITLE = "streamTitle";
	private static final String BY = " by ";
	private static final String encoding = "utf-8";
	private static final int TRANSPARENT_WINDOW = 140;
	// private static final String HTML_HEAD =
	// "<head><style>@font-face {font-family: 'myFont';src: url('file:///android_asset/fonts/MyFont.otf');}body {font-family: 'myFont';}</style></head>";
	private static final String DIV_PREFIX = "<div style='background-color:transparent;padding: 10px;color:#ccc;font-family: myFont'>";
	private static final String DIV_SUFIX = "</div>";
	// the feed entry
	private Entry entry;
	// animation to show the feed content after the loading fragment shows
	private Animation webViewAnimation;
	// animation to show the title fading in
	private Animation titleFadeInAnimation;
	// fragment that shows while loading the entry content
	private Fragment loadingFragment;
	@Extra(ACCESS_TOKEN)
	String accessToken;
	@Extra(ENTRY_ID)
	String entryId;
	@Extra(ENTRY_CONTENT)
	String entryContent;
	@Extra(ENTRY_AUTHOR)
	String entryAuthor;
	@Extra(ENTRY_DATE)
	String entryDate;
	@Extra(ENTRY_TITLE)
	String entryTitle;
	@Extra(STREAM_TITLE)
	String streamTitle;

	/** title text view */
	@ViewById(R.id.entry_title)
	TextView tvTitle;
	/** date text view */
	@ViewById(R.id.entry_date)
	TextView tvDate;
	/** author text view */
	@ViewById(R.id.entry_author)
	TextView tvAuthor;
	@ViewById(R.id.entry_webview)
	WebView webView;
	@ViewById(R.id.transparent_view)
	View transparentView;

	@ViewById(R.id.feed_entry_title_layout)
	RelativeLayout titleLayout;
	@ViewById(R.id.entry_bg_image_view)
	NetworkImageView bgImage;

	@SuppressLint("SetJavaScriptEnabled")
	@AfterViews
	void afterViews() {

		getSupportActionBar().setBackgroundDrawable(null);
		webView.getSettings().setJavaScriptEnabled(true);
		View.OnClickListener onClickListener = getOnClickListener();
		titleLayout.setOnClickListener(onClickListener);
		// tvTitle.setOnClickListener(onClickListener);
		// action bar icon navagable up
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		// animate webview content
		webViewAnimation = AnimationUtils.loadAnimation(this, R.anim.push_up_in);
		titleFadeInAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in_title);
		loadingFragment = new LoadingFragment_();
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction().replace(R.id.frame_webview, loadingFragment).commitAllowingStateLoss();

		if (streamTitle != null) {
			setTitle(streamTitle);
		}
		tvTitle.setText(entryTitle);

		if (entryAuthor != null) {
			tvAuthor.setText(BY + entryAuthor);
		}
		if (entryDate != null) {
			tvDate.setText(entryDate);
		}
		titleLayout.setAnimation(titleFadeInAnimation);

		webView.getSettings().setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
		webView.setBackgroundColor(color.transparent);
		webView.getSettings().setDefaultFontSize(18);
		webView.setWebViewClient(new WebViewClient() {
			@Override
			public void onPageFinished(WebView view, String url) {
				FragmentManager fragmentManager = getSupportFragmentManager();
				if (null != fragmentManager.findFragmentById(loadingFragment.getId())) {
					// this is to avoid errors when the user clicks back before
					// this can commit (the activity would be destroyed already)
					fragmentManager.beginTransaction().remove(loadingFragment).commitAllowingStateLoss();
				}
				webView.setAnimation(webViewAnimation);
				// mark entry as read
				markEntry(MARK_READ, getMarkAsReadSuccessListener());
			}
		});

		loadPage();
		loadEntryInInnerBrowser(entryContent);
	}

	private void loadPage() {
		RequestQueue queue = MyVolley.getRequestQueue();
		try {
			JsonArrayRequest myReq = NetworkUtils.getJsonArrayRequest(
					MainActivity.ROOT_URL + ENTRY_PATH + URLEncoder.encode(entryId, encoding),
					createMyReqSuccessListener(), createMyReqErrorListener(), accessToken);
			queue.add(myReq);
		} catch (UnsupportedEncodingException uex) {
			Log.e(TAG, "Error encoding entryId URL");
		}
	}

	private String getHtmlData(String data) {
		String head = "<head><style>@font-face {font-family: 'myFont';src: url('file:///android_asset/fonts/Roboto-Light.ttf');}body {font-family: 'myFont';}a:link {color:#8AC007;}</style></head>";
		String htmlData = "<html>" + head + "<body>" + DIV_PREFIX + data + DIV_SUFIX + "</body></html>";
		return htmlData;
	}

	private Response.Listener<JSONArray> createMyReqSuccessListener() {
		return new Response.Listener<JSONArray>() {
			@Override
			public void onResponse(JSONArray response) {
				try {
					if (response.length() > 0) {
						JSONObject jentry = (JSONObject) response.get(0);
						entry = new Entry(jentry);
						if (entry.getVisual() != null) {
							bgImage.setImageUrl(entry.getVisual(), MyVolley.getImageLoader());
							bgImage.setAnimation(titleFadeInAnimation);
							transparentView.getLayoutParams().height = TRANSPARENT_WINDOW;
						}
					}
				} catch (JSONException e) {
					Log.e(TAG, "Error parsing feed entry");
					Log.e(TAG, e.getMessage());
					showErrorDialog(getResources().getString(R.string.error_loading_entry));
				}
			}
		};
	}

	/**
	 * When back is pressed in the inner browser, it goes all the way to the
	 * article. If there is no back history, it finishes the activity
	 */
	@Override
	public void onBackPressed() {
		if (webView.isFocused() && webView.canGoBack()) {
			loadEntryInInnerBrowser(entryContent);
			webView.clearHistory();
			// webView.goBack();
		} else {
			super.onBackPressed();
			finish();
			overridePendingTransition(R.anim.open_main, R.anim.close_next);
		}
	}

	/**
	 * Loads the entry in the internal browser
	 */
	private void loadEntryInInnerBrowser(String content) {
		webView.loadDataWithBaseURL("file:///android_asset/", getHtmlData(content), "text/html", "utf-8", null);
	}

	private Response.ErrorListener createMyReqErrorListener() {
		return new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				Log.e(TAG, "Error loading entry");
				Log.e(TAG, error.getMessage());
				showErrorDialog(getResources().getString(R.string.error_loading_entry));
			}
		};
	}

	private void showErrorDialog(String error) {
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setMessage(error);
		b.show();
	}

	/**
	 * Floating fragment onclick listener that shows the fragment
	 * 
	 * @return View.OnClickListener
	 */
	private View.OnClickListener getOnClickListener() {
		return new OnClickListener() {
			@Override
			public void onClick(View v) {
				switch (v.getId()) {
				case R.id.feed_entry_title_layout:
				case R.id.entry_title:
					openEntryInBrowser(entry.getUrl());
					break;
				default:
					break;
				}
			}
		};
	}

	/**
	 * Open browser after clicking the title
	 */
	private void openEntryInBrowser(String url) {
		if (entry.getUrl() != null) {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
			startActivity(browserIntent);
		}
	}

	@Override
	public void setTitle(CharSequence title) {
		getSupportActionBar().setTitle(title);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.entry, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			overridePendingTransition(R.anim.open_main, R.anim.close_next);
			return true;
		case R.id.action_share:
			shareEntry();
			return true;
		case R.id.action_mark_unread:
			markEntry(MARK_KEEP_UNREAD, getKeptAsUnreadSuccessListener());
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	private void markEntry(String markOrUnmark, Listener<JSONObject> successListener) {
		RequestQueue queue = MyVolley.getRequestQueue();
		try {
			JSONObject jsonRequest = new JSONObject();
			jsonRequest.put("action", markOrUnmark);
			jsonRequest.put("type", "entries");
			JSONArray entries = new JSONArray();
			entries.put(entryId);
			jsonRequest.put("entryIds", entries);
			JsonObjectRequest myReq = NetworkUtils.getJsonPostRequest(MainActivity.ROOT_URL + MARKERS_PATH,
					jsonRequest, successListener, createMyReqErrorListener(), accessToken);
			queue.add(myReq);
		} catch (JSONException uex) {
			Log.e(TAG, "Error marking read or unread");
		}
	}

	private Response.Listener<JSONObject> getKeptAsUnreadSuccessListener() {
		return new Response.Listener<JSONObject>() {
			@Override
			public void onResponse(JSONObject response) {
				Toast.makeText(FeedEntryActivity.this, getResources().getString(R.string.kept_as_unread),
						Toast.LENGTH_SHORT).show();
			}
		};
	}

	private Response.Listener<JSONObject> getMarkAsReadSuccessListener() {
		return new Response.Listener<JSONObject>() {
			@Override
			public void onResponse(JSONObject response) {
			}
		};
	}

	/**
	 * Share entry
	 */
	private void shareEntry() {
		Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
		sharingIntent.setType("text/plain");
		String shareBody = entry.getUrl();
		sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, entry.getTitle());
		sharingIntent.putExtra(android.content.Intent.EXTRA_TEXT, shareBody);
		startActivity(Intent.createChooser(sharingIntent, getResources().getString(R.string.share_text)));
	}
}
