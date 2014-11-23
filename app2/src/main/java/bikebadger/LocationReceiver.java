package bikebadger;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.location.LocationManager;
import android.util.Log;

public class LocationReceiver extends BroadcastReceiver {

    private static final String TAG = "LocationReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Location loc = (Location)intent.getParcelableExtra(LocationManager.KEY_LOCATION_CHANGED);
        //if(isVisible)
        if (loc != null) {
            Log.d(TAG,"LocationReceiver::onReceive");
            onLocationReceived(context, loc);
            return;
        }

        Log.d(TAG, "Something else has happened as location is null ");

        // if we get here, something else has happened
        if (intent.hasExtra(LocationManager.KEY_PROVIDER_ENABLED)) {
            boolean enabled = intent.getBooleanExtra(LocationManager.KEY_PROVIDER_ENABLED, false);
            onProviderEnabledChanged(enabled);
        }
    }
    
    protected void onLocationReceived(Context context, Location loc) {
        // should be overridden
        if (loc != null)
        {
            Log.d(TAG, this + "LocationReceiver Got location from " + loc.getProvider() + ": " + loc.getLatitude() + ", " + loc.getLongitude());
        }
    }
    
    protected void onProviderEnabledChanged(boolean enabled) {
        Log.d(TAG, "onProviderEnabledChanged Provider " + (enabled ? "enabled" : "disabled"));
    }

}
