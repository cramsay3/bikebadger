package bikebadger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import util.Constants;

/**
 * Created by cramsay on 9/8/2014.
 */
public class TimerServiceReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(Constants.APP.TAG, "TimerServiceReceiver:onReceive should be overridden");

        if(intent == null || context == null)
            return; // bail

    }
}