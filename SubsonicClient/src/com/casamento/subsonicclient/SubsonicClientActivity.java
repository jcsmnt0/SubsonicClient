package com.casamento.subsonicclient;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;

import android.os.Bundle;

public class SubsonicClientActivity extends SherlockFragmentActivity {
	protected final static String logTag = "SubsonicClientActivity";
	protected final static String apiVersion = "1.4.0"; // Subsonic API version; 1.4.0+ required for JSON
	protected final static String clientId = "Android Subsonic Client";
    
    // don't restart activity on device rotation!
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
      //ignore orientation change
      super.onConfigurationChanged(newConfig);
    }
	
    @Override
    public void onCreate(final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    	this.setContentView(R.layout.main);
    }
    
    @Override
    public boolean onCreateOptionsMenu(com.actionbarsherlock.view.Menu menu) {
    	this.getSupportMenuInflater().inflate(R.menu.optionsmenu_main, menu);
    	return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(com.actionbarsherlock.view.MenuItem item) {
    	switch (item.getItemId()) {
    		case R.id.option_preferences:
    			Intent settingsActivity = new Intent(this.getBaseContext(), SupportPreferenceActivity.class);
    			startActivity(settingsActivity);
    			return true;
    			
    		default:
    			return super.onOptionsItemSelected(item);
    	}
    }
    
//    @Override
//    public void onBackPressed() {
//    	if (this.currentFolder != null) {
//	    	if (this.currentFolder.parent != null) {
//	    		this.setCurrentFolder(this.currentFolder.parent);
//	    	} else {
//	    		this.setCurrentMediaFolder(this.currentMediaFolder);
//	    	}
//	    	this.listView.setSelectionFromTop(this.savedScrollPositions.pop(), 0);
//	    	return;
//    	}
//    	super.onBackPressed();
//    }
}