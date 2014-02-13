package com.yairkukielka.feedhungry;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import com.crashlytics.android.Crashlytics;
import com.actionbarsherlock.app.ActionBar.OnNavigationListener;
import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonArrayRequest;
import com.android.volley.toolbox.JsonObjectRequest;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EActivity;
import com.googlecode.androidannotations.annotations.ViewById;
import com.yairkukielka.feedhungry.app.MyVolley;
import com.yairkukielka.feedhungry.feedly.Category;
import com.yairkukielka.feedhungry.feedly.Subscription;
import com.yairkukielka.feedhungry.settings.PreferencesActivity;
import com.yairkukielka.feedhungry.toolbox.MyListener;
import com.yairkukielka.feedhungry.toolbox.NetworkUtils;

@EActivity(R.layout.drawer_layout)
public class MainActivity extends SherlockFragmentActivity implements OnNavigationListener {

	private static final String TAG = MainActivity.class.getSimpleName();
	public static final String APP_PREFERENCES = "APP_PREFERENCS";
	// http://cloud.feedly.com in production
	public static final String ROOT_URL = "http://cloud.feedly.com";
	public static final String SUBSCRIPTIONS_URI = "/v3/subscriptions";
	public static final String MARKERS_COUNTS_URI = "/v3/markers/counts";
	public static final String MARKERS_PATH = "/v3/markers";
	public static final String TAGS_PATH = "/v3/tags";
	public static final String SHPREF_KEY_USERID_TOKEN = "User_Id";
	public static final String SHPREF_KEY_ACCESS_TOKEN = "Access_Token";
	public static final String SHPREF_KEY_REFRESH_TOKEN = "Refresh_Token";
	public static final String AUTORIZATION_HEADER = "Authorization";
	public static final String OAUTH_HEADER_PART = "OAuth ";
	private static final int PREF_CODE_ACTIVITY = 0;
	public static final String USERS_PREFIX_PATH = "user/";
	public static final String GLOBAL_ALL_SUFFIX = "/category/global.all";
	public static final String GLOBAL_UNCATEGORIZED_SUFFIX = "/category/global.uncategorized";
	public static final String GLOBAL_MUST_SUFFIX = "/category/global.must";
	public static final String GLOBAL_SAVED_SUFFIX = "/tag/global.saved";
	private boolean preferencesChanged = false;
	public static final String UTF_8 = "utf-8";
	private ActivityData activityData;
	private static final String FEEDLY_CATEGORIES = "Feedly Categories";
	private boolean isMix;
	protected String lastReadStreamId;
	private Menu actionBarmenu;
	private MenuItem reloadMenuItem;

	// private DisplayMetrics metrics;
	ExpandableListAdapter listAdapter;
	List<Category> categories;
	HashMap<String, List<Subscription>> subscriptionsMap;
	private ActionBarDrawerToggle mDrawerToggle;
	private CharSequence mDrawerTitle;
	private CharSequence mTitle;
	private String accessToken;
	private String userId;
	private List<Subscription> subscriptions;
	private Fragment loadingFragment;
	@ViewById(R.id.tv_about_developer)
	TextView tvAboutDeveloper;
	@ViewById(R.id.linear_layout)
	LinearLayout linearLayout;
	@ViewById(R.id.expandable)
	ExpandableListView mDrawerList;
	@ViewById(R.id.drawer_layout)
	DrawerLayout mDrawerLayout;

	@AfterViews
	void afterViews() {
		// ensures that your application is properly initialized with default
		// settings
		Crashlytics.start(this);
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		configureNavigationDrawer();
		// mDrawerList.setDivider(null);
		loadingFragment = new LoadingFragment_();
		configureActionBar();
		tvAboutDeveloper.setOnClickListener(mAboutDeveloperOnClickListener);
		// see if the activity data existed before
		activityData = (ActivityData) getLastCustomNonConfigurationInstance();
		if (activityData == null) {
			activityData = new ActivityData();
			categories = new ArrayList<Category>();
			subscriptionsMap = new LinkedHashMap<String, List<Subscription>>();
			startConnection();
		} else {
			// recover subscriptions from activity data
			subscriptionsMap = activityData.getSubscriptionsMap();
			categories = activityData.getCategories();
			lastReadStreamId = activityData.getLastReadStreamId();
			paintDrawerSubscriptions();
		}
	}

