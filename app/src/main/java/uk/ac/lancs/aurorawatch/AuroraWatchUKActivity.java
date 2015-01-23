package uk.ac.lancs.aurorawatch;



import java.util.List;

import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Process;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.Toast;

public class AuroraWatchUKActivity extends Activity {

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(getClass().getSimpleName(), "onCreate() called");
        context = this;

        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        startService(new Intent(AuroraWatchUKActivity.this,
                AuroraWatchUK.class));

        Log.i(getClass().getSimpleName(),
                "onCreate() TID:" + Integer.toString(Process.myTid()));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.MENU_ABOUT:
                Toast.makeText(this, getString(R.string.aboutMessage),
                        Toast.LENGTH_LONG).show();
                break;
            case R.id.MENU_PREFS:
                Intent settingsActivity = new Intent(context, Preferences.class);
                startActivity(settingsActivity);
                break;
            case R.id.MENU_EXIT:
                stopService(new Intent(AuroraWatchUKActivity.this,
                        AuroraWatchUK.class));
                finish();
                break;
        }
        return true;
    }

    @Override
    public void onDestroy() {
        Log.i(getClass().getSimpleName(),
                "onDestroy() TID:" + Integer.toString(Process.myTid()));

        // Stop the service
        //stopService(new Intent(AuroraWatchUKActivity.this,
        //		AuroraWatchUK.class));

        // Make sure app is completely closed
        // forceCloseProcess(context, getPackageName());
        super.onDestroy();
    }

    public static void forceCloseProcess(Context context, String processName) {
        ActivityManager am = (ActivityManager) context
                .getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> pids = am
                .getRunningAppProcesses();
        for (int i = 0; i < pids.size(); i++) {
            ActivityManager.RunningAppProcessInfo info = pids.get(i);
            if (info.processName.equalsIgnoreCase(processName)) {
                android.os.Process.killProcess(info.pid);
            }
        }
    }


}