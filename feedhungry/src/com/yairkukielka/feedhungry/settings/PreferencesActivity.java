package com.yairkukielka.feedhungry.settings;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.yairkukielka.feedhungry.R;

public class PreferencesActivity extends SherlockPreferenceActivity implements OnSharedPreferenceChangeListener {
	public static final String KEY_ONLY_UNREAD = "pref_key_only_unread";
	public static final String KEY_PAGE_SIZE = "pref_key_page_size";
	public static final String KEY_LOGOUT = "pref_key_logout";
	public static final String KEY_LIST_WITH_CARDS = "pref_key_list_with_cards";
	public static final String KEY_LICENCES = "pref_key_licences";
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
		// action bar icon navagable up
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

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
		Preference thanksPref = (Preference) findPreference(KEY_LICENCES);
		thanksPref.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				// return the code to reset the accessToken
				if (KEY_LICENCES.equals(preference.getKey())) {
					Intent licencesIntent = new Intent(PreferencesActivity.this, LicencesActivity_.class);
					startActivity(licencesIntent);
				}
				return true;
			}

		});
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			finish();
			overridePendingTransition(R.anim.open_main, R.anim.close_next);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}
}
