package bikebadger;

import android.content.Context;
import android.os.Handler;

import java.util.ArrayList;

import gpx.Waypoint;

/**
 * Created by cramsay on 9/29/2014.
 */
public class DemoManager {

    static int  idx = 0;
    static ArrayList<Waypoint> waypoints = new ArrayList<Waypoint>();
    static RideManager mRunManger;
    static Handler handler = new Handler();

    final static Runnable r = new Runnable() {
        @Override
        public void run() {
           DoWaypoint(idx);
            idx++;
        }
    };

    public static void DoWaypoint(int idx) {
        if(idx < waypoints.size()) {
            Waypoint wp = waypoints.get(idx);
            mRunManger.mWaypoints.add(wp);
           // kick off subsequent ones every 10 seconds...
           handler.postDelayed(r, 10000);
        }
    }

    public static void Demo(Context c) {
        mRunManger = RideManager.get(c);
        mRunManger.mWaypoints = new ArrayList<Waypoint>();
        waypoints.add(new Waypoint("START 10.0", "START/LAP Waypoint", mRunManger.GetLastKnowLocation()));
        waypoints.add(new Waypoint("PROMPT", "PROMPT Waypoint", mRunManger.GetLastKnowLocation()));
        waypoints.add(new Waypoint("START 20.0", "START/LAP Waypoint", mRunManger.GetLastKnowLocation()));

        mRunManger.startNewRun();
        mRunManger.SpeakWords("Starting Demo");
        // kick off the first one.
        handler.post(r);

        /*
        for (final int idx = 0; idx < waypoints.size(); idx++) {

            mRunManger.mWaypoints.add(waypoints.get(idx));
            Runnable r = new Runnable();

            handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    mRunManger.mWaypoints.add(waypoints.get(idx));
                }
            }, 10000);

            handler = new Handler();
            handler.postDelayed(new Runnable() {
                public void run() {
                    mRunManger.AddNewWaypoint("SPEED", "SPEED Waypoint", mRunManger.GetLastKnowLocation());
                }
            }, 20000);

        }
 */
    }


}

