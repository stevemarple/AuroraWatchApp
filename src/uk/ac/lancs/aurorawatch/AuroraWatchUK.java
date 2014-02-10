package uk.ac.lancs.aurorawatch;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

import org.xmlpull.v1.XmlPullParser;
import org.xmlpull.v1.XmlPullParserException;
import org.xmlpull.v1.XmlPullParserFactory;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.os.SystemClock;
import android.os.Vibrator;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

public class AuroraWatchUK extends Service implements Runnable {
	static public final String ALARM_ACTION = "uk.ac.lancs.aurorawatch.ALARM";
	private Handler handler;
	private NotificationManager notificationManager;

	// Unique Identification Number for the Notification.
	// We use it on Notification start, and to cancel it.
	private int NOTIFICATION = R.string.startService;

	URL url;

	// State
	AuroraWatchXml currentXml;
	AuroraWatchXml previousXml;
	// String currentLevel = "";
	// String currentLevelDescription = "";
	// long currentLevelColor = 0L;

	// Preferences
	long minutesToMillis = 60000L;
	long defaultUpdateInterval = 5 * minutesToMillis;
	long minimumUpdateInterval = 3 * minutesToMillis;
	// long defaultUpdateInterval = 10000;
	// long minimumUpdateInterval = 5000;
	long updateInterval = defaultUpdateInterval * minutesToMillis;

	private AlarmManager alarmManager;
	private WakeLock wakeLock;
	
	@Override
	public IBinder onBind(Intent arg0) {
		// Not using IPC so don't care about this method
		return null;
	}

	@Override
	public void onCreate() {
		super.onCreate();
		handler = new Handler();

		try {
			url = new URL("http://aurorawatch.lancs.ac.uk/android_app/status");
		} catch (MalformedURLException e) {
			Toast.makeText(this, getString(R.string.appName) + ": bad URL",
					Toast.LENGTH_LONG).show();
		}
		getPrefs();
		
		PowerManager powerMgr = (PowerManager) this
				.getSystemService(Context.POWER_SERVICE);
		wakeLock = powerMgr.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
				getClass().getSimpleName());
		wakeLock.acquire();
		alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
		
		startService();
		notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

		// Display a notification about us starting. We put an icon in the
		// status bar.
		showNotification();
		Toast.makeText(this, getString(R.string.appName) + " service started",
				Toast.LENGTH_LONG).show();
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		String action = null;
		if (intent != null)
			action = intent.getAction();
		Log.i(getClass().getSimpleName(), "onStartCommand() intent=" + intent + ", action=" + action);
		
		if (!ALARM_ACTION.equals(action)) {
			// Not alarm, so ensure alarm is running
			setAlarm();
		}

