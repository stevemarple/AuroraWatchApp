package uk.ac.lancs.aurorawatch;

import uk.ac.lancs.aurorawatch.R;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.util.Log;

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
