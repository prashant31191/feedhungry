package com.yairkukielka.feedhungry;

import java.util.HashMap;
import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import com.actionbarsherlock.app.SherlockFragment;
import com.android.volley.AuthFailureError;
import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.googlecode.androidannotations.annotations.AfterViews;
import com.googlecode.androidannotations.annotations.EFragment;
import com.googlecode.androidannotations.annotations.ViewById;
import com.yairkukielka.feedhungry.app.MyVolley;
import com.yairkukielka.feedhungry.toolbox.MyListener;

@EFragment(R.layout.auth_webview)
public class WebviewFragment extends SherlockFragment {

	@ViewById(R.id.item_webview)
	protected WebView webview;
	private static final String TAG = WebviewFragment.class.getSimpleName();
	private static final String AUTHORIZE_PATH = "/v3/auth/auth";
	private static final String AUTH_TOKEN_PATH = "/v3/auth/token";
	private static final String RESPONSE_TYPE_PATH = "?response_type=code";
	private static final String REDIRECT_URI_PATH = "&redirect_uri=";
	private static final String REDIRECT_URI = "http://localhost";
	private static final String SCOPE_PATH = "&scope=";
	public static final String CLIENT_ID = "feedhungry";//"sandbox"; // feedly en producción;
	public static final String CLIENT_SECRET = "FE01RP4J052ACM129GCW7KYB70MS";//"Z5ZSFRASVWCV3EFATRUY";
	private static final String SCOPE = "https://cloud.feedly.com/subscriptions";
	private static final String CLIENT_ID_PATH = "&client_id="; // feedly en
																// producción
	private String accessToken;
	private String refreshToken;
	private String userId;
	private Activity thisActivity;
	protected MyListener.ErrorTokenListener mErrorTokenListener;
	protected MyListener.TokenListener mTokenListener;

	public static WebviewFragment getInstance(
			MyListener.TokenListener mTokenListener,
			MyListener.ErrorTokenListener mErrorTokenListener) {
		WebviewFragment_ fragment = new WebviewFragment_();
		fragment.mErrorTokenListener = mErrorTokenListener;
		fragment.mTokenListener = mTokenListener;
		return fragment;
	}

