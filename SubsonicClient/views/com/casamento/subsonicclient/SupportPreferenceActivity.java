package com.casamento.subsonicclient;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Configuration;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.text.InputType;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;

// some code from http://stackoverflow.com/questions/10186697/preferenceactivity-android-4-0-and-earlier/11336098#11336098 and http://www.blackmoonit.com/2012/07/all_api_prefsactivity/
// should work the same across all API levels, unlike the built-in PreferenceActivity or PreferenceFragment
public class SupportPreferenceActivity extends PreferenceActivity implements OnSharedPreferenceChangeListener {
	private final static String logTag = "SupportPreferenceActivity";
	protected Method mLoadHeaders = null;
	protected Method mHasHeaders = null;

	// don't restart activity on device rotation!
	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		//ignore orientation change
		super.onConfigurationChanged(newConfig);
	}
	
	/**
     * Checks to see if using new v11+ way of handling PrefFragments.
     * @return Returns false pre-v11, else checks to see if using headers.
     */
    public boolean isNewV11Prefs() {
        if (mHasHeaders!=null && mLoadHeaders!=null) {
            try {
                return (Boolean)mHasHeaders.invoke(this);
            } catch (IllegalArgumentException e) {
            } catch (IllegalAccessException e) {
            } catch (InvocationTargetException e) {
            }
        }
        return false;
    }

    @SuppressWarnings("deprecation")
	@Override
    public void onCreate(Bundle aSavedState) {
        //onBuildHeaders() will be called during super.onCreate()
        try {
            mLoadHeaders = getClass().getMethod("loadHeadersFromResource", int.class, List.class);
            mHasHeaders = getClass().getMethod("hasHeaders");
        } catch (NoSuchMethodException e) {
        }
        super.onCreate(aSavedState);
        if (!isNewV11Prefs()) {
            addPreferencesFromResource(R.xml.server_prefs);
            
            Preference testConnection = this.findPreference("testConnection");
            testConnection.setOnPreferenceClickListener(new OnPreferenceClickListener() {
				public boolean onPreferenceClick(Preference preference) {
					SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
					SubsonicCaller caller = new SubsonicCaller(prefs.getString("serverUrl", ""), prefs.getString("username", ""), prefs.getString("password", ""), "1.4.0", "SubsonicClient_v0.0.0.1.0_pre-pre-alpha", getBaseContext());
					caller.ping(new OnPingResponseListener() {
						@Override
						void onPingResponse(boolean ok) {

						}
					});
					return true;
				}
            });
        }
    }

    @Override
    public void onBuildHeaders(List<Header> aTarget) {
        try {
            mLoadHeaders.invoke(this,new Object[]{R.xml.pref_headers,aTarget});
        } catch (IllegalArgumentException e) {
        } catch (IllegalAccessException e) {
        } catch (InvocationTargetException e) {
        }   
    }
    
    @SuppressWarnings("deprecation")
	@Override
    public void onResume() {
    	super.onResume();
    	
    	if (!isNewV11Prefs()) {
	    	SharedPreferences prefs = this.getPreferenceScreen().getSharedPreferences();
	    	
	    	for (String prefStr : prefs.getAll().keySet()) {
	    		SupportPreferenceActivity.updatePrefSummary(this.findPreference(prefStr));
	    	}
	    	
	    	prefs.registerOnSharedPreferenceChangeListener(this);
    	}
    }
    
    @SuppressWarnings("deprecation")
	@Override
    public void onPause() {
    	super.onPause();
    	
    	if (!isNewV11Prefs())
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
    	SupportPreferenceActivity.updatePrefSummary(pref);
    }
    
    static public class SupportPreferenceFragment extends PreferenceFragment implements OnSharedPreferenceChangeListener {
        @Override
        public void onCreate(Bundle aSavedState) {
            super.onCreate(aSavedState);
            final Context actContext = getActivity().getApplicationContext();
            int thePrefRes = actContext.getResources().getIdentifier(getArguments().getString("pref-resource"), "xml", actContext.getPackageName());
            addPreferencesFromResource(thePrefRes);
            
            Preference testConnection = this.findPreference("testConnection");
            testConnection.setOnPreferenceClickListener(new OnPreferenceClickListener() {
            	public boolean onPreferenceClick(Preference preference) {
					SharedPreferences prefs = getPreferenceScreen().getSharedPreferences();
					SubsonicCaller caller = new SubsonicCaller(prefs.getString("serverUrl", ""), prefs.getString("username", ""), prefs.getString("password", ""), "1.4.0", "SubsonicClient_v0.0.0.1.0_pre-pre-alpha", getActivity());
					caller.ping(new OnPingResponseListener() {
						@Override
						void onPingResponse(boolean ok) {
//							if (ok)
//								Util.showSingleButtonAlertBox(getActivity(), "Success!", "Hooray!");
//							else
//								Util.showSingleButtonAlertBox(getActivity(), "Failure.", "Sadness");
						}
					});
					return true;
				}
            });
        }
        
    	@Override
        public void onResume() {
        	super.onResume();
        	
        	SharedPreferences prefs = this.getPreferenceScreen().getSharedPreferences();
        	
        	for (String prefStr : prefs.getAll().keySet()) {
        		SupportPreferenceActivity.updatePrefSummary(this.findPreference(prefStr));
        	}
        	
        	prefs.registerOnSharedPreferenceChangeListener(this);
        }
        
        @Override
		public void onPause() {
        	super.onPause();
        	
        	this.getPreferenceScreen().getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        }

		public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
			Preference pref = this.findPreference(key);
	    	SupportPreferenceActivity.updatePrefSummary(pref);
		}
    }
}
