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
    protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		
    	addPreferencesFromResource(R.xml.server_prefs);
        
        final Preference testConnection = this.findPreference("testConnection");
        testConnection.setOnPreferenceClickListener(new OnPreferenceClickListener() {
			public boolean onPreferenceClick(Preference preference) {
				//SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
				SharedPreferences prefs = getPreferenceManager().getSharedPreferences();
				SubsonicCaller caller = new SubsonicCaller(prefs.getString("serverUrl", ""), prefs.getString("username", ""), prefs.getString("password", ""), "1.4.0", "SubsonicClient_v0.0.0.1.0_pre-pre-alpha", getBaseContext());
				testConnection.setSummary("Testing connection...");
				caller.ping(new OnPingResponseListener() {
					@Override
					void onPingResponse(boolean ok) {
						if (ok)
							testConnection.setSummary("Success!");
						else
							testConnection.setSummary("Failure.");
					}
				});
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