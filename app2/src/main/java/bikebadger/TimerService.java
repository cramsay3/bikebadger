package bikebadger;

import android.annotation.TargetApi;
import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.util.Log;

import us.theramsays.bikebadger.app2.R;
import util.Constants;

public class TimerService extends Service {
    public static final String BROADCAST_TIMER_ACTION = "bikebadger.TimerService";
    public static final int MY_TIMER_SERVICE_REEQUST_CODE = 1337;
    // When you connect a Handler to your UI thread, the code that handles messages runs on the UI thread.
    private final Handler handler = new Handler();
    private Intent mIntent;

    private int intervalInSeconds = 0;

    private SharedPreferences mPrefs;
    private long mStartSeconds = System.currentTimeMillis() / 1000;

    @Override
    public void onCreate() {
        // Called on service created
        mIntent = new Intent(BROADCAST_TIMER_ACTION);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(Constants.APP.TAG, "TimerService::onStartCommand()");
        try {
            Bundle extras = intent.getExtras();
            if (extras != null) {
                Integer i = extras.getInt("intervalInSeconds", 0);
                intervalInSeconds = i;
                Log.d(Constants.APP.TAG, "Setting timer to duration: " + i);
                mStartSeconds = System.currentTimeMillis() / 1000;
            }

            handler.removeCallbacks(sendUpdatesToUI);
            handler.post(sendUpdatesToUI);

            putNotification();

                   // Process it here

        } catch (Exception e) {

        }

        return START_NOT_STICKY;
}

    private int getTimeInterval()
    {
        mPrefs = PreferenceManager.getDefaultSharedPreferences( getApplicationContext() );
        int interval = intervalInSeconds;
        String sValue = mPrefs.getString("prefs_speak_speed_interval", Integer.toString(intervalInSeconds));

        try {
            interval = Integer.parseInt(sValue);
        } catch(NumberFormatException nfe) {
            Log.d("RunTracker", "Error parsing DecimalFormat prefs_speak_speed_interval");
        }

        return interval;
    }

private Runnable sendUpdatesToUI = new Runnable() {
    public void run() {
        try{
            Log.d("Bike Badger","TimerService::sendUpdatesToUIOnResume");
            broadcastTimeIsUp();
            handler.postDelayed(this, 1 * 1000); // put it on the queue again every second
            Log.d("Bike Badger","TimerService::sendUpdatesToUIOnResume intervalInSeconds=" + intervalInSeconds);
          } catch (Exception e) { }
    }
};

     private void broadcastTimeIsUp() {
        try {
            Log.d(Constants.APP.TAG,"TimerService::broadcastTimeIsUp");
            mIntent.putExtra("badger", Boolean.valueOf(false));
            long nowSeconds = System.currentTimeMillis() / 1000;
            long deltaSeconds = nowSeconds - mStartSeconds;
            // Check to see if the badger interval is up...
            if(deltaSeconds > getTimeInterval() ) {
                mIntent.putExtra("badger", Boolean.valueOf(true));
                mStartSeconds = System.currentTimeMillis() / 1000;
            } else {
                mIntent.putExtra("badger", Boolean.valueOf(false));
            }
            sendBroadcast(mIntent);
        } catch (Exception e) {
            Log.d("Bike Badger","Exception in TimerService::broadcastTimeIsUp");
         }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        Log.d(Constants.APP.TAG,"TimerService::onDestroy");
        try {
            Log.d(Constants.APP.TAG,"TimerService::removeCallbacks");
            handler.removeCallbacks(sendUpdatesToUI);
            stopForeground(true);
        } catch (Exception e) { }
        Log.d("Bike Badger","Exception in TimerService::onDestroy");
    }

    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    private void putNotification()
    {
        Intent i=new Intent(this, RideActivity.class);
        i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|
                Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pi=PendingIntent.getActivity(this, 0,
                i, PendingIntent.FLAG_UPDATE_CURRENT);

        // Notification that ties back to the Activity...
        Notification.Builder nb = new Notification.Builder(this)
                .setContentText("Bike Badger")
                .setSmallIcon(R.drawable.ic_launcher)
                .setWhen(System.currentTimeMillis())
                .setAutoCancel(true)
                .setTicker("Tracking")
                .setContentTitle("Bike Badger")
                .setContentText("Tracking")
                .setContentIntent(pi);

        Notification note  = nb.build();

        startForeground(MY_TIMER_SERVICE_REEQUST_CODE, note);
    }

    @Override
    public boolean stopService(Intent intentName) {
        Log.d("Bike Badger","TimerService::stopService");
        handler.removeCallbacks(sendUpdatesToUI);
        return super.stopService(intentName);
    }
}