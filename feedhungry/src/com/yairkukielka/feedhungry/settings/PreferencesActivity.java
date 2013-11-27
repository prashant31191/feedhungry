package com.yairkukielka.feedhungry.settings;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;

import com.yairkukielka.feedhungry.R;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String KEY_ONLY_UNREAD = "pref_key_only_unread";
	public static final String KEY_PAGE_SIZE = "pref_key_page_size";
	public static final String KEY_LOGOUT = "pref_key_logout";
	public static final String KEY_LIST_WITH_CARDS = "pref_key_list_with_cards";
	public static int PREFERENCES_CODE = 1;
	public static boolean LOG_OUT = false;

	@SuppressWarnings("deprecation")
	@Override
	protected void onResume() {
		super.onResume();
		getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	protected void onPause() {
		super.onPause();
		getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		setResult(PREFERENCES_CODE);
	}

	@Override
	public void onCreate(Bundle aSavedState) {
		super.onCreate(aSavedState);
		addPreferencesFromResource(R.xml.preferences);
		
		Preference logoutPref = (Preference) findPreference(KEY_LOGOUT);
		logoutPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
		             public boolean onPreferenceClick(Preference preference) {
		            	 // return the code to reset the accessToken
		            	 if (KEY_LOGOUT.equals(preference.getKey())) {
		            		 LOG_OUT = true;
		            	 }
		            	 setResult(PREFERENCES_CODE);
		            	 finish();
		            	 return true;
		             }
		         });
	}

}