	/**
	 * Connects to the internet to access the subscriptions and global feeds
	 */
	private void startConnection() {
		expandRefreshMenuItem();
		mDrawerTitle = getApplicationName();
		setTitle(getApplicationName());
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction().replace(R.id.content_frame, loadingFragment).attach(loadingFragment)
				.addToBackStack(null).commit();
		if (isInternetAvailable(this)) {// returns true if internet available
			accessToken = getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE).getString(
					MainActivity.SHPREF_KEY_ACCESS_TOKEN, null);
			if (accessToken != null) {
				getSubscriptions();
			} else {
				collapseRefreshMenuItem();
				getAccessToken(mSuccessTokenListener(), mErrorTokenListener());
			}
		} else {
			collapseRefreshMenuItem();
			Toast.makeText(this, getResources().getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Looks if there is an access token. If there is, it will be used to ask
	 * for the user's subscritpions. If there isn't, WebviewFragment will be
	 * launched to get one.
	 * 
	 * @param mTokenListener
	 *            success listener
	 * @param mErrorTokenListener
	 *            error listener
	 */
	private void getAccessToken(MyListener.TokenListener mTokenListener,
			MyListener.ErrorTokenListener mErrorTokenListener) {
		Fragment fr = WebviewFragment.getInstance(mTokenListener, mErrorTokenListener);
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction().replace(R.id.content_frame, fr).commit();
		paintDrawerSubscriptions();
	}

	/**
	 * Creates a token listener to indicate authentication has finished with
	 * success
	 * 
	 * @return listener
	 */
	private MyListener.TokenListener mSuccessTokenListener() {
		return new MyListener.TokenListener() {
			@Override
			public void onResponse(String accessTokenParam) {
				accessToken = accessTokenParam;
				getSubscriptions();
			}
		};
	}

	/**
	 * Creates a token listener to indicate authentication has finished with
	 * errors
	 * 
	 * @return error listener
	 */
	private MyListener.ErrorTokenListener mErrorTokenListener() {
		return new MyListener.ErrorTokenListener() {
			@Override
			public void onErrorResponse(String error) {
				showErrorDialog(error);
			}
		};
	}

	/**
	 * Shows an error dialog
	 * 
	 * @param error
	 *            the error
	 */
	private void showErrorDialog(String error) {
		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setMessage(getResources().getString(R.string.receiving_subscriptions_exception));
		b.show();
	}

	private void getSubscriptions() {
		userId = getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE).getString(
				SHPREF_KEY_USERID_TOKEN, null);
		RequestQueue queue = MyVolley.getRequestQueue();
		JsonArrayRequest myReq = NetworkUtils.getJsonArrayRequest(ROOT_URL + SUBSCRIPTIONS_URI,
				createSuscriptionsSuccessListener(), createSuscriptionsErrorListener(), accessToken);
		queue.add(myReq);

	}

	private Response.Listener<JSONArray> createSuscriptionsSuccessListener() {
		return new Response.Listener<JSONArray>() {
			@Override
			public void onResponse(JSONArray response) {
				// show in the main fragment the global.all entries
				showEntriesFragment(USERS_PREFIX_PATH + userId + GLOBAL_ALL_SUFFIX);

				subscriptions = new ArrayList<Subscription>();
				for (int i = 0; i < response.length(); i++) {
					try {
						JSONObject jobject = (JSONObject) response.get(i);
						subscriptions.add(new Subscription(jobject));
					} catch (JSONException jse) {
						Log.w(TAG, getResources().getString(R.string.parsing_subscriptions_exception));
					}
				}
				prepareSubscriptionsData(subscriptions);
				getUnreadSubscriptions(subscriptions);
			}
		};
	}

	private Response.ErrorListener createSuscriptionsErrorListener() {
		return new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				collapseRefreshMenuItem();
				String errorMessage = getResources().getString(R.string.receiving_subscriptions_exception)
						+ error.getMessage();
				Log.e(TAG, errorMessage);
				paintDrawerSubscriptions();
				// usually here we must refresh the access token because it has
				// expired
				getAccessToken(mSuccessTokenListener(), mErrorTokenListener());
			}
		};
	}

	/**
	 * Get the unread articles numbers
	 * 
	 * @param subs
	 *            subscriptions to set the unread numbers in them
	 */
	private void getUnreadSubscriptions(List<Subscription> subs) {
		RequestQueue queue = MyVolley.getRequestQueue();
		JsonObjectRequest myReq = NetworkUtils.getJsonObjectRequest(ROOT_URL + MARKERS_COUNTS_URI,
				createUnreadSuscriptionsSuccessListener(), createUnreadSuscriptionsErrorListener(), accessToken);
		queue.add(myReq);
	}

	/**
	 * Receives the unread counts and sets them in the subscription objects
	 * 
	 * @return listener
	 */
	private Response.Listener<JSONObject> createUnreadSuscriptionsSuccessListener() {
		return new Response.Listener<JSONObject>() {
			@Override
			public void onResponse(JSONObject response) {
				try {
					JSONArray unreadCounts = response.getJSONArray("unreadcounts");
					String popularItemsTitle = getResources().getString(R.string.drawer_popular);
					for (int i = 0; i < unreadCounts.length(); i++) {
						JSONObject jobject = (JSONObject) unreadCounts.get(i);
						String id = jobject.getString("id");
						int numUnread = jobject.getInt("count");
						for (List<Subscription> subs : subscriptionsMap.values()) {
							for (Subscription s : subs) {
								if (s.getId().equals(id) && !s.getTitle().contains(popularItemsTitle)) {
									s.setUnread(numUnread);
									break;
								}
							}
						}
					}

				} catch (JSONException jse) {
					Log.w(TAG, getResources().getString(R.string.parsing_subscriptions_exception));
				}
				paintDrawerSubscriptions();
				collapseRefreshMenuItem();
			}

		};
	}

	/**
	 * Paints the drawer subscriptions
	 */
	private void paintDrawerSubscriptions() {
		listAdapter = new ExpandableListAdapter(MainActivity.this, categories, subscriptionsMap);
		mDrawerList.setAdapter(listAdapter);
	}

	private Response.ErrorListener createUnreadSuscriptionsErrorListener() {
		return new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				String errorMessage = getResources().getString(R.string.receiving_unread_subscriptions_exception)
						+ error.getMessage();
				Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
			}
		};
	}

	@Override
	protected void onPostCreate(Bundle savedInstanceState) {
		super.onPostCreate(savedInstanceState);
		// Sync the toggle state after onRestoreInstanceState has occurred.
		mDrawerToggle.syncState();
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		mDrawerToggle.onConfigurationChanged(newConfig);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			if (mDrawerLayout.isDrawerOpen(linearLayout)) {
				mDrawerLayout.closeDrawer(linearLayout);
			} else {
				mDrawerLayout.openDrawer(linearLayout);
			}
			return true;
		case R.id.action_settings:
			showSettings();
			return true;
		case R.id.action_refresh:
			refresh();
			return true;
		case R.id.action_mark_read:
			markAsReadFeedOrCategory();
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Refresh feeds
	 */
	private void refresh() {
		lastReadStreamId = null;
		subscriptionsMap.clear();
		categories.clear();
		startConnection();
	}

	/**
	 * Expand and change the refresh icon
	 */
	public void expandRefreshMenuItem() {
		if (actionBarmenu != null) {
			reloadMenuItem = actionBarmenu.findItem(R.id.action_refresh);
		}
		if (reloadMenuItem != null) {
			reloadMenuItem.setActionView(R.layout.progressbar);
			reloadMenuItem.expandActionView();
		}
	}

	/**
	 * Collapse and change the refresh icon
	 */
	public void collapseRefreshMenuItem() {
		if (actionBarmenu != null) {
			reloadMenuItem = actionBarmenu.findItem(R.id.action_refresh);
		}
		if (reloadMenuItem != null) {
			reloadMenuItem.setActionView(null);
			reloadMenuItem.collapseActionView();
		}
	}

	@Override
	public void onBackPressed() {
		if (!mDrawerLayout.isDrawerOpen(linearLayout)) {
			mDrawerLayout.openDrawer(linearLayout);
		} else {
			super.onBackPressed();
		}
	}

	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content
		// view
		boolean drawerOpen = mDrawerLayout.isDrawerOpen(linearLayout);
		// menu.findItem(R.id.action_websearch).setVisible(!drawerOpen);
		hideMenuItems(menu, !drawerOpen);
		return super.onPrepareOptionsMenu(menu);
	}

	private void hideMenuItems(Menu menu, boolean visible) {
		for (int i = 0; i < menu.size(); i++) {
			menu.getItem(i).setVisible(visible);
		}
	}

	/** A Subscription is selected */
	private void selectItem(ExpandableListView parent, int groupPosition, int childPosition) {
		Subscription chosenSub = subscriptionsMap.get(categories.get(groupPosition).getLabel()).get(childPosition);
		// if it has to get contents from /v3/mixes/contents endpoint, we set
		// the flag here
		if (getResources().getString(R.string.drawer_popular).equals(chosenSub.getTitle())) {
			isMix = true;
		} else {
			isMix = false;
		}
		showEntriesFragment(chosenSub.getId());
		// Highlight the selected item, update the title, and close the drawer
		setTitle(chosenSub.getTitle());

		int index = parent.getFlatListPosition(ExpandableListView.getPackedPositionForChild(groupPosition,
				childPosition));
		parent.setItemChecked(index, true);
		mDrawerLayout.closeDrawer(linearLayout);
	}

	/** A category is selected */
	private void selectItem(ExpandableListView parent, int groupPosition) {
		Category category = categories.get(groupPosition);
		if (!FEEDLY_CATEGORIES.equals(category.getId())) {
			// if feedly categories chosen, don't look for items
			showEntriesFragment(category.getId());
		}
		// Highlight the selected item, update the title, and close the drawer
		setTitle(category.getLabel());
		int index = parent.getFlatListPosition(ExpandableListView.getPackedPositionForGroup(groupPosition));
		parent.setItemChecked(index, true);
	}

	/**
	 * Creates a fragment with the entries of the stream of the subscription and
	 * shows the entries in the main fragment
	 * 
	 * @param id
	 *            stream id
	 */
	private void showEntriesFragment(String id) {
		try {
			Fragment entriesFragment = new EntryListFragment_();
			Bundle args = new Bundle();
			args.putString(EntryListFragment.STREAM_ID, URLEncoder.encode(id, UTF_8));
			args.putString(EntryListFragment.ACCESS_TOKEN, accessToken);
			args.putBoolean(EntryListFragment.IS_MIX, isMix);
			entriesFragment.setArguments(args);
			lastReadStreamId = id;

			// Insert the fragment by replacing any existing fragment
			FragmentManager fragmentManager = getSupportFragmentManager();
			// fragmentManager.beginTransaction().remove(loadingFragment).commit();
			// fragmentManager.beginTransaction().replace(R.id.content_frame,
			// entriesFragment).commit();
			// https://code.google.com/p/android/issues/detail?id=42601
			fragmentManager.beginTransaction().detach(loadingFragment).replace(R.id.content_frame, entriesFragment)
					.attach(entriesFragment).addToBackStack(null).commit();
		} catch (UnsupportedEncodingException uex) {
			Log.e(TAG, "Error encoding stream or category id");

		}
	}

	/**
	 * Initialize data for the adapter of the ExpandableListAdapter
	 */
	private void prepareSubscriptionsData(List<Subscription> subs) {
		categories = new ArrayList<Category>();
		subscriptionsMap = new LinkedHashMap<String, List<Subscription>>();

		// add global.all subscription to feedly categories
		Subscription allSubscription = new Subscription();
		allSubscription.setId(USERS_PREFIX_PATH + userId + GLOBAL_ALL_SUFFIX);
		String allLabel = getResources().getString(R.string.drawer_all);
		allSubscription.setTitle(allLabel);

		// add must read subscription to feedly categories
		Subscription mustReadSubscription = new Subscription();
		// here, global.all is used with the /v3/mixes/contents endpoint
		mustReadSubscription.setId(USERS_PREFIX_PATH + userId + GLOBAL_ALL_SUFFIX);
		String mustReadTitle = getResources().getString(R.string.drawer_popular);
		mustReadSubscription.setTitle(mustReadTitle);

		// add saved subscription to feedly categories
		Subscription savedSubscription = new Subscription();
		savedSubscription.setId(USERS_PREFIX_PATH + userId + GLOBAL_SAVED_SUFFIX);
		String savedLabel = getResources().getString(R.string.drawer_saved);
		savedSubscription.setTitle(savedLabel);

		// add global.uncategorized subscription to feedly categories
		Subscription uncategorizedSubscription = new Subscription();
		uncategorizedSubscription.setId(USERS_PREFIX_PATH + userId + GLOBAL_UNCATEGORIZED_SUFFIX);
		String uncategorizedLabel = getResources().getString(R.string.drawer_uncategorized);
		uncategorizedSubscription.setTitle(uncategorizedLabel);

		// add them to the feedlyCategory subscriptions list
		List<Subscription> feedlyCategoriesSubscriptions = new ArrayList<Subscription>();
		feedlyCategoriesSubscriptions.add(mustReadSubscription);
		feedlyCategoriesSubscriptions.add(savedSubscription);
		feedlyCategoriesSubscriptions.add(allSubscription);
		feedlyCategoriesSubscriptions.add(uncategorizedSubscription);
		// feedly categories
		Category feedlyCategory = new Category();
		feedlyCategory.setId(FEEDLY_CATEGORIES);
		String feedlyLabel = FEEDLY_CATEGORIES;
		feedlyCategory.setLabel(feedlyLabel);
		categories.add(feedlyCategory);
		subscriptionsMap.put(feedlyLabel, feedlyCategoriesSubscriptions);

		for (Subscription s : subs) {
			for (Category cat : s.getCategories()) {
				List<Subscription> categorySubscriptions = subscriptionsMap.get(cat.getLabel());
				if (categorySubscriptions == null) {
					categories.add(cat);
					categorySubscriptions = new ArrayList<Subscription>();
				}
				categorySubscriptions.add(s);
				subscriptionsMap.put(cat.getLabel(), categorySubscriptions);
			}
		}
	}

	@Override
	public boolean onNavigationItemSelected(int itemPosition, long itemId) {
		return true;
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getSupportMenuInflater();
		inflater.inflate(R.menu.main, menu);
		actionBarmenu = menu;
		return super.onCreateOptionsMenu(menu);
	}

	/**
	 * Show settings activity
	 */
	private void showSettings() {
		startActivityForResult(new Intent(this, PreferencesActivity.class), PREF_CODE_ACTIVITY);
	}

	/**
	 * Callback for started activities
	 */
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == PREF_CODE_ACTIVITY) {
			if (resultCode == PreferencesActivity.PREFERENCES_CODE) {
				preferencesChanged = true;
			}
		}
	}

	@Override
	protected void onPostResume() {
		super.onResume();
		if (preferencesChanged) {
			preferencesChanged = false;
			if (PreferencesActivity.LOG_OUT) {
				// log out. Clean accessToken, refreshToken and userId Token
				accessToken = null;
				getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE).edit()
						.putString(SHPREF_KEY_ACCESS_TOKEN, null).commit();
				getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE).edit()
						.putString(SHPREF_KEY_REFRESH_TOKEN, null).commit();
				getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE).edit()
						.putString(SHPREF_KEY_USERID_TOKEN, null).commit();
				PreferencesActivity.LOG_OUT = false;
			}
			refresh();
		}
	}

	/**
	 * Tells if there is internet connection
	 * 
	 * @param context
	 *            context
	 * @return true if there is connection. False otherwise
	 */
	public static boolean isInternetAvailable(Context context) {
		NetworkInfo info = (NetworkInfo) ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE))
				.getActiveNetworkInfo();

		if (info == null) {
			Log.d(TAG, "no internet connection");
			return false;
		} else {
			if (info.isConnected()) {
				Log.d(TAG, " internet connection available...");
				return true;
			} else {
				Log.d(TAG, " internet connection");
				return true;
			}
		}
	}

	/**
	 * Listener for the 'about developer' item
	 */
	private final OnClickListener mAboutDeveloperOnClickListener = new OnClickListener() {
		public void onClick(View v) {
			if (v == tvAboutDeveloper) {
				Fragment developerFragment = new developerFragment_();
				FragmentManager fragmentManager = getSupportFragmentManager();
				fragmentManager.beginTransaction().replace(R.id.content_frame, developerFragment).commit();
				setTitle(getResources().getString(R.string.about_developer));
				mDrawerLayout.closeDrawer(linearLayout);
			}
		}
	};

	/**
	 * Configures the navigation drawer
	 */
	private void configureNavigationDrawer() {
		mDrawerToggle = new ActionBarDrawerToggle(this, /* host Activity */
		mDrawerLayout, /* DrawerLayout object */
		R.drawable.ic_drawer, /* nav drawer icon to replace 'Up' caret */
		R.string.drawer_open, /* "open drawer" description */
		R.string.drawer_close /* "close drawer" description */
		) {

			/** Called when a drawer has settled in a completely closed state. */
			public void onDrawerClosed(View view) {
				getSupportActionBar().setTitle(mTitle);
				supportInvalidateOptionsMenu(); // creates call to
												// onPrepareOptionsMenu()
			}

			/** Called when a drawer has settled in a completely open state. */
			public void onDrawerOpened(View drawerView) {
				getSupportActionBar().setTitle(mDrawerTitle);
				supportInvalidateOptionsMenu(); // creates call to
												// onPrepareOptionsMenu()
			}
		};
		// Set the drawer toggle as the DrawerListener
		mDrawerLayout.setDrawerListener(mDrawerToggle);
		mDrawerLayout.setFocusableInTouchMode(false);

		// Listview on child click listener
		mDrawerList.setOnChildClickListener(new OnChildClickListener() {
			@Override
			public boolean onChildClick(ExpandableListView parent, View v, int groupPosition, int childPosition, long id) {
				selectItem(parent, groupPosition, childPosition);
				return true;
			}
		});
		// Listview on child click listener
		mDrawerList.setOnGroupClickListener(new OnGroupClickListener() {
			@Override
			public boolean onGroupClick(ExpandableListView parent, View v, int groupPosition, long id) {
				selectItem(parent, groupPosition);
				return false;
			}
		});

		// set a custom shadow that overlays the main content when the drawer
		mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);
	}

	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getSupportActionBar().setTitle(mTitle);
	}

	/**
	 * Configures the action bar
	 */
	private void configureActionBar() {
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
	}

	/**
	 * Action bar mark as read button for a feed, category or mix
	 * 
	 * @param successListener
	 *            successListener
	 */
	private void markAsReadFeedOrCategory() {
		RequestQueue queue = MyVolley.getRequestQueue();
		try {
			JSONObject jsonRequest = new JSONObject();
			jsonRequest.put("action", "markAsRead");
			String type = "feeds";
			boolean isCategory = isCategory(lastReadStreamId);
			if (isCategory) {
				type = "categories";
			}
			String typeName = "feedIds";
			if (isCategory) {
				typeName = "categoryIds";
			}
			jsonRequest.put("type", type);
			JSONArray entries = new JSONArray();
			entries.put(lastReadStreamId);
			jsonRequest.put(typeName, entries);
			// set the time
			jsonRequest.put("asOf", String.valueOf(new Date().getTime()));
			JsonObjectRequest myReq = NetworkUtils.getJsonPostRequest(ROOT_URL + MARKERS_PATH, jsonRequest,
					getMarkEntrySuccessListener(), createMyReqErrorListener(), accessToken);
			queue.add(myReq);
		} catch (JSONException uex) {
			Log.e(TAG, "Error marking read or unread");
		}
	}

	private boolean isCategory(String id) {
		if (id != null && id.contains("category")) {
			return true;
		}
		return false;
	}

	/**
	 * Returns a mark as read successListener
	 * 
	 * @return listener
	 */
	private Response.Listener<JSONObject> getMarkEntrySuccessListener() {
		return new Response.Listener<JSONObject>() {
			@Override
			public void onResponse(JSONObject response) {
				Toast.makeText(MainActivity.this,
						getResources().getString(R.string.marked_all_as_read_success_message), Toast.LENGTH_SHORT)
						.show();
			}
		};
	}

	/**
	 * Returns a mark as read errorListener
	 * 
	 * @return error listener
	 */
	private Response.ErrorListener createMyReqErrorListener() {
		return new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				showErrorDialog(error.getMessage());
			}
		};
	}

	/**
	 * Activity data to retain in case of a change of configuration, e.g.
	 * orientation of the device
	 */
	private class ActivityData {
		private List<Category> categories;
		private HashMap<String, List<Subscription>> subscriptionsMap;
		private String lastReadStreamId;

		public List<Category> getCategories() {
			return categories;
		}

		public void setCategories(List<Category> categories) {
			this.categories = categories;
		}

		public HashMap<String, List<Subscription>> getSubscriptionsMap() {
			return subscriptionsMap;
		}

		public void setSubscriptionsMap(HashMap<String, List<Subscription>> subscriptionsMap) {
			this.subscriptionsMap = subscriptionsMap;
		}

		public String getLastReadStreamId() {
			return lastReadStreamId;
		}

		public void setLastReadStreamId(String lastReadStreamId) {
			this.lastReadStreamId = lastReadStreamId;
		}

	}

	/**
	 * Get the application name
	 * 
	 * @return the app name
	 */
	private String getApplicationName() {
		int stringId = getApplicationInfo().labelRes;
		return getString(stringId);
	}

	/**
	 * To retain activity information. It's stored in this object so it is used
	 * in case there is a chagen of configuration, e.g. screen orientation
	 */
	@Override
	public Object onRetainCustomNonConfigurationInstance() {
		activityData.setCategories(categories);
		activityData.setSubscriptionsMap(subscriptionsMap);
		activityData.setLastReadStreamId(lastReadStreamId);
		return activityData;
	}

}
