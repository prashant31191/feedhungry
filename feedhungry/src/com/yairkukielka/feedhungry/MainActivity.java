package com.yairkukielka.feedhungry;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

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
import android.support.v4.widget.DrawerLayout;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.View;
import android.widget.ExpandableListView;
import android.widget.ExpandableListView.OnChildClickListener;
import android.widget.ExpandableListView.OnGroupClickListener;
import android.widget.Toast;

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
	public static final String ROOT_URL = "http://sandbox.feedly.com";// http://cloud.feedly.com
																		// en
																		// producción
	public static final String SUBSCRIPTIONS_URI = "/v3/subscriptions";
	public static final String MARKERS_COUNTS_URI = "/v3/markers/counts";
	public static final String SHPREF_KEY_USERID_TOKEN = "User_Id";
	public static final String SHPREF_KEY_ACCESS_TOKEN = "Access_Token";
	public static final String SHPREF_KEY_REFRESH_TOKEN = "Refresh_Token";
	public static final String AUTORIZATION_HEADER = "Authorization";
	public static final String OAUTH_HEADER_PART = "OAuth ";
	private static final int PREF_CODE_ACTIVITY = 0;
	private boolean preferencesChanged = false;
	private static final String encoding = "utf-8";
	private ActivityData activityData;

	private DisplayMetrics metrics;
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

	@ViewById(R.id.left_drawer)
	ExpandableListView mDrawerList;
	@ViewById(R.id.drawer_layout)
	DrawerLayout mDrawerLayout;
	

	private class ActivityData {
		List<Category> categories;
		HashMap<String, List<Subscription>> subscriptionsMap;
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
		
		
	}
	
	@Override
	public Object onRetainCustomNonConfigurationInstance() {
//	    final MyDataObject data = collectMyLoadedData();
//	    return data;		
		return activityData;
	}
	
	@Override
	public void setTitle(CharSequence title) {
		mTitle = title;
		getSupportActionBar().setTitle(mTitle);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (savedInstanceState != null) {
			restoreProgress(savedInstanceState);
			return;
		} else {

		}
	}

	@Override
    protected void onRestoreInstanceState (Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // The activity is about to become visible.
    }
	@Override
    protected void onStart() {
        super.onStart();
        // The activity is about to become visible.
    }
	
	@Override
	protected void onSaveInstanceState(Bundle saveState) {
		super.onSaveInstanceState(saveState);
		saveState.putBoolean("waiting", true);
	}

	private void restoreProgress(Bundle savedInstanceState) {
	}

	@AfterViews
	void afterViews() {
		metrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(metrics);
		// ensures that your application is properly initialized with default
		// settings
		PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
		configureNavigationDrawer();
		// mDrawerList.setDivider(null);
		loadingFragment = new LoadingFragment_();
		configureActionBar();
		activityData = (ActivityData) getLastCustomNonConfigurationInstance();
		if (activityData == null) {
			activityData = new ActivityData();
			startConnection();
		} else {
			//prepareSubscriptionsData(subscriptions);
			subscriptionsMap = activityData.getSubscriptionsMap();
			categories = activityData.getCategories();
			paintDrawerSubscriptions();
		}
	}

	/**
	 * Connects to the internet to access the subscriptions and global feeds
	 */
	private void startConnection() {
		mTitle = mDrawerTitle = getTitle();
		// open drawer
		mDrawerLayout.openDrawer(mDrawerList);
		// Insert the fragment by replacing any existing fragment
		FragmentManager fragmentManager = getSupportFragmentManager();
		fragmentManager.beginTransaction().replace(R.id.content_frame, loadingFragment).commit();
		if (isInternetAvailable(this)) {// returns true if internet available
			checkAccessToken(mSuccessTokenListener(), mErrorTokenListener());
		} else {
			Toast.makeText(this, getResources().getString(R.string.no_internet_connection), Toast.LENGTH_SHORT).show();
		}
	}

	/**
	 * Configures the action bar
	 */
	private void configureActionBar() {
		//getSupportActionBar().setBackgroundDrawable(null);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);
		getSupportActionBar().setHomeButtonEnabled(true);
		// Context context = getSupportActionBar().getThemedContext();
		// ArrayAdapter<CharSequence> list =
		// ArrayAdapter.createFromResource(context, R.array.tags,
		// (android.R.layout.simple_list_item_1));
		// getSupportActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
		// getSupportActionBar().setListNavigationCallbacks(list, this);
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
	private void checkAccessToken(MyListener.TokenListener mTokenListener,
			MyListener.ErrorTokenListener mErrorTokenListener) {
		// getPreferences(Context.MODE_PRIVATE).edit().putString(SHPREF_KEY_ACCESS_TOKEN,
		// null).commit();
		accessToken = getPreferences(Context.MODE_PRIVATE).getString(SHPREF_KEY_ACCESS_TOKEN, null);
		if (accessToken == null) {
			Fragment fr = WebviewFragment.getInstance(mTokenListener, mErrorTokenListener);
			FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.beginTransaction().replace(R.id.content_frame, fr).commit();
		} else {
			getSubscriptions(accessToken);
		}
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
				//Toast.makeText(MainActivity.this, "User log in OK", Toast.LENGTH_SHORT).show();
				accessToken = accessTokenParam;
				getSubscriptions(accessToken);
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
				//Toast.makeText(MainActivity.this, error, Toast.LENGTH_SHORT).show();
			}
		};
	}

	private void getSubscriptions(final String accessTokenParam) {
		userId = getPreferences(Context.MODE_PRIVATE).getString(SHPREF_KEY_USERID_TOKEN, null);
		RequestQueue queue = MyVolley.getRequestQueue();
		JsonArrayRequest myReq = NetworkUtils.getJsonArrayRequest(ROOT_URL + SUBSCRIPTIONS_URI,
				createSuscriptionsSuccessListener(), createSuscriptionsErrorListener(), accessTokenParam);
		queue.add(myReq);

		// show in the main fragment the global.all entries
		showEntriesFragment("user/" + userId + "/category/global.all");
	}

	private Response.Listener<JSONArray> createSuscriptionsSuccessListener() {
		return new Response.Listener<JSONArray>() {
			@Override
			public void onResponse(JSONArray response) {
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
				String errorMessage = getResources().getString(R.string.receiving_subscriptions_exception)
						+ error.getMessage();
				//Toast.makeText(MainActivity.this, errorMessage, Toast.LENGTH_SHORT).show();
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
					for (int i = 0; i < unreadCounts.length(); i++) {
						JSONObject jobject = (JSONObject) unreadCounts.get(i);
						String id = jobject.getString("id");
						int numUnread = jobject.getInt("count");
						for (List<Subscription> subs : subscriptionsMap.values()) {
							for (Subscription s : subs) {
								if (s.getId().equals(id)) {
									s.setUnread(numUnread);
								}
							}
						}
					}
					// we have all the data to show the activity. It's stored in this object so it is used 
					// in case there is a chagen of configuration, e.g. screen orientation
					activityData.setCategories(categories);
					activityData.setSubscriptionsMap(subscriptionsMap);
				} catch (JSONException jse) {
					Log.w(TAG, getResources().getString(R.string.parsing_subscriptions_exception));
				}
				paintDrawerSubscriptions();
			}

		};
	}

	/**
	 * Paints the drawer subscriptions
	 */
	private void paintDrawerSubscriptions() {
		listAdapter = new ExpandableListAdapter(MainActivity.this, categories, subscriptionsMap, metrics);
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

		// The action bar home/up action should open or close the drawer.
		// ActionBarDrawerToggle will take care of this.
		// if (mDrawerToggle.onOptionsItemSelected(item)) {
		// return true;
		// }
		// Handle action buttons
		switch (item.getItemId()) {
		case android.R.id.home:
			if (mDrawerLayout.isDrawerOpen(mDrawerList)) {
				mDrawerLayout.closeDrawer(mDrawerList);
			} else {
				mDrawerLayout.openDrawer(mDrawerList);
			}
			return true;
		case R.id.action_settings:
			showSettings();
			return true;
		case R.id.action_refresh:
			refresh();
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	/**
	 * Refresh feeds
	 */
	private void refresh() {
		startConnection();
	}

	@Override
	public void onBackPressed() {
		if (!mDrawerLayout.isDrawerOpen(mDrawerList)) {
			mDrawerLayout.openDrawer(mDrawerList);
		} else {
			super.onBackPressed();
		}
	}

	/* Called whenever we call invalidateOptionsMenu() */
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		// If the nav drawer is open, hide action items related to the content
		// view
		boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);
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
		showEntriesFragment(chosenSub.getId());
		// Highlight the selected item, update the title, and close the drawer
		setTitle(chosenSub.getTitle());

		int index = parent.getFlatListPosition(ExpandableListView.getPackedPositionForChild(groupPosition,
				childPosition));
		parent.setItemChecked(index, true);

		// mDrawerList.setItemChecked(childPosition, true);
		mDrawerLayout.closeDrawer(mDrawerList);
	}

	/** A category is selected */
	private void selectItem(ExpandableListView parent, int groupPosition) {
		Category category = categories.get(groupPosition);
		showEntriesFragment(category.getId());
		// Highlight the selected item, update the title, and close the drawer
		setTitle(category.getLabel());

		int index = parent.getFlatListPosition(ExpandableListView.getPackedPositionForGroup(groupPosition));
		parent.setItemChecked(index, true);

		// mDrawerList.setItemChecked(childPosition, true);
		// mDrawerLayout.closeDrawer(mDrawerList);
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
			args.putString(EntryListFragment.STREAM_ID, URLEncoder.encode(id, encoding));
			args.putString(EntryListFragment.ACCESS_TOKEN, accessToken);
			entriesFragment.setArguments(args);

			// Insert the fragment by replacing any existing fragment
			FragmentManager fragmentManager = getSupportFragmentManager();
			fragmentManager.beginTransaction().remove(loadingFragment).commit();
			fragmentManager.beginTransaction().replace(R.id.content_frame, entriesFragment).commit();

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
			if (resultCode == PreferencesActivity.PREFERENCES_CHANGED) {
				preferencesChanged = true;
			}
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
	}
}
