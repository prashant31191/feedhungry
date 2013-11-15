package com.yairkukielka.feedhungry;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.Format;
import java.text.SimpleDateFormat;
import java.util.Locale;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.R.color;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.webkit.WebSettings.LayoutAlgorithm;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
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
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.Extra;
import com.googlecode.androidannotations.annotations.ViewById;
import com.yairkukielka.feedhungry.app.MyVolley;
import com.yairkukielka.feedhungry.feedly.Entry;
import com.yairkukielka.feedhungry.toolbox.DateUtils;
import com.yairkukielka.feedhungry.toolbox.NetworkUtils;

@EActivity(R.layout.feed_entry_layout)
public class FeedEntryActivity extends SherlockFragmentActivity {
	private static final String TAG = FeedEntryActivity.class.getSimpleName();
	private static final String ENTRY_PATH = "/v3/entries/";
	private static final String MARKERS_PATH = "/v3/markers";
	private static final String MARK_READ = "markAsRead";
	private static final String MARK_KEEP_UNREAD = "keepUnread";
	private static final String ENTRY_ID = "entryId";
	public static final String ACCESS_TOKEN = "accessToken";
	private static final String BY = " by ";
	private static final String encoding = "utf-8";
	// private static final String HTML_HEAD =
	// "<head><style>@font-face {font-family: 'myFont';src: url('file:///android_asset/fonts/MyFont.otf');}body {font-family: 'myFont';}</style></head>";
	private static final String DIV_PREFIX = "<div style='background-color:transparent;padding: 5px;color:#ccc;font-family: myFont'>";
	private static final String DIV_SUFIX = "</div>";

	private ProgressBar progress;
	// the feed entry
	private Entry entry;
	// animation to show the feed content after the loading fragment shows
	private Animation webViewAnimation;
	// animation to show the title fading in
	private Animation titleAnimation;
	// fragment that shows while loading the entry content
	private Fragment loadingFragment;
	@Extra(ACCESS_TOKEN)
	String accessToken;
	@Extra(ENTRY_ID)
	String entryId;
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

	@ViewById(R.id.feed_entry_title_layout)
	RelativeLayout titleLayout;
	@ViewById(R.id.scroll_view_entry)
	ViewGroup root;

	@AfterViews
	void afterViews() {	 

		getSupportActionBar().setBackgroundDrawable(null);
		// set webView invisible so we don't see the white frame before it
		// loads.
		webView.setVisibility(View.INVISIBLE);
		// hide action bar
		// getSupportActionBar().hide();
		View.OnClickListener onClickListener = getOnClickListener();
		titleLayout.setOnClickListener(onClickListener);
		tvTitle.setOnClickListener(onClickListener);
		// action bar icon navagable up
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		// animate webview content
		webViewAnimation = AnimationUtils.loadAnimation(this, R.anim.push_up_in);
		titleAnimation = AnimationUtils.loadAnimation(this, R.anim.fade_in);
		tvTitle.setAnimation(titleAnimation);
		loadingFragment = new LoadingFragment_();
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction().replace(R.id.frame_webview, loadingFragment).commit();
		loadPage();
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
		String head = "<head><style>@font-face {font-family: 'myFont';src: url('file:///android_asset/fonts/Roboto-Light.ttf');}body {font-family: 'myFont';}</style></head>";
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

						// setTitle(e.getOriginTitle());
						tvTitle.setText(entry.getTitle());

						if (entry.getAuthor() != null) {
							tvAuthor.setText(BY + entry.getAuthor());
						}
						if (entry.getPublished() != null) {
							try {
								tvDate.setText(DateUtils.dateToString(entry.getPublished()));
							} catch (IllegalArgumentException ie) {
							}
						}
						// webView.loadData(HTML_PREFIX + e.getContent() +
						// HTML_SUFIX, mimeType, null);
						webView.loadDataWithBaseURL("file:///android_asset/", getHtmlData(entry.getContent()),
								"text/html", "utf-8", null);
						webView.getSettings().setLayoutAlgorithm(LayoutAlgorithm.SINGLE_COLUMN);
						webView.setBackgroundColor(color.transparent);
						webView.getSettings().setDefaultFontSize(18);//setTextZoom(WebSettings.TextSize.SMALLEST);
						webView.setWebViewClient(new WebViewClient() {	
							@Override					    
							public void onPageFinished(WebView view, String url) {
								FragmentManager fragmentManager = getSupportFragmentManager();
								fragmentManager.beginTransaction().remove(loadingFragment).commit();
								webView.setAnimation(webViewAnimation);
								webView.setVisibility(View.VISIBLE);
								// mark entry as read
								markEntry(MARK_READ, getMarkAsReadSuccessListener());
						        //mProgressView.setVisibility(View.INVISIBLE);
							}
						});
					}
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
				Toast.makeText(FeedEntryActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
			}
		};
	}

	private void showErrorDialog(String error) {
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setMessage("Error occured: " + error);
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
					openEntryInBrowser();
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
	private void openEntryInBrowser() {
		if (entry.getUrl() != null) {
			Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(entry.getUrl()));
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
			// app icon in action bar clicked; go home
			// NavUtils.navigateUpFromSameTask(this);
			finish();
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
			//new String[] { entryId }
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
				Toast.makeText(FeedEntryActivity.this, getResources().getString(R.string.keept_as_unread), Toast.LENGTH_SHORT).show();
			}
		};
	}

	private Response.Listener<JSONObject> getMarkAsReadSuccessListener() {
		return new Response.Listener<JSONObject>() {
			@Override
			public void onResponse(JSONObject response) { }
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
