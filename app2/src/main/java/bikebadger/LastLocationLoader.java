package bikebadger;

import android.content.Context;
import android.location.Location;

class LastLocationLoader extends DataLoader<Location> {
    private long mRunId;
    
    public LastLocationLoader(Context context, long runId) {
        super(context);
        mRunId = runId;
    }

    @Override
    public Location loadInBackground() {
        return RideManager.get(getContext()).getLastLocationForRun(mRunId);
    }
}