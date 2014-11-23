package bikebadger;

import android.app.ActivityManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.media.AudioManager;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.provider.MediaStore;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

import export.format.WaypointsTracksToGPX;
import gpx.GPX;
import gpx.Waypoint;
import gpx.parser.GPXTrackParser;
import gpx.parser.GPXWaypointParser;
import gpx.parser.ParsingException;
import us.theramsays.bikebadger.app2.R;
import util.Constants;
import util.mediamanager.GoogleMusicUtils;
import util.LatLngUtils;
import util.MyToast;
import util.mediamanager.PowerAmpUtils;
import util.SafeParse;
import util.TaskerIntent;

public class RideManager {

    private static final String PREF_CURRENT_RUN_ID = "RideManager.currentRunId";
    public static final String PREF_MAP_TYPE = "RideManager.MAP_TYPE";
    public static final String ACTION_LOCATION = "us.theramsays.bikebadger.ACTION_LOCATION";
    public static final String PREF_GPX_PATH = "RideManager.PREF_GPX_PATH";

    // singleton
    private static RideManager singletonRideManager;
    public Context mAppContext;
    private LocationManager mLocationManager;
    private RunDatabaseHelper mHelper;

    // prefs
    public SharedPreferences mPrefs;
    public SharedPreferences.Editor mPrefsEditor;
    private SharedPreferences.OnSharedPreferenceChangeListener mPrefsListener;
    private boolean mStartUponLaunch = false;
    private int mProximityAccuracy = 200;

    private double mDefaultTargetAvgSpeed;
    private boolean mBadger = true;
    private int mBadgerIntervalSeconds;
    private static final int BADGER_LEVEL_SIMPLE = 0;
    private static final int BADGER_LEVEL_DETAILED = 1;
    private static final int BADGER_LEVEL_VERBOSE = 2;
    private short mBadgerLevel = BADGER_LEVEL_SIMPLE;
    private boolean mBadgerIncludeAvgSpeed;
    private String mBehindPhrase;
    private String mAheadPhrase;
    private boolean mStartNow;

    // run
    private long mCurrentRunId;
    public Ride mCurrentRide = null;

    // text to speech
    public static TextToSpeech mTTS;

    // action waypoint management
    public ArrayList<Waypoint> mWaypoints;
    private boolean mOkayToLaunch = true;
    public Waypoint mClosestWaypoint;
    public double mClosestDistance = 40000000; // around the world
    public double mClosestBearing = 0;

    // map
    public GoogleMap mMap;
    private Marker mCurrentMarker = null;
    public PolylineOptions mPolylineOptions;
    public ArrayList<LatLng> mTrkpts = null;

    // location
    public Location mCurrentLocation;

    // file management
    public String mCurrentGPXPath;
    public boolean mLoadLastGPXFile = false;
    private boolean mIsDirty = false;

    // ctor
    private RideManager(Context appContext) {

        Log.d(Constants.APP.TAG, "RunManger::RideManager(Context appContext = " + appContext);
        mAppContext = appContext;
        //assert(mAppContext != null);
        mLocationManager = (LocationManager) mAppContext.getSystemService(Context.LOCATION_SERVICE);

       // mLocationManager.getGpsStatus()
        mHelper = new RunDatabaseHelper(mAppContext);
        mPrefs = PreferenceManager.getDefaultSharedPreferences(mAppContext);
        mPrefsEditor = mPrefs.edit();

        initializeMembers();

        //ArrayList<ActionCommand> actions = new ArrayList<ActionCommand>();
        /*
        final ActionCommand action1 = new ActionCommand(mAppContext, "START",  Arrays.asList("LAP", "START") );

       Runnable run = new Runnable() {
            @Override
           public void run() {
                mCurrentRide.SetTargetAvgSpeed(action1.getArgument(), mDefaultTargetAvgSpeed);
                SpeakWords(String.format("Target speed set to %.1f miles per hour.", mCurrentRide.GetTargetAvgSpeed()));
                mCurrentRide.ResetAverageSpeed(); // clear the current averageSpeed
           }
        };
        //action1.CreateRunnable("11");
        //action.setRunnable( new Runnable() {} );
 */

        Thread.setDefaultUncaughtExceptionHandler(new CrashHandler());
    }

