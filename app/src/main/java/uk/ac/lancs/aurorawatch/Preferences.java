package uk.ac.lancs.aurorawatch;

import android.os.Bundle;
import android.preference.PreferenceActivity;

/**
 * Created by Craig on 23/01/2015.
 */
public class Preferences extends PreferenceActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
    }

//	@Override
//	protected void onResume() {
//		super.onResume();
//	}

//	@Override
//	protected void onPause() {
//		super.onPause();
//	}
}
