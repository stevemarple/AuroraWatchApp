package uk.ac.lancs.aurorawatch;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

public class AuroraWatchUKReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        WakeLock wakeLock;
        PowerManager powerMgr = (PowerManager) context
                .getSystemService(Context.POWER_SERVICE);
        wakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                getClass().getSimpleName());
        wakeLock.acquire();
        Log.i(getClass().getSimpleName(), "Starting service; received action "
                + intent.getAction());
        context.startService(new Intent(context, AuroraWatchUK.class));
        // context.startService(intent);
        wakeLock.release();

    }

}
