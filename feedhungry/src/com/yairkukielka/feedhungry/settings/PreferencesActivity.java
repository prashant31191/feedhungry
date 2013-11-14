package com.yairkukielka.feedhungry.settings;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.PreferenceActivity;

import com.yairkukielka.feedhungry.R;

public class PreferencesActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String KEY_ONLY_UNREAD = "pref_key_only_unread";
	public static final String KEY_PAGE_SIZE = "pref_key_page_size";
	public static final int PREFERENCES_CHANGED = 1;

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
		setResult(PREFERENCES_CHANGED);
	}

	@Override
	public void onCreate(Bundle aSavedState) {
		super.onCreate(aSavedState);
		addPreferencesFromResource(R.xml.preferences);
	}

}
