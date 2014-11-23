package bikebadger;

import android.content.Context;

class RunLoader extends DataLoader<Ride> {
    private long mRunId;
    
    public RunLoader(Context context, long runId) {
        super(context);
        mRunId = runId;
    }
    
    @Override
    public Ride loadInBackground() {
        return RideManager.get(getContext()).getRun(mRunId);
    }
}