/*
 * Copyright (c) 2012, Joseph Casamento
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.casamento.subsonicclient;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.text.InputType;
import com.actionbarsherlock.app.SherlockPreferenceActivity;

import java.util.Arrays;

public class PreferenceActivity extends SherlockPreferenceActivity implements OnSharedPreferenceChangeListener {
	final PreferenceActivity self = this;

	@SuppressWarnings("deprecation")
	@Override
	protected void onCreate(final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		addPreferencesFromResource(R.xml.server_prefs);

		final Preference testConnection = this.findPreference("testConnection");
		testConnection.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				SharedPreferences prefs = getPreferenceManager().getSharedPreferences();

				final String serverUrl, username, password;
				if ((serverUrl = prefs.getString("serverUrl", "")).equals("")) {
					testConnection.setSummary(getString(R.string.missing_server_url));
					return true;
				}
				if ((username = prefs.getString("username", "")).equals("")) {
					testConnection.setSummary(getString(R.string.missing_username));
					return true;
				}
				if ((password = prefs.getString("password", "")).equals("")) {
					testConnection.setSummary(getString(R.string.missing_password));
					return true;
				}

				testConnection.setSummary(getString(R.string.testing_connection));

				try {
					new SubsonicCaller(serverUrl, username, password, self).ping(new SubsonicCaller.OnPingResponseListener() {
						@Override
						public void onPingResponse(boolean ok) {
							testConnection.setSummary(ok ? getString(R.string.success) : getString(R.string.failure));
						}
					});
				} catch (Exception e) {
					testConnection.setSummary(getString(R.string.failure));
				}
				return true;
			}
		});
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onResume() {
		super.onResume();

		SharedPreferences prefs = this.getPreferenceScreen().getSharedPreferences();

		for (String prefStr : prefs.getAll().keySet()) {
			PreferenceActivity.updatePrefSummary(this.findPreference(prefStr));
		}

		prefs.registerOnSharedPreferenceChangeListener(this);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onPause() {
		super.onPause();

		this.getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
	}

	// set the preference summary to its value
	static private void updatePrefSummary(Preference pref) {
		if (pref instanceof EditTextPreference) {
			EditTextPreference etPref = (EditTextPreference)pref;
			String summary = etPref.getText();

			// if password, only show '*' chars
			if (etPref.getEditText().getInputType() == (InputType.TYPE_TEXT_VARIATION_PASSWORD | InputType.TYPE_CLASS_TEXT)) {
				char[] chars = new char[summary.length()];
				Arrays.fill(chars, '*');
				summary = new String(chars);
			}

			pref.setSummary(summary);
		}
	}

	@SuppressWarnings("deprecation")
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Preference pref = this.findPreference(key);
		PreferenceActivity.updatePrefSummary(pref);
	}
}