	@SuppressLint("SetJavaScriptEnabled")
	@AfterViews
	void afterViews() {
		thisActivity = this.getActivity();		
		accessToken = thisActivity.getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE)
				.getString(MainActivity.SHPREF_KEY_ACCESS_TOKEN, null);
		refreshToken = thisActivity.getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE)
				.getString(MainActivity.SHPREF_KEY_REFRESH_TOKEN, null);
		if (refreshToken == null) {
			// delete cookies with the sessions
			CookieSyncManager.createInstance(getActivity());
			CookieManager cookieManager = CookieManager.getInstance();
			cookieManager.removeAllCookie();
			
			// do all the login process
			WebSettings webSettings = webview.getSettings();
			webSettings.setJavaScriptEnabled(true);
			// need to get access token with OAuth2.0
			// set up webview for OAuth2 login
			webview.setWebViewClient(new WebViewClient() {
				@Override
				public boolean shouldOverrideUrlLoading(WebView view, String url) {
					// Log.d(TAG, "** in shouldOverrideUrlLoading(), url is: " +
					// url);
					if (url.startsWith(REDIRECT_URI)) {

						if (url.indexOf("error=") != -1) {
							String error = mExtractError(url);
							mErrorTokenListener.onErrorResponse(error);
						} else if (url.indexOf("code=") != -1) {
							String code = mExtractCode(url);
							getTokens(false, code);
						}
						// don't go to redirectUri
						return true;
					}
					// load the webpage from url (login and grant access)
					return false;//super.shouldOverrideUrlLoading(view, url);
				}
			});

			// do OAuth2 login
			String authorizationUri = mReturnAuthorizationRequestUri();
			webview.loadUrl(authorizationUri);

		} else {
			// only do the refresh authentication token process
			getTokens(true, refreshToken);
		}
	}


	/**
	 * Spawns a worker thread in the volley library to get the access and
	 * refresh tokens
	 * 
	 * @param code
	 *            access code
	 */
	private void getTokens(Boolean isRefresh, String code) {
		JSONObject params = new JSONObject();
		try {
			if (isRefresh) {
				params.put("refresh_token", code);
				params.put("grant_type", "refresh_token");
			} else {
				params.put("code", code);
				params.put("state", "state-of-feedhungry");
				params.put("grant_type", "authorization_code");
				params.put("redirect_uri", REDIRECT_URI);
			}
			params.put("client_id", CLIENT_ID);
			params.put("client_secret", CLIENT_SECRET);
		} catch (JSONException je) {
			Log.e(TAG, "Error receiving tokens from Feedly");
		}
		RequestQueue queue = MyVolley.getRequestQueue();
		JsonObjectRequest myReq = new JsonObjectRequest(Method.POST, MainActivity.ROOT_URL
				+ AUTH_TOKEN_PATH, params, createAuthTokenSuccessListener(),
				createAuthTokenErrorListener()){
					@Override
					public Map<String, String> getHeaders() throws AuthFailureError {
						Map<String, String> params = new HashMap<String, String>();
						params.put("Content-Type", "application/json");
						return params;
					}
				};
		queue.add(myReq);
	}
	

	/**
	 * Gets the access and refresh tokens. Spawns a worker thread in the volley
	 * library.
	 * 
	 * @param code
	 *            access code
	 */
	private Response.Listener<JSONObject> createAuthTokenSuccessListener() {
		return new Response.Listener<JSONObject>() {
			@Override
			public void onResponse(JSONObject response) {
				Editor e = thisActivity.getSharedPreferences(MainActivity.APP_PREFERENCES, Context.MODE_PRIVATE).edit();				
				try {
					userId = response.getString("id");
					accessToken = response.getString("access_token");
					if (response.has("refresh_token")) {
						refreshToken = response.getString("refresh_token");
					}
				} catch (JSONException je) {
					Log.e(TAG, "Error al parsear el access token");
					je.printStackTrace();
				}
				e.putString(MainActivity.SHPREF_KEY_USERID_TOKEN, userId);
				e.putString(MainActivity.SHPREF_KEY_ACCESS_TOKEN, accessToken);
				e.putString(MainActivity.SHPREF_KEY_REFRESH_TOKEN, refreshToken);
				e.commit();

				mTokenListener.onResponse(accessToken);
			}
		};
	}

	/**
	 * Creates an error listener
	 * @return Error listnener
	 */
	private Response.ErrorListener createAuthTokenErrorListener() {
		return new Response.ErrorListener() {
			@Override
			public void onErrorResponse(VolleyError error) {
				Toast.makeText(thisActivity, getResources().getString(R.string.error_login), Toast.LENGTH_SHORT).show();
				mErrorTokenListener.onErrorResponse(error.getMessage());
			}
		};
	}

	/**
	 * Extract the access token
	 * 
	 * @param url
	 * @return
	 */
	private String mExtractCode(String url) {
		// url has format
		// https://localhost/#access_token=<tokenstring>&token_type=Bearer&expires_in=315359999
		String[] sArray = url.split("code=");
		return (sArray[1].split("&state"))[0];
	}

	private String mExtractError(String url) {
		// url has format
		// https://localhost/#access_token=<tokenstring>&token_type=Bearer&expires_in=315359999
		String[] sArray = url.split("error=");
		return (sArray[1].split("&state"))[0];
	}

	private String mReturnAuthorizationRequestUri() {
		StringBuilder sb = new StringBuilder();
		sb.append(MainActivity.ROOT_URL);
		sb.append(AUTHORIZE_PATH);
		sb.append(RESPONSE_TYPE_PATH);
		sb.append(REDIRECT_URI_PATH);
		sb.append(REDIRECT_URI);
		sb.append(CLIENT_ID_PATH);
		sb.append(CLIENT_ID);
		sb.append(SCOPE_PATH);
		sb.append(SCOPE);
		return sb.toString();
	}	
}