		// We want this service to continue running until it is explicitly
		// stopped, so return sticky.
		return START_STICKY;
	}

	@Override
	public void onDestroy() {
		runState = RunState.STATE_STOPPED;
		shutdownService();

		// Cancel the persistent notification.
		if (notificationManager != null)
			notificationManager.cancel(NOTIFICATION);

		super.onDestroy();
		Toast.makeText(this, getString(R.string.appName) + " service stopped",
				Toast.LENGTH_LONG).show();
	}

	private void startService() {
		Log.i(getClass().getSimpleName(), "starting service");
		handler.post(this);
	}

	private void shutdownService() {
		Log.i(getClass().getSimpleName(), "stopping service");
		if (wakeLock.isHeld())
			wakeLock.release();
	}

	// TODO: set a recurring alarm
	void setAlarm() {
		Intent intent = new Intent(ALARM_ACTION);
		PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0,
				intent, 0);
		long triggerTime = SystemClock.elapsedRealtime() + delay;
		alarmManager.cancel(pendingIntent);
		alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerTime,
				pendingIntent);
	}
	/**
	 * Show a notification while this service is running.
	 */
	private void showNotification() {
		// In this sample, we'll use the same text for the ticker and the
		// expanded notification
		CharSequence text = getText(R.string.startService);

		// Set the icon, scrolling text and timestamp
		Notification notification = new Notification(
				R.drawable.ic_stat_notify_aw, text, System.currentTimeMillis());

		// The PendingIntent to launch our activity if the user selects this
		// notification
		PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
				new Intent(this, AuroraWatchUKActivity.class), 0);
		// LocalServiceActivities.Controller.class), 0);

		// Set the info for the views that show in the notification panel.
		notification.setLatestEventInfo(this, getText(R.string.showApp), text,
				contentIntent);

		// Send the notification.
		notificationManager.notify(NOTIFICATION, notification);
	}

	enum RunState {
		STATE_STOPPED, STATE_STARTING, STATE_FETCHING_XML, STATE_PROCESSING_XML,
	}

	private RunState runState = RunState.STATE_STARTING;
	// private long delay;

	Boolean parsedXml = false;
	HttpRequestTask httpRequestTask;

	public void run() {
		long delay = 0;
		switch (runState) {
		case STATE_STOPPED:
			// Don't change state, don't post()
			return;

		case STATE_STARTING:
			if (!wakeLock.isHeld())
				wakeLock.acquire();
			
			// Get current preferences in case they have changed
			getPrefs();

			// Get the current status
			Log.i(getClass().getSimpleName(), "run()");
			// delay = 5000;
			runState = RunState.STATE_FETCHING_XML;
			break;

		case STATE_FETCHING_XML:
			currentXml = null;
			httpRequestTask = (HttpRequestTask) new HttpRequestTask()
					.execute(url);
			runState = RunState.STATE_PROCESSING_XML;
			delay = 10000;
			break;

		case STATE_PROCESSING_XML:
			if (currentXml != null) {
				Log.i(getClass().getSimpleName(), "Parsed " + url.toString());
				Log.i(getClass().getSimpleName(), currentXml.toString());
				// If we have previous XML details then compare the current
				// state now with the current state from the previous XML. If we
				// do not have the previous XML details (first run) then notify
				// user of current status as if it the level has changed.
				if (previousXml == null
						|| !currentXml.currentState.name
								.equals(previousXml.currentState.name)) {
					Vibrator v = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
					v.vibrate(500);
					Toast.makeText(
							this,
							getString(R.string.appName) + ": "
									+ currentXml.currentState.description,
							Toast.LENGTH_LONG).show();

					// Notify user with a status bar notification
					int icon = R.drawable.ic_stat_notify_aw;
					CharSequence tickerText = currentXml.currentState.description;
					long when = System.currentTimeMillis();

					Notification notification = new Notification(icon,
							tickerText, when);
					Context context = getApplicationContext();
					CharSequence contentTitle = getString(R.string.appName);
					CharSequence contentText = currentXml.currentState.description;
					Intent notificationIntent = new Intent(this,
							AuroraWatchUKActivity.class);
					PendingIntent contentIntent = PendingIntent.getActivity(
							this, 0, notificationIntent, 0);

					notification.setLatestEventInfo(context, contentTitle,
							contentText, contentIntent);
					notificationManager.notify(NOTIFICATION, notification);
				}
				try {
					previousXml = currentXml.clone();
				} catch (CloneNotSupportedException e) {
					Log.e(getClass().getSimpleName(),
							"Could not clone currentXml", e);
					previousXml = currentXml;
				}
				// Successful wait for a 'long' time
				delay = updateInterval;
			} else {
				httpRequestTask.cancel(true);
				Log.i(getClass().getSimpleName(), "Timeout fetching/parsing "
						+ url.toString());
//				// Try again, but don't wait for a full update interval
//				delay = updateInterval / 2;
			}

			// Set alarm ??
			Log.i(getClass().getSimpleName(),
					"run() waiting for " + Long.toString(delay) + " ms");
			runState = RunState.STATE_STARTING;
			if (wakeLock.isHeld())
				wakeLock.release();
			break;
		}

		handler.postDelayed(this, delay);
	}

	private void getPrefs() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(getBaseContext());
		SharedPreferences.Editor editor = prefs.edit();
		String updateIntervalStr = prefs.getString("updateInterval", "0");
		try {
			updateInterval = Long.parseLong(updateIntervalStr)
					* minutesToMillis;
		} catch (NumberFormatException e) {
			Log.i(getClass().getSimpleName(), "getPrefs(): could not parse "
					+ updateIntervalStr);
			updateInterval = defaultUpdateInterval;
			editor.putString("updateInterval",
					Long.toString(updateInterval / minutesToMillis));
		}
		if (updateInterval < minimumUpdateInterval) {
			updateInterval = minimumUpdateInterval;
			editor.putString("updateInterval",
					Long.toString(updateInterval / minutesToMillis));
		}

		editor.commit();
	}

	class HttpRequestTask extends AsyncTask<URL, Void, AuroraWatchXml> {

		protected AuroraWatchXml doInBackground(URL... params) {
			URL url = params[0];
			HttpURLConnection urlConnection;

			try {
				Log.i(getClass().getSimpleName(), "Fetching " + url.toString());
				urlConnection = (HttpURLConnection) url.openConnection();
				urlConnection.setRequestMethod("POST");
			} catch (IOException e) {
				Log.e(getClass().getSimpleName(),
						"Could not fetch " + url.toString(), e);
				return null;
			}
			Log.i(getClass().getSimpleName(), "Fetched " + url.toString());

			AuroraWatchXml awState = null;
			try {
				InputStream in = new BufferedInputStream(
						urlConnection.getInputStream());
				awState = parseStream(in, urlConnection.getContentEncoding());
			} catch (IOException e) {
				Log.e(getClass().getSimpleName(),
						"Could not fetch " + url.toString(), e);
			} catch (XmlPullParserException e) {
				Log.e(getClass().getSimpleName(),
						"Could not parse " + url.toString(), e);
			} catch (Exception e) {
				Log.e(getClass().getSimpleName(), "XML document not valid", e);
			} finally {
				urlConnection.disconnect();
			}
			Log.i(getClass().getSimpleName(), "Parsed " + url.toString());
			return awState;
		}

		private AuroraWatchXml parseStream(InputStream in, String encoding)
				throws Exception {

			Log.i(getClass().getSimpleName(), "parseStream()");
			AuroraWatchXml xmlState = new AuroraWatchXml();

			XmlPullParserFactory factory = XmlPullParserFactory.newInstance();
			factory.setNamespaceAware(true);
			XmlPullParser xpp = factory.newPullParser();

			xpp.setInput(in, encoding);
			// xpp.setInput( new StringReader ( "<foo>Hello World!</foo>" ) );
			int eventType = xpp.getEventType();
			String tag = "";
			AuroraWatchXml.State state = null;
			boolean documentValid = false;
			while (eventType != XmlPullParser.END_DOCUMENT) {
				if (isCancelled())
					throw new Exception("Task cancelled");
				if (eventType == XmlPullParser.START_TAG) {
					tag = xpp.getName();
					Log.d(getClass().getSimpleName(), "Found tag '" + tag + "'");
					if (tag.equals("state")) {
						state = new AuroraWatchXml.State();
						state.name = xpp.getAttributeValue(null, "name");
						// state.description = xpp.getAttributeValue(null,
						// "description");
						state.value = Integer.parseInt(xpp.getAttributeValue(
								null, "value"));
						state.color = Integer.parseInt(
								xpp.getAttributeValue(null, "color").substring(
										1), 16);
					}
				} else if (eventType == XmlPullParser.END_TAG) {
					tag = xpp.getName();
					Log.d(getClass().getSimpleName(), "End tag for " + tag);
					if (tag.equals("current")) {
						xmlState.currentState = state;
						Log.i(getClass().getSimpleName(), "end tag for " + tag
								+ " state: " + state.toString());
					} else if (tag.equals("previous")) {
						xmlState.previousState = state;
						Log.i(getClass().getSimpleName(), "end tag for " + tag
								+ " state: " + state.toString());
					} else if (tag.equals("aurorawatch"))
						documentValid = true;
					tag = "";

				} else if (eventType == XmlPullParser.TEXT) {
					String text = xpp.getText();
					Log.d(getClass().getSimpleName(), "Found text '" + text
							+ "', tag '" + tag + "'");
					if (tag.equals("state"))
						state.description = text;
					else if (tag.equalsIgnoreCase("station"))
						xmlState.station = text;
					else if (tag.equalsIgnoreCase("updated")) {
						DateFormat dfm = new SimpleDateFormat(
								"yyyy-MM-dd HH:mm:ss");
						dfm.setTimeZone(TimeZone.getTimeZone("UTC"));
						try {
							xmlState.updated = dfm.parse(text);
						} catch (ParseException e) {
							Log.e(getClass().getSimpleName(),
									"Could not parse date '" + text + "'", e);
							xmlState.updated = null;
						}
					}
				}
				eventType = xpp.next();
			}
			if (documentValid == false)
				throw new Exception("Document not valid");

			Log.i(getClass().getSimpleName() + ".onPostExecute()", "TID="
					+ Integer.toString(android.os.Process.myTid()) + " "
					+ xmlState.toString());
			return xmlState;
		}

		@Override
		protected void onPostExecute(AuroraWatchXml s) {
			if (s == null)
				Log.i(getClass().getSimpleName() + ".onPostExecute()", "(null)");
			else
				Log.i(getClass().getSimpleName() + ".onPostExecute()",
						s.toString());
			currentXml = s;
		}

	}
}