    public void SetDefaultTargetAvgSpeed(double defaultTargetAvgSpeed) {
        this.mDefaultTargetAvgSpeed = defaultTargetAvgSpeed;
        mPrefsEditor.putString("pref_target_value", Double.toString(defaultTargetAvgSpeed));
        mPrefsEditor.commit();
    }

    public boolean IsGPSEnabled() {
        boolean isEnabled = false;
        if(mLocationManager != null) {
            isEnabled = mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        }

        return isEnabled;
    }

    public final class CrashHandler implements Thread.UncaughtExceptionHandler {
        private final Thread.UncaughtExceptionHandler handler;

        public CrashHandler() {
            // Uncomment this line if you want to show the default app crash message
            this.handler = Thread.getDefaultUncaughtExceptionHandler();
        }

        @Override
        public void uncaughtException(final Thread thread, final Throwable throwable) {
            // Show pretty message to user
            File file = new File("printStackTrace.log");
            try {
                new AlertDialog.Builder(mAppContext)
                        .setTitle("Error")
                        .setMessage(throwable.getMessage())
                        .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // continue
                            }
                        })
                        .setNegativeButton(android.R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                // do nothing
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();


                File extStore = Constants.APP.EXTERNAL_APP_DIR;
                FileOutputStream fs = new FileOutputStream(extStore + "/bikebadger.log");
                PrintWriter pw = new PrintWriter(fs);
                //PrintStream ps = new PrintStream(file);
                throwable.printStackTrace(pw);
                pw.close();
                Shutdown();
            } catch (FileNotFoundException fnf)
            {
                Log.d(Constants.APP.TAG, "File not found!");
            }
             // Uncomment this line to show the default app crash message
           this.handler.uncaughtException(thread, throwable);
        }
    }

    public void initializeMembers() {
        Log.d(Constants.APP.TAG, "RunManger::initializeMembers");


        setMemberFromPreferences("pref_start");
        setMemberFromPreferences("pref_badger_proximity");
        setMemberFromPreferences("pref_target_value");
        setMemberFromPreferences("prefs_speak_speed_interval");
        setMemberFromPreferences("pref_badger");
        setMemberFromPreferences("pref_badger_level");
        setMemberFromPreferences("pref_badger_include_avg_speed");
        setMemberFromPreferences("pref_badger_behind_phrase");
        setMemberFromPreferences("pref_badger_ahead_phrase");
        setMemberFromPreferences(PREF_MAP_TYPE);
        setMemberFromPreferences(PREF_GPX_PATH);
        setMemberFromPreferences("pref_load_last_file");

          // Use instance field for listener
        // It will not be gc'd as long as this instance is kept referenced
        mPrefsListener = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                setMemberFromPreferences(key);
            }
        };

        mPrefs.registerOnSharedPreferenceChangeListener(mPrefsListener);
      }

    private void setMemberFromPreferences(String key) {
        Log.d(Constants.APP.TAG, "setMemberFromPreferences");
        if (key.equals("pref_target_value")) {
            mDefaultTargetAvgSpeed = SafeParse.parseDecimal(mPrefs.getString("pref_target_value", "11.0"), 11.0);
        } else if (key.equals("prefs_speak_speed_interval")) {
            mBadgerIntervalSeconds = SafeParse.parseInt(mPrefs.getString("prefs_speak_speed_interval", "60"), 60);
        } else if (key.equals("pref_badger_proximity")) {
            mProximityAccuracy = SafeParse.parseInt(mPrefs.getString("pref_badger_proximity", "200"), 200);
        } else if(key.equals("pref_badger")) {
            mBadger = mPrefs.getBoolean("pref_badger", true);
        } else if(key.equals("pref_start")) {
            mStartNow= mPrefs.getBoolean("pref_start", false);
        } else if(key.equals("pref_badger_include_avg_speed")) {
            mBadgerIncludeAvgSpeed = mPrefs.getBoolean("pref_badger_include_avg_speed", true);
        } else if(key.equals("pref_badger_level")) {
                 mBadgerLevel = SafeParse.parseShort( mPrefs.getString("pref_badger_level", "1"), (short)1);
        } else if(key.equals(PREF_MAP_TYPE)) {
            SetPreferenceMapType();
        } else if(key.equals("pref_badger_behind_phrase")) {
            mBehindPhrase = mPrefs.getString("pref_badger_behind_phrase", "Behind");
        } else if(key.equals("pref_badger_ahead_phrase")) {
            mAheadPhrase = mPrefs.getString("pref_badger_ahead_phrase", "Ahead");
        } else if(key.equals(PREF_GPX_PATH)) {
            mCurrentGPXPath = mPrefs.getString(PREF_GPX_PATH, Constants.APP.DEFAULT_GPX_PATH);
        } else if(key.equals("pref_load_last_file")) {
            mLoadLastGPXFile = mPrefs.getBoolean("pref_load_last_file", false);
        }
    }

    public void SetPreferenceMapType()
    {
        int map_type = mPrefs.getInt(PREF_MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL);

        try {
            if(mMap != null)
                mMap.setMapType(map_type);
        } catch (NumberFormatException nfe) {
             Log.d("RunTracker", "Error parsing RideManager.MapType");
        }
    }

    public static RideManager get(Context c) {
        if (singletonRideManager == null) {
            // we use the application context to avoid leaking activities
            Log.d(Constants.APP.TAG,"Creating a NEW sRideManager");
            singletonRideManager = new RideManager( c.getApplicationContext() );
        }
        return singletonRideManager;
    }

    public String GetGPXFileName()
    {
        File f = new File(mCurrentGPXPath);
        if (f != null)
            return f.getName();
        else
            return "";
    }

    public void AddNewWaypoint(String command, String argument, Location location)
    {

        // START arg
        // PROMPT desc
        // PLAYLIST desc
        // STRAVA arg
        // VOLUME arg
        // SPEED <null>
        // MUTE <null>
        String name = "";
        String desc = "";

        // because the arguments may be long, put PROMPT in the desc
        if( command.equals("PROMPT") ) {
            name = command;
            desc = argument;
        } else {
            // e.g. START 11.0, VOLUME 13, SPEED <null>
            name = command + (argument.isEmpty() ? "" : " " + argument);
        }

        Waypoint wp = new Waypoint(name, desc, location);

        wp.setCommand(command);
        wp.setArgument(argument);

        if(mWaypoints == null)
        {
           mWaypoints = new ArrayList<Waypoint>();
        }
        mWaypoints.add(wp);

        mIsDirty = true;
    }

    public boolean IsDirtyGPX() { return mIsDirty; }
    public void SetDirtyGPX(boolean val) { mIsDirty = val; }

    private PendingIntent getLocationPendingIntent(boolean shouldCreate) {

        Intent broadcast = new Intent(ACTION_LOCATION);
        int flags = shouldCreate ? 0 : PendingIntent.FLAG_NO_CREATE;
        if(mAppContext != null) {
            PendingIntent pi = PendingIntent.getBroadcast(mAppContext, 0, broadcast, flags);
            Log.d(Constants.APP.TAG, "RunManger::getLocationPendingIntent ACTION_LOCATION");
            return pi;
        } else {
            Log.d(Constants.APP.TAG, "RunManger::getLocationPendingIntent mAppContext is NULL!");
            return null;
        }

    }

    Location GetLastKnowLocation() {
        Location lastKnown = null;
        if (mCurrentLocation != null) {
            return mCurrentLocation;
        } else {
            if (mLocationManager != null) {
                lastKnown = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                if (lastKnown != null) {
                    // reset the time to now
                    lastKnown.setTime(System.currentTimeMillis());
                    mCurrentLocation = lastKnown;
                }
            }
        }
        return lastKnown;
    }

    public void startLocationProviderService() {

        List<String> matchingProviders = mLocationManager.getAllProviders();
        Location bestResult;
        long bestTime = 9999;
        float bestAccuracy = 10;

        for (String provider: matchingProviders) {
            Location location = mLocationManager.getLastKnownLocation(provider);
            if (location != null) {
                float accuracy = location.getAccuracy();
                long time = location.getTime();
                Log.d(Constants.APP.TAG,"accuracy = " + accuracy);
                Log.d(Constants.APP.TAG,"time = " + time);

                if ((time > 10 && accuracy < 15)) {
                    bestResult = location;
                    bestAccuracy = accuracy;
                    bestTime = time;
                }
                else if (time < 10 &&
                        bestAccuracy == Float.MAX_VALUE && time > bestTime){
                    bestResult = location;
                    bestTime = time;
                }
            }
        }

        String provider = LocationManager.GPS_PROVIDER;
        // if we have the test provider and it's enabled, use it
    //   if (mLocationManager.getProvider(TEST_PROVIDER) != null &&
      //          mLocationManager.isProviderEnabled(TEST_PROVIDER)) {
        //    provider = TEST_PROVIDER;
        //}

        Log.d(Constants.APP.TAG, "Using provider " + provider);

        // get the last known location and broadcast it if we have one
        Location lastKnown = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
        if (lastKnown != null) {
            // reset the time to now
            lastKnown.setTime(System.currentTimeMillis());
            broadcastLocation(lastKnown);
        }

        // start updates from the location manager
        PendingIntent pi = getLocationPendingIntent(true);
        if(pi != null && provider != null) {
            mLocationManager.requestLocationUpdates(provider, 0, 0, pi);
        }
    }

    public void stopLocationUpdates() {
        PendingIntent pi = getLocationPendingIntent(false);
        if (pi != null) {
            mLocationManager.removeUpdates(pi);
            pi.cancel();
        }
    }

    public boolean isTrackingLocations() {
        return getLocationPendingIntent(false) != null;
    }

    public boolean isTrackingCurrentRun() {
        return mCurrentRide != null;
    }

    public boolean isTrackingRun(Ride ride) {
        if(ride != null) {
            Log.d(Constants.APP.TAG, "ride.getId() =" + ride.GetId());
        }

         return ride != null && ride.GetId() == mCurrentRunId;
    }

    private void broadcastLocation(Location location) {
        Log.d("Bike Badger","RideManager::broadcastLocation KEY_LOCATION_CHANGED");
        Intent broadcast = new Intent(ACTION_LOCATION);
        broadcast.putExtra(LocationManager.KEY_LOCATION_CHANGED, location);
        mAppContext.sendBroadcast(broadcast);
    }

    public void OpenGPXFile(String path)
    {
        File gpxFile = null;
        File externalAppsDirectory = Constants.APP.EXTERNAL_APP_DIR;
        if(!externalAppsDirectory.exists()) {
            externalAppsDirectory.mkdirs(); //make if not exist
        }

        gpxFile = new File(path);

        // check size
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) mAppContext.getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long availableKilos = mi.availMem / 1024; // kilobytes
        Log.d(Constants.APP.TAG, "availableMegs =" + availableKilos);

        long fileSize = gpxFile.length() / 1024; // kilobytes

        if(fileSize > availableKilos) {
            Log.d(Constants.APP.TAG, "Out of memory error soon: filesize = " + fileSize + "availableKilos = " + availableKilos);
            MyToast.Show(mAppContext, "File too big to load!", Color.RED);
            return; // bail
        }

       if(LoadWaypoints(gpxFile))
       {
           mPrefsEditor.putString(RideManager.PREF_GPX_PATH, gpxFile.getAbsolutePath());
           mPrefsEditor.commit();
           mIsDirty = false;
           mCurrentGPXPath = gpxFile.getAbsolutePath();

           // TODO
           // This may not work out in the long run
           // Asych load the same file for it's tracks
           // The thought is we need the waypoints loaded now but we have time to load the tracks...
           GPXTrackParser gr = new GPXTrackParser(this);
           gr.execute(gpxFile);
       }

        mPolylineOptions = null; // reset the polyline so when we bring up the map it will load
    }


    public boolean LoadWaypoints(File gpxFile) {

        try {
            GPXWaypointParser jdom = new GPXWaypointParser();
            //      jdom.progressDialog = progressDialog;
            GPX gpx = jdom.parse(gpxFile);

            // we may have already loaded a set of waypoint. Clear them to replace...
            if(mWaypoints != null) {
                mWaypoints.clear();
            }

            mWaypoints = gpx.getWaypoints();
            //MyToast.Show(mAppContext, "\"" + gpxFile.getName() + "\" loaded (" + mWaypoints.size() + ")", Color.BLACK);
            Log.i(getClass().getCanonicalName(), String.format("%d waypoints loaded.", mWaypoints.size()));
        } catch (ParsingException e) {
            Log.e(getClass().getCanonicalName(), String.format("Error parsing gpx file: %s", "ridebud.gpx"));
            e.printStackTrace();
            return false;
        }
        return true;
    }



    public void ExportWaypointsTrack() {
        File gpxOut = new File(mCurrentGPXPath);
        File gpxOutSaftyNet = new File(mCurrentGPXPath +".out");
        try {
            FileWriter fw = new FileWriter(gpxOutSaftyNet);
            WaypointsTracksToGPX wptgpx = new WaypointsTracksToGPX();
            wptgpx.Export(mWaypoints, mTrkpts, fw);
            gpxOutSaftyNet.renameTo(gpxOut);
            mIsDirty = false;
        } catch (IOException ioe)
        {
            Log.d(Constants.APP.TAG, "Failed to save GPX file");
            MyToast.Show(mAppContext, "Error saving \"" + gpxOut.getName() + "\"! Check \"" + gpxOutSaftyNet.getName() + "\"", Color.RED);
        }
    }

    public boolean WaypointsLoaded() {
        return mWaypoints != null /*&& !mWaypoints.isEmpty() */;
    }

    public Ride startNewRun() {

       // LoadWaypoints();
        mCurrentRide = createRun();

        // start tracking the run
        startTrackingRun(mCurrentRide);
        return mCurrentRide;

    }

    public void startTrackingRun(Ride ride) {

        // RideManager starts the Location and Timer SERVICES
        // Let the RideFragment UI start receiving their updates...

        // keep the ID
        mCurrentRunId = ride.GetId();
        // store it in shared preferences
        mPrefs.edit().putLong(PREF_CURRENT_RUN_ID, mCurrentRunId).commit();

        // Start the timer service
        Intent serviceIntent = new Intent(mAppContext, TimerService.class);
        serviceIntent.putExtra("intervalInSeconds", mBadgerIntervalSeconds);
        mAppContext.startService(serviceIntent);

        // start location updates
        startLocationProviderService();
    }

    public void stopRun() {
        stopLocationUpdates();
        mCurrentRunId = -1;
        mPrefs.edit().remove(PREF_CURRENT_RUN_ID).commit();
        mAppContext.stopService(new Intent(mAppContext, TimerService.class));
    }

    private Ride createRun() {
        Ride ride = new Ride(); // provide the default target speed across the entire ride
        ride.SetId(mHelper.insertRun(ride));
        ride.SetTargetAvgSpeed(mDefaultTargetAvgSpeed);
        return ride;
    }

    public RunDatabaseHelper.RunCursor queryRuns() {
        return mHelper.queryRuns();
    }

    public Ride getRun(long id) {
        Ride ride = null;
        RunDatabaseHelper.RunCursor cursor = mHelper.queryRun(id);
        cursor.moveToFirst();
        // if we got a row, get a ride
        if (!cursor.isAfterLast())
            ride = cursor.getRun();
        cursor.close();
        return ride;
    }

    public void insertLocation(Location loc) {
        Log.d(Constants.APP.TAG,"RideManager::insertLocation");
        if (mCurrentRunId != -1) {
            if ((mHelper != null) && (mCurrentRide != null) && (loc != null) && loc.hasSpeed()) {
                double avgSpeed;
                // update the average
                avgSpeed = mCurrentRide.UpdateAverageSpeed(loc.getSpeed() * Ride.METERS_TO_MILES); // in ft
                mHelper.insertLocation(mCurrentRunId, loc);
            }
        } else {
            Log.e(Constants.APP.TAG, "Location received with no tracking run; ignoring.");
        }
    }

    public Location getLastLocationForRun(long runId) {
        Location location = null;
        RunDatabaseHelper.LocationCursor cursor = mHelper.queryLastLocationForRun(runId);
        cursor.moveToFirst();
        // if we got a row, get a location
        if (!cursor.isAfterLast())
            location = cursor.getLocation();
        cursor.close();
        return location;
    }

    public ArrayList<Waypoint> GetWaypoints() {
        return mWaypoints;
    }

    // Core of the Action Waypoints. Check to see if we trigger a waypoint based on location
    public void ProcessProximityActions(Location loc) {
        double lat = (double) Math.round(loc.getLatitude() * 1000000) / 1000000;
        double lng = (double) Math.round(loc.getLongitude() * 1000000) / 1000000;
        Location curLocation = loc;

        double dist = 0;
        double bearing = 0;
        mClosestDistance = 99999999;

        Waypoint closestWpt = null;
        // got to a state where there where no waypoints but still receiving locations
        if(mWaypoints == null || mWaypoints.size() < 1)
        {
            Log.d(Constants.APP.TAG, "ProcessProximityActions::mWaypoints is null! Skipping.");
            return; // bail
        }

        for (int idx = 0; idx < (mWaypoints.size()); idx++) {
            // compare each waypoint's distance with that of the current location. The closest is triggered.


            //Log.d(Constants.APP.TAG, "mWaypoints.get(" + idx + ")=" +  mWaypoints.get(idx));
            final Waypoint waypoint = mWaypoints.get(idx);
            double lat2 = (waypoint.getLatitude() * 1000000) / 1000000;
            double lng2 = (waypoint.getLongitude() * 1000000) / 1000000;
            double results[] = { 0, 0 , 0 };
            LatLngUtils.computeDistanceAndBearing(lat, lng, lat2, lng2, results);
            dist = results[0] * 3.2808; // feet
            bearing = results[2]; // final bearing
            if (mOkayToLaunch) {
                 //Log.d(Constants.APP.TAG, "ProcessProximityActions:: waypoint=" + mWaypoints.get(idx).getName() + ", dist=" + dist);
                if (dist < mProximityAccuracy) // proximity in feet
                {
                    Log.d(Constants.APP.TAG, "Trigger ProcessProximityActions:: waypoint=" + waypoint.getName() + "?");

                    if (!waypoint.IsTriggered()) {
                        if (waypoint.getCommand().equals("SPEED") || waypoint.getCommand().equals("STATUS") ) {
                            mWaypoints.get(idx).SetTriggered(true);
                            SpeakCurrentSpeed(curLocation);
                        } else if (waypoint.getCommand().equals("TASKER") || waypoint.getCommand().equals("TASK")) {
                            mWaypoints.get(idx).SetTriggered(true);
                            if ( TaskerIntent.testStatus(mAppContext).equals( TaskerIntent.Status.OK ) ) {
                                TaskerIntent i = new TaskerIntent( waypoint.getArgument() );
                                mAppContext.sendBroadcast(i);
                            }
                        } else if (waypoint.getCommand().equals("PLAYLIST") || waypoint.getCommand().equals("PLAY")) {
                            mWaypoints.get(idx).SetTriggered(true);

                           // mOkayToLaunch = false;
                            //new Timer().schedule(new TimerTask() {
                              //  @Override
                                //public void run() {
                                 //   mOkayToLaunch = true;
                                //}
                            //}, 1000 * 60);

                            if(GoogleMusicUtils.ContainsPlaylist(mAppContext, waypoint.getArgument()) ) {
                                   GoogleMusicUtils.PlayPLaylist(mAppContext, waypoint.getArgument() );
                            } else if(PowerAmpUtils.ContainsPlaylist(mAppContext, waypoint.getArgument() )) {
                                PowerAmpUtils.PlayPlaylist(mAppContext, waypoint.getArgument() );

                            //LaunchSpotifyPlayFromSearch(wptName);
                            }
                        } else if (waypoint.getCommand().equals("PROMPT")) {
                                mWaypoints.get(idx).SetTriggered(true);
                                if(!waypoint.getArgument().isEmpty() ) {
                                    SpeakWords(waypoint.getArgument());
                                } else if (!waypoint.getDesc().isEmpty() ) {
                                    SpeakWords(waypoint.getDesc());
                                }
                        } else if (waypoint.getCommand().equals("STRAVA")) {
                                mWaypoints.get(idx).SetTriggered(true);
                                Intent stravaIntent = new Intent("android.intent.action.RUN");
                                Uri uriData = Uri.parse("http://strava.com/nfc/record");
                                if (! waypoint.getArgument().isEmpty()  && waypoint.getArgument().equals("STOP")) {
                                    uriData = Uri.parse("http://strava.com/nfc/record/stop");
                                }
                                    stravaIntent.setData(uriData);
                                    stravaIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
                                    if (stravaIntent.resolveActivity(mAppContext.getPackageManager()) != null) {
                                        mAppContext.startActivity(stravaIntent);
                                    }

                        } else if (waypoint.getCommand().equals("VOLUME")) {
                            mWaypoints.get(idx).SetTriggered(true);
                            if (!waypoint.getArgument().isEmpty()) {
                                int vol = Integer.parseInt(waypoint.getArgument()) ;
                                SetVolume(vol);
                            }
                            } else if (waypoint.getCommand().equals("QUIET") || waypoint.getCommand().equals("MUTE") ) {
                                mWaypoints.get(idx).SetTriggered(true);
                                QuietMusic();
                        } else if (waypoint.getCommand().equals("LAP") || waypoint.getCommand().equals("START")) {
                            mWaypoints.get(idx).SetTriggered(true);
                            if (!waypoint.getArgument().isEmpty()) {
                                mCurrentRide.SetTargetAvgSpeed(waypoint.getArgument(), mDefaultTargetAvgSpeed);
                                SpeakWords(String.format("Target speed set to %.1f miles per hour.", mCurrentRide.GetTargetAvgSpeed()));
                                mCurrentRide.ResetAverageSpeed(); // clear the current averageSpeed
                            }
                        }
                    } // if not triggered
                } else {
                    // distance between location and waypoint is out of bounds. Rest Triggered...
                    mWaypoints.get(idx).SetTriggered(false);
                }
            } // okay to launch

            // swap out the closest wpt
            if (dist < mClosestDistance) {
                mClosestWaypoint = mWaypoints.get(idx);
                mClosestDistance = dist;
                mClosestBearing = bearing;
            }
        }

    }

    public void SpeakAverageSpeed() {
        double avgSpeed = mCurrentRide.GetAverageSpeed(); // in feet
        SpeakWords(String.format("Average speed is %.1f miles per hour.", avgSpeed));
    }

    public void BadgerSpeak()
    {
        Log.d(Constants.APP.TAG, "BadgerSpeak");
        if(mCurrentRide == null)
            return; // bail

        double avgSpeed = mCurrentRide.GetAverageSpeed(); // in feet
        double behindTargetSpeed;
        if (mBadger /* && avgSpeed > 0 */) {
            if(mBadgerIncludeAvgSpeed) {
                SpeakWords(String.format("Average speed is %.1f miles per hour.", avgSpeed));
            }

            behindTargetSpeed = mCurrentRide.BehindTargetSpeed();
            if (behindTargetSpeed > 0) {
                if(mBadgerLevel == BADGER_LEVEL_SIMPLE)
                    SpeakWords(mBehindPhrase);
                else if(mBadgerLevel == BADGER_LEVEL_DETAILED)
                    SpeakWords(String.format("Behind %.1f miles per hour.", behindTargetSpeed));
                else if(mBadgerLevel == BADGER_LEVEL_VERBOSE) {
                    SpeakWords(String.format("Behind %.1f miles per hour.", behindTargetSpeed));
                    SpeakWords(mBehindPhrase);
                }
            } else {
                // On Track

                double deltaSpeed = Math.abs(behindTargetSpeed);
                //SpeakWords(String.format("Ahead %.1f miles per hour.", deltaSpeed));
                if(mBadgerLevel == BADGER_LEVEL_SIMPLE)
                    SpeakWords(mAheadPhrase);
                else if(mBadgerLevel == BADGER_LEVEL_DETAILED) {
                    SpeakWords(String.format("Ahead %.1f miles per hour.", deltaSpeed));
                }
                else if(mBadgerLevel == BADGER_LEVEL_VERBOSE) {
                    SpeakWords(String.format("Ahead %.1f miles per hour.", deltaSpeed));
                    SpeakWords(mAheadPhrase);
                }
            }
        }
    }

    public void SpeakCurrentSpeed(Location location) {
        if (location != null && location.hasSpeed()) {
            double speed = (location.getSpeed() * Ride.METERS_TO_MILES);
            SpeakWords(String.format("Speed is %.1f miles per hour.", speed));
        }
        SpeakAverageSpeed();
   }

    public double GetBearingBetweenLocations(Location first, Location second)
    {
        double first_lat = first.getLatitude();
        double second_lat = second.getLatitude();
        double first_lon = first.getLongitude();
        double second_lon = second.getLongitude();

        double dLon = (second_lon - first_lon);
        double y = Math.sin(dLon) * Math.cos(second_lat);
        double x = Math.cos(first_lat)*Math.sin(second_lat) - Math.sin(first_lat)*Math.cos(second_lat)*Math.cos(dLon);
        double brng = Math.toDegrees((Math.atan2(y, x)));
        brng = (360 - ((brng + 360) % 360));
        return brng;
    }

    public void SpeakWords(String speech) {
        //speak straight away
        //String defaultEngine = mTTS.getDefaultEngine();
        if (mTTS != null && speech != null && !speech.isEmpty() ) {
            mTTS.speak(speech, TextToSpeech.QUEUE_ADD, null);
        }
    }

    public boolean IsStartNow() { return mStartNow; }
    public void PlotCurrentLocation(Location loc) {

        if (mMap == null)
            return; // bail

        //double[] d = getlocation();
        double lat = (double) Math.round(loc.getLatitude() * 1000000) / 1000000;
        double lng = (double) Math.round(loc.getLongitude() * 1000000) / 1000000;

        if (mCurrentMarker != null) {
            // mCurrentMarker.setIcon(BitmapDescriptorFactory
            //         .fromResource(R.drawable.dot_green));
            mCurrentMarker.setVisible(false);
        }

        mCurrentMarker = mMap.addMarker(new MarkerOptions()
                .position(new LatLng(lat, lng))
                .title("Current Location")
                .icon(BitmapDescriptorFactory
                        .fromResource(R.drawable.dot_blue)));

        mMap
                .animateCamera(CameraUpdateFactory.newLatLngZoom(
                        new LatLng(lat, lng), 14));
    }

    public void Shutdown()
    {
        Log.d(Constants.APP.TAG, "RideManager::Shutdown");
        stopRun();
        stopLocationUpdates();

        // kill everything
        android.os.Process.killProcess(android.os.Process.myPid());

       /*
        if(mMap != null) {
            mMap.clear();
            mMap = null;
        }
        if(mWaypoints != null)
            mWaypoints = null;
        if(mTTS != null) {
            mTTS.shutdown();
            mTTS = null;
        }
    */

    }

    double GetDefaultTargetAvgSpeed()
    {
        return mDefaultTargetAvgSpeed;
    }

   public void LaunchSpotifyPlayFromSearch(String query) {
        try {
            final Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.setAction(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
            intent.setComponent(new ComponentName("com.spotify.music", "com.spotify.music.MainActivity"));
            intent.putExtra(SearchManager.QUERY, query);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // not calling it from an activity
            mAppContext.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            Log.e(Constants.APP.TAG, "Error searching Spotify w/ query '" + query +"'");
            e.printStackTrace();
        }
    }

    public void QuietMusic()
    {
        AudioManager audioManager =
                (AudioManager)mAppContext.getSystemService(Context.AUDIO_SERVICE);

        int maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        int currVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currVol - 5, AudioManager.FLAG_PLAY_SOUND);

       // mAppContext.startService(new Intent(PowerampAPI.ACTION_API_COMMAND).putExtra(PowerampAPI.COMMAND, PowerampAPI.Commands.PAUSE));
    }

    public void SetVolume(int vol)
    {
        AudioManager mAudioManager = (AudioManager) mAppContext.getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_SHOW_UI);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_PLAY_SOUND);
        mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, vol, AudioManager.FLAG_VIBRATE);
    }
}
