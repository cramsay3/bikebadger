package bikebadger;

import android.content.Context;
import android.location.Location;
import android.util.Log;

import util.Constants;

public class TrackingLocationReceiver extends LocationReceiver {
    
    @Override
    protected void onLocationReceived(Context c, Location loc) {
        Log.d(Constants.APP.TAG, "TrackingLocationReceiver Got location from " + loc.getProvider() + ": " + loc.getLatitude() + ", " + loc.getLongitude());
        try {
            Log.d(Constants.APP.TAG, "RideManager.get(c).isTrackingRun = " + RideManager.get(c).isTrackingLocations());
           //TODO DO we need?
           // RideManager.get(c).insertLocation(loc);
            if(RideManager.get(c).isTrackingCurrentRun()) {
                if(RideManager.get(c).mCurrentRide != null) {
                    Log.d(Constants.APP.TAG, "RideManager.get(c).mCurrentRide = " + RideManager.get(c).mCurrentRide);
                    RideManager.get(c).mCurrentRide.UpdateAverageSpeed(loc.getSpeed() * Ride.METERS_TO_MILES); // in ft
                } else
                    Log.d(Constants.APP.TAG, "RideManager.get(c).mCurrentRide is null")  ;
                RideManager.get(c).ProcessProximityActions(loc);
                RideManager.get(c).mCurrentLocation = loc;
            }
        } catch (Exception e)
        {
           Log.d(Constants.APP.TAG, "RideManager.get(c).ProcessProximityActions(loc) crashed")  ;
            e.printStackTrace();
        }

        //RideManager.get(c).PlotCurrentLocation(loc);
    }

}
