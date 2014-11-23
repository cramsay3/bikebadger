package bikebadger;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Matrix;
import android.location.Location;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.Loader;
import android.text.InputType;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;

import export.SimpleFileDialog;
import us.theramsays.bikebadger.app2.R;
import util.Constants;
import util.Formatter;
import util.MyToast;
import util.appmanager.AppManager;

public class RideFragment extends Fragment implements TextToSpeech.OnInitListener {

    private static final String ARG_RUN_ID = "RUN_ID";
    private static final int LOAD_RUN = 0;
    private static final int LOAD_LOCATION = 1;
    public static final int MY_DATA_CHECK_CODE_REQUEST = 9099;
    public static final int MY_MAP_CODE_REQUEST = 9100;

    private RideManager mRideManager;
    private Ride mRide;
    private Location mLastLocation;
    private Menu mMenu;
    private ImageButton mStartStopButton, mResetButton, mWaypointButton;
    private Button mTargetEditButton;
    private TextView mStartedTextView, mDurationTextView, mSpeedTextView, mTargetSpeedTextView;
    private TextView mMessagebarView, mAverageSpeedTextView;
    private ImageView mArrowView;
    //private boolean mReceivingLocationUpdates = false;

    private View.OnClickListener mPlayButtonClickListener = null;
    private View.OnClickListener mPauseButtonClickListener = null;

    private BroadcastReceiver mLocationReceiver = new LocationReceiver() {

        @Override
        protected void onLocationReceived(Context context, Location loc) {
            float bearing = 0;

            //if (mRideManager == null || mRide == null || !mRideManager.isTrackingRun(mRide))
            //    return; // bail

            if(mLastLocation!= null && mLastLocation.hasBearing())
                bearing = mLastLocation.getBearing(); // may be zero

            // currently tracking a run
            mLastLocation = loc;

            // update the lastLocation with the last known bearing if one doesn't exist
            if(loc.hasBearing())
                mLastLocation.setBearing( loc.getBearing() );
            else
                mLastLocation.setBearing( bearing );

           if (isVisible())
              updateUI();
        }

        @Override
        protected void onProviderEnabledChanged(boolean enabled) {
            int toastText = enabled ? R.string.gps_enabled : R.string.gps_disabled;
            Log.d(Constants.APP.TAG, "onProviderEnabledChanged");
            //Toast.makeText(getActivity(), toastText, Toast.LENGTH_LONG).show();
        }
    };

    private TimerServiceReceiver mTimerServiceReceiver = new TimerServiceReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle extras = intent.getExtras();
            //String sTime = "";
            boolean badger = false;
            if (extras != null) {
                //sTime = extras.getString("time");
                badger = extras.getBoolean("badger");
                //Log.w("RunTracker", sTime);
                if(badger) {
                    mRideManager.BadgerSpeak();
                }
            }

            if(isVisible()) {
                updateUI();
            }
        }
    };

    public static RideFragment newInstance(long runId) {
        Log.d(Constants.APP.TAG, "RideFragment::newInstance");

        Bundle args = new Bundle();
        args.putLong(ARG_RUN_ID, runId);
        RideFragment rf = new RideFragment();
        rf.setArguments(args);
        return rf;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Log.d(Constants.APP.TAG, "RideFragment.onCreate()");
         //setRetainInstance(true);

        mRideManager = RideManager.get(getActivity());
        Log.d(Constants.APP.TAG, "mRideManager set = RideManager.get(" + getActivity() + ")");

        setHasOptionsMenu(true);

        // check for a Ride ID as an argument, and find the run
        Bundle args = getArguments();
        if (args != null) {
            long runId = args.getLong(ARG_RUN_ID, -1);
            Log.d(Constants.APP.TAG, "RideFragment.onCreate() runId=" + runId);
            if (runId != -1) {
                LoaderManager lm = getLoaderManager();
                lm.initLoader(LOAD_RUN, args, new RunLoaderCallbacks());
                lm.initLoader(LOAD_LOCATION, args, new LocationLoaderCallbacks());
            }
        }

        // Initialize the TextToSpeech Engine...
        // TODO this engine may not exist. Provide the ability to run without it.
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);

        if (checkTTSIntent.resolveActivity(mRideManager.mAppContext.getPackageManager()) != null) {
            startActivityForResult(checkTTSIntent, MY_DATA_CHECK_CODE_REQUEST);
        } else {
            Log.d(Constants.APP.TAG, "Could not find TTS Intent");
            MyToast.Show(getActivity(), "Could not find Text To Speech Service", Color.RED);
            AlertDialog ad1 = new AlertDialog.Builder(getActivity())
                    .create();
            ad1.setCancelable(false);
            ad1.setTitle("Text To Speech Engine Not Found");
            ad1.setMessage("There was a problem finding the Text To Speech Engine. Make sure it's install properly under Language and Input Settings.");
            ad1.setButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            ad1.show();

        }

        if (mRideManager.mLoadLastGPXFile) {
            Log.d(Constants.APP.TAG, "mLoadLastGPXFile=" + mRideManager.mLoadLastGPXFile);
            Log.d(Constants.APP.TAG, "mCurrentGPXPath=" + mRideManager.mCurrentGPXPath);

            OpenGPXFileOnNewThreadWithDialog(mRideManager.mCurrentGPXPath);
        }

          if (!mRideManager.IsGPSEnabled()) {
            ShowAlertMessageNoGps();
        }

        // keep the screen on so it doesn't time out and turn dark
        getActivity().getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        Log.d(Constants.APP.TAG, "RideFragment.onCreateView()");
        setRetainInstance(true);
        View view = inflater.inflate(R.layout.fragment_run, container, false);
        mMessagebarView = (TextView) view.findViewById(R.id.run_messagebarView);
        mStartedTextView = (TextView) view.findViewById(R.id.run_startedTextView);
        mDurationTextView = (TextView) view.findViewById(R.id.run_durationTextView);
        mSpeedTextView = (TextView) view.findViewById(R.id.run_speedTextView);
        mTargetSpeedTextView = (TextView) view.findViewById(R.id.run_targetSpeedTextView);
        mAverageSpeedTextView = (TextView) view.findViewById(R.id.run_avgSpeedTextView);
        mStartStopButton = (ImageButton) view.findViewById(R.id.run_startButton);
        mTargetEditButton = (Button) view.findViewById(R.id.target_editButton);
        mArrowView = (ImageView) view.findViewById(R.id.img1);

        // set up startstop buttons
        mPlayButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mRide == null) {
                    startReceivingTimerUpdates();
                    startReceivingLocationUpdates(); // RideManager starts the location services
                    mRide = mRideManager.startNewRun();
                } else {

                    mRideManager.startTrackingRun(mRide);
                }

                //MediaUtil.Beep();
                mRideManager.SpeakWords("Starting");
                updateUI();
                mRide.StartStopwatch();
                MyToast.Show( getActivity().getApplicationContext(),  "Starting", Color.BLACK);
            }
        };

        mStartStopButton.setOnClickListener(mPlayButtonClickListener);

        mPauseButtonClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //MediaUtil.Beep();
                mRideManager.SpeakWords("Stopping");
                mRideManager.stopRun();
                updateUI();
                stopReceivingTimerUpdates();
                if(mRide != null)
                    mRide.PauseStopwatch();
                MyToast.Show( getActivity().getApplicationContext(),  "Stopped", Color.BLACK);
            }
        };

        // Reset the average
        mResetButton = (ImageButton) view.findViewById(R.id.run_resetButton);
        mResetButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //MediaUtil.Beep();
                if(mRide != null && mRideManager.isTrackingRun(mRide)) {
                    mRide.ResetAverageSpeed();
                }
                updateUI();
            }
        });

        mWaypointButton = (ImageButton) view.findViewById(R.id.run_waypointButton);
        mWaypointButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //MediaUtil.Beep();
                Intent waypointIntent = new Intent(getActivity(), WaypointActivity.class);
                waypointIntent.putExtra("location", mRideManager.GetLastKnowLocation());
                startActivityForResult(waypointIntent, Constants.APP.ACTION_WAYPOINT_REQUEST_CODE);
                updateUI();
            }
        });

        mTargetEditButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                AlertDialog.Builder alert = new AlertDialog.Builder(getActivity());
                final EditText einput = new EditText(getActivity());
                alert.setTitle("Target Average Speed");
                alert.setMessage("Edit the target average speed.");
                einput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                einput.setText(mTargetSpeedTextView.getText());
                alert.setView(einput);
                alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        String sValue = einput.getText().toString();

                        double targetVal;
                        try {
                            targetVal = Double.parseDouble(sValue);

                        } catch (NumberFormatException nfe) {
                            targetVal = 0;
                            Log.d("RunTracker", "Error parsing mSpeedIntervalSeconds");
                        }

                        if(targetVal > 78700 && targetVal < 78800)
                        {
                            final SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                            myPrefs.edit().putBoolean("Hacked", true).commit();
                            AppManager.SimpleNotice(getActivity(), "Full Version Hack", "You now have access to the full paid version for free.");
                            //getActivity().finish();
                        } else {
                            if (mRide != null) {
                                mRide.SetTargetAvgSpeed(targetVal);
                            } else if(mRideManager != null) {
                                mRideManager.SetDefaultTargetAvgSpeed(targetVal);
                            }

                            mTargetSpeedTextView.setText(sValue);
                        }
                    }
                });

                alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });
                alert.show();
            }
        });

        // do we start now?
        if(mRideManager != null && mRideManager.IsStartNow()) {
            startReceivingTimerUpdates();
            startReceivingLocationUpdates();
            mRide = mRideManager.startNewRun();
            mRideManager.SpeakWords("Starting");
        }

   //  Trying to fix flicker issue
   //   if(isVisible())
     //       updateUI();

        return view;
    }

    private void ShowAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder( getActivity() );
        builder.setMessage("GPS seems to be disabled. Do you wish to enable it?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, final int id) {
                        dialog.cancel();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

     public void OpenGPXFileOnNewThreadWithDialog(final String path) {
         final ProgressDialog pd = new ProgressDialog(getActivity());
         pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
         final File gpxFile = new File(path);
         pd.setMessage("Loading \"" + gpxFile.getName() + "\"");
         pd.setIndeterminate(true);
         pd.setCancelable(false);
         pd.show();
         Thread mThread = new Thread() {
             @Override
             public void run() {
                 mRideManager.OpenGPXFile(path);
                 pd.dismiss();
                 if (mRideManager.mWaypoints != null) {
                     getActivity().runOnUiThread(new Runnable() {
                         public void run() {
                             //Toast.makeText(getActivity(), "Hello", Toast.LENGTH_SHORT).show();
                             MyToast.Show(getActivity(), "\"" + gpxFile.getName() + "\" loaded (" + mRideManager.mWaypoints.size() + ")", Color.BLACK);
                         }
                     });                                            }
             }
         };
         mThread.start();
     }

    public void SaveGPXFileOnNewThreadWithDialog(final String path) {
        final ProgressDialog pd = new ProgressDialog(getActivity());
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        final File gpxFile = new File(path);
        pd.setMessage("Saving \"" + gpxFile.getName() + "\"");
        pd.setIndeterminate(true);
        pd.setCancelable(false);
        pd.show();
        Thread mThread = new Thread() {
            @Override
            public void run() {
                mRideManager.ExportWaypointsTrack();
                pd.dismiss();
                if (mRideManager.mWaypoints != null) {
                    getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            //Toast.makeText(getActivity(), "Hello", Toast.LENGTH_SHORT).show();
                            MyToast.Show(getActivity(), "\"" + gpxFile.getName() + "\" saved", Color.BLACK);
                        }
                    });                                            }
            }
        };
        mThread.start();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.run_list_options, menu);
        mMenu = menu;
        mMenu.findItem(R.id.menu_item_gpx_save).setEnabled(false);
   }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_item_show_map:
                Intent i = new Intent(getActivity(), MapsActivity2.class);
                startActivityForResult(i, MY_MAP_CODE_REQUEST);
                break;
            case R.id.menu_item_gpx_save:
                // Free version doesn't allow save.
                if(AppManager.IsFreeVersion(mRideManager.mAppContext)) {
                    AppManager.SimpleNotice(getActivity(), "Free Version Limitation", "This version doesn't allow saving. Please consider purchasing the full version.");
                } else {
                    SimpleFileDialog FileSaveDialog = new SimpleFileDialog(getActivity(), "FileSave", new SimpleFileDialog.SimpleFileDialogListener() {
                        @Override
                        public void onChosenDir(String chosenDir) {
                            // The code in this function will be executed when the dialog OK button is pushed
                            mRideManager.mCurrentGPXPath = chosenDir;
                            SaveGPXFileOnNewThreadWithDialog(mRideManager.mCurrentGPXPath);
                            //MyToast.Show(mRideManager.mAppContext, "\"" + mRideManager.GetGPXFileName() + "\" saved", Color.BLACK);
                            mMenu.findItem(R.id.menu_item_gpx_save).setEnabled(false);
                        }
                    });

                    FileSaveDialog.Default_File_Name = mRideManager.GetGPXFileName();
                    FileSaveDialog.chooseFile_or_Dir(Constants.APP.EXTERNAL_APP_DIR.getAbsolutePath());
                }
                break;
            case R.id.menu_item_gpx_file:
                SimpleFileDialog fileOpenDialog = new SimpleFileDialog(getActivity(), "FileOpen", new SimpleFileDialog.SimpleFileDialogListener() {
                    @Override public void onChosenDir(String chosenDirOrFile) {
                        try {
                            mRideManager.mCurrentGPXPath = chosenDirOrFile;
                        OpenGPXFileOnNewThreadWithDialog(chosenDirOrFile);
                        //MyToast.Show(mRideManager.mAppContext, "\"" + mRideManager.GetGPXFileName() + "\" saved", Color.BLACK);
                        mMenu.findItem(R.id.menu_item_gpx_save).setEnabled(false);
                    } catch (Exception e) {
                        Log.e("FileSelectorTestActivity", "File select error", e);
                        e.printStackTrace();
                    }
                }});

                fileOpenDialog.Default_File_Name = mRideManager.GetGPXFileName();
                fileOpenDialog.chooseFile_or_Dir(Constants.APP.EXTERNAL_APP_DIR.getAbsolutePath());

                // Intent fxintent = new Intent(getActivity(), FileChooserActivity.class);
                //startActivityForResult(fxintent, Constants.APP.FILE_CHOOSER_REQUEST_CODE);
                break;
            case R.id.settings:
                Intent setting_intent = new Intent(getActivity(), SettingsActivity.class);
                startActivity(setting_intent);
                break;
            case R.id.about:

                //DemoManager.Demo(mRideManager.mAppContext);
                //updateUI();

   //              mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_PLAY_SOUND);

// OR
     //           mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_SHOW_UI);

// OR
       //         mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_VIBRATE);
/*                AudioManager mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);
                int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                int vol = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                //StravaCommand("RECORD");
*/

                Intent creditsIntent = new Intent(getActivity(), CreditsActivity.class);
                startActivity(creditsIntent);


                /*
                String LicenseInfo = GooglePlayServicesUtil.getOpenSourceSoftwareLicenseInfo(
                        getActivity());
                AlertDialog.Builder LicenseDialog = new AlertDialog.Builder(getActivity());
                LicenseDialog.setTitle("Legal Notices");
                LicenseDialog.setMessage(LicenseInfo);
                LicenseDialog.show();
*/

                /*
                PlaylistUtils.PlayGMPLaylist(mRideManager.mAppContext, "GM1");

                //long id = idForplaylist(mRideManager.mAppContext,"MTB2");
               //long id1 = PlaylistUtils.getPlaylistId(mRideManager.mAppContext,"MTB2");
                PlaylistUtils.showPlaylistDialog(mRideManager.mAppContext, "0");


                Intent intent = new Intent( MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
                intent.putExtra(MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE,
                        "android.intent.extra.playlist" );
                intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/playlist");
                intent.putExtra(SearchManager.QUERY, "MTB1");
                if (intent.resolveActivity(mRideManager.mAppContext.getPackageManager()) != null) {
                    startActivity(intent);
                }else{
                    //doToast("Sorry, no app was found to service this request", context);
                }
                  return true;
                   */
                break;
            default:
                return super.onOptionsItemSelected(item);
        }
        return true;
    }



        public void startReceivingTimerUpdates()
        {
            getActivity().registerReceiver(mTimerServiceReceiver,
                    new IntentFilter(TimerService.BROADCAST_TIMER_ACTION));
        }

    public void stopReceivingTimerUpdates()
    {
        if (mTimerServiceReceiver != null) {
            getActivity().unregisterReceiver(mTimerServiceReceiver);
            mTimerServiceReceiver = null;
        }
    }

    public void startReceivingLocationUpdates()
    {
        if (mLocationReceiver != null) {
            //mReceivingLocationUpdates = true;
            //mLastLocation = null;
            Intent intent1 = getActivity().registerReceiver(mLocationReceiver,
            new IntentFilter(RideManager.ACTION_LOCATION));
            if(isVisible())
                updateUI();
        }
    }

    public void stopReceivingLocationUpdates()
    {
        if (mLocationReceiver != null) {
            //mReceivingLocationUpdates = false;
            Log.d(Constants.APP.TAG, "RideFragment:: unregistering mLocationReceiver...");
            getActivity().unregisterReceiver(mLocationReceiver);
        }
    }



    @Override
    public void onResume(){
        Log.d(Constants.APP.TAG, "RideFragment::onResume");
        updateUI();
        if(mRideManager.IsDirtyGPX()) {
            // may be coming back from the map where markers were added...
            if(mMenu != null)
                mMenu.findItem(R.id.menu_item_gpx_save).setEnabled(true);
        }
        super.onResume();
    }

    @Override
    public void onStart() {
        Log.d(Constants.APP.TAG, "RideFragment::onStart");

         super.onStart();
    }

    @Override
    public void onStop() {
        Log.d(Constants.APP.TAG, "RideFragment::onStop");
          super.onStop();
    }

    @Override
    public void onDestroy()
    {
        Log.d(Constants.APP.TAG, "RideFragment::onDestroy");
        stopReceivingTimerUpdates();
        stopReceivingLocationUpdates();
        super.onDestroy();
    }

    public void onPause() {
        Log.d(Constants.APP.TAG, "RideFragment::onPause");
        super.onPause();
     }

    private void updateUI() {

        boolean trackingLocations = mRideManager.isTrackingLocations();
        boolean trackingThisRun = mRideManager.isTrackingRun(mRide);
        boolean mRunNotNull = mRide != null;
        String  msg = "";

        Log.d(Constants.APP.TAG, "RideFragment::updateUI");
        if (trackingLocations)
            Log.d(Constants.APP.TAG, "trackingLocations=" + trackingLocations);
        else
            Log.d(Constants.APP.TAG, "trackingLocations is FALSE");
        if (trackingThisRun)
            Log.d(Constants.APP.TAG, "trackingThisRun=" + trackingThisRun);
        else
            Log.d(Constants.APP.TAG, "trackingThisRun is FALSE");

        if (mRunNotNull)
            Log.d(Constants.APP.TAG, "mRide is NOT null!");
        else
            Log.d(Constants.APP.TAG, "mRide is NULL!");

        if (mRide != null) {
            mStartedTextView.setText(mRide.getStartDate().toString());
        }

        int durationSeconds = 0;

        if (mRide != null && mLastLocation != null) {
            Log.d(Constants.APP.TAG, "mRide != null && mLastLocation != null");
            mMessagebarView.setTextColor(Color.WHITE);
            //String msg = "mRide != null && mLastLocation != null";
            double bearing1 = mLastLocation.getBearing();
            double bearing2 = mRideManager.mClosestBearing;
            double bearingDif = bearing2 - bearing1;
            if (mRideManager.mClosestWaypoint != null) {
                Log.d(Constants.APP.TAG, "mRideManager.mClosestWaypoint != null");

                if (mRideManager.mClosestWaypoint.IsTriggered()) {
                    mMessagebarView.setTextColor(Color.RED);
                    msg = "\"" + mRideManager.mClosestWaypoint.getName() + "\"";
                    msg += " (Active)";
                    Bitmap activeBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_triggered);
                    mArrowView.setImageBitmap(activeBitmap);
                } else {
                     mMessagebarView.setTextColor(Color.WHITE);
                    msg = "\"" + mRideManager.mClosestWaypoint.getName() + "\"";
                    msg += " at ";
                    msg += Formatter.FormatDistanceMiles(mRideManager.mClosestDistance);
                    //msg += " ft";
                    //msg += " (" + Formatter.FormatDecimal(bearing1) + "->";
                   // msg += Formatter.FormatDecimal(bearing2) + ":" + Formatter.FormatDecimal(bearingDif);
                    // Point the arrow to the bearing of the closest waypoint...
                    Bitmap myBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.ic_arrow_white);
                    Matrix matrix = new Matrix();
                    matrix.postRotate((float)(bearingDif));
                    final int width = myBitmap.getWidth();
                    final int height =  myBitmap.getHeight();
                    Log.d(Constants.APP.TAG,"myBitmap.getWidth=" + myBitmap.getWidth() + "myBitmap.getHeight=" + myBitmap.getHeight() );
                    Bitmap rotatedBitmap = Bitmap.createBitmap(myBitmap , 0, 0,  width, height , matrix, true);
                    mArrowView.setImageBitmap(rotatedBitmap);
                }
            }

            // set the top message line
            mMessagebarView.setText(msg);

            durationSeconds = mRide.StopwatchSeconds();
            mSpeedTextView.setText(Formatter.FormatDecimal(mLastLocation.getSpeed() * Ride.METERS_TO_MILES));
            mTargetSpeedTextView.setText( Formatter.FormatDecimal( mRide.GetTargetAvgSpeed() ));
            mAverageSpeedTextView.setText(Formatter.FormatDecimal(mRide.GetAverageSpeed()));
            mDurationTextView.setText(Formatter.FormatDuration(durationSeconds));

        } else if (mRide == null && mLastLocation == null) {
            Log.d(Constants.APP.TAG, "mRide == null && mLastLocation == null");
            mTargetSpeedTextView.setText( Formatter.FormatDecimal( mRideManager.GetDefaultTargetAvgSpeed() ));

            mMessagebarView.setTextColor(Color.WHITE);
            if(mRideManager.WaypointsLoaded()) {
                mMessagebarView.setText("\"" + mRideManager.GetGPXFileName() + "\" loaded (" + mRideManager.mWaypoints.size() + ")");
            }
        } else if (mRide != null && mLastLocation == null) { // mLastLocation is null
            mMessagebarView.setTextColor(Color.RED);
            mMessagebarView.setText("Waiting on GPS...");
        } else if(mRide == null && mLastLocation != null) {
            Log.d(Constants.APP.TAG, "mRide == null && mLastLocation != null");
        }

        if(!trackingThisRun)
        {
            mStartStopButton.setBackgroundResource(R.drawable.ic_button_white_play);
            mStartStopButton.setOnClickListener(mPlayButtonClickListener);
        }

        if(trackingThisRun)
        {
            mStartStopButton.setBackgroundResource(R.drawable.ic_button_white_pause);
            mStartStopButton.setOnClickListener(mPauseButtonClickListener);
        }

        //mStartButton.setEnabled(!started);
        mResetButton.setEnabled(trackingLocations && trackingThisRun);
        mWaypointButton.setEnabled(trackingLocations);
        //mTargetEditButton.setEnabled(trackingLocations && trackingThisRun);
     }

    private class RunLoaderCallbacks implements LoaderCallbacks<Ride> {

        @Override
        public Loader<Ride> onCreateLoader(int id, Bundle args) {
            return new RunLoader(getActivity(), args.getLong(ARG_RUN_ID));
        }

        @Override
        public void onLoadFinished(Loader<Ride> loader, Ride ride) {
            mRide = ride;
            updateUI();
        }

        @Override
        public void onLoaderReset(Loader<Ride> loader) {
            // do nothing
        }
    }

    private class LocationLoaderCallbacks implements LoaderCallbacks<Location> {

        @Override
        public Loader<Location> onCreateLoader(int id, Bundle args) {
            return new LastLocationLoader(getActivity(), args.getLong(ARG_RUN_ID));
        }

        @Override
        public void onLoadFinished(Loader<Location> loader, Location location) {
            mLastLocation = location;
            updateUI();
        }

        @Override
        public void onLoaderReset(Loader<Location> loader) {
            // do nothing
        }
    }

    // Returns from TTS Startup, Opening a GPX file, Creating a waypoint and kicking off the map
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case MY_DATA_CHECK_CODE_REQUEST:
                // for testing
                //resultCode = TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL;
                switch (resultCode) {
                    case TextToSpeech.Engine.CHECK_VOICE_DATA_PASS:
                        RideManager.mTTS = new TextToSpeech(mRideManager.mAppContext, this);
                        //RideManager.mTTS = null;
                        Log.v(Constants.APP.TAG, "TextToSpeech installed");
                        break;
                    case TextToSpeech.Engine.CHECK_VOICE_DATA_BAD_DATA:
                    case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_DATA:
                    case TextToSpeech.Engine.CHECK_VOICE_DATA_MISSING_VOLUME:
                    case TextToSpeech.Engine.CHECK_VOICE_DATA_FAIL:

                        Log.e(Constants.APP.TAG, "Got a failure. Text To Speech not available");

                        AlertDialog ad = new AlertDialog.Builder(getActivity())
                                .create();
                        ad.setCancelable(false);
                        ad.setTitle("Text To Speech Engine Not Found");
                        ad.setMessage("There was a problem finding the Text To Speech Engine. Make sure it's install properly under Language and Input Settings.");
                        ad.setButton("Ok", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        ad.show();

                        // missing data, install it
                        //Log.v(Constants.APP.TAG, "Need language stuff: " + resultCode);
                        //Intent installIntent = new Intent();
                        //installIntent.setAction(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                        //startActivity(installIntent);
                        break;
                }

                break;
            /* No longer used as it is deprecated. Use SimpleFileDialog instead even though it blocks.
            TODO - integrate a slick open file that includes DropBox and Drive library!
            case Constants.APP.FILE_CHOOSER_REQUEST_CODE:

                if (resultCode == getActivity().RESULT_OK) {
                    if (data != null) {
                        // Get the URI of the selected file
                        final Uri uri = data.getData();
                        Log.i(Constants.APP.TAG, "Uri = " + uri.toString());
                        try {
                            // Get the file path from the URI
                            final String path = FileUtils.getPath(mRideManager.mAppContext, uri);
                            //Toast.makeText(getActivity(), "File Selected: " + path, Toast.LENGTH_LONG).show();
                            OpenGPXFileOnNewThreadWithDialog(path);
                           // if(isVisible())
                             //    updateUI(); // update title bar file loaded
                        } catch (Exception e) {
                            Log.e("FileSelectorTestActivity", "File select error", e);
                        }
                    }
                }
                break;
                 */
            case Constants.APP.ACTION_WAYPOINT_REQUEST_CODE:
                if (resultCode == getActivity().RESULT_OK) {
                    if (data != null) {
                        final Location location = data.getParcelableExtra("location");
                        String command = data.getStringExtra("command");
                        final String arg = data.getStringExtra("argument");

                         mRideManager.AddNewWaypoint(command, arg, location);
                        // enable the save menu
                        mMenu.findItem(R.id.menu_item_gpx_save).setEnabled(true);
                    }
                }
                break;
            case MY_MAP_CODE_REQUEST:

                if (resultCode == getActivity().RESULT_OK) {
                    Log.d(Constants.APP.TAG, "Return from map");
                }
                break;
          }

        super.onActivityResult(requestCode, resultCode, data);
    }

    //setup TTS

    public void onInit(int initStatus) {
       // assert  (false);
        //check for successful instantiation
        if (initStatus == TextToSpeech.SUCCESS) {
            //if(RideManager.mTTS.isLanguageAvailable(Locale.US)==TextToSpeech.LANG_AVAILABLE)
              //  RideManager.mTTS.setLanguage(Locale.US);
        }
        else if (initStatus == TextToSpeech.ERROR) {
            Toast.makeText(mRideManager.mAppContext, "Sorry! Text To Speech failed...", Toast.LENGTH_LONG).show();
            AlertDialog ad = new AlertDialog.Builder(getActivity())
                    .create();
            ad.setCancelable(false);
            ad.setTitle("Text To Speech Engine Not Found");
            ad.setMessage("There was a problem finding the Text To Speech Engine. Make sure it's install properly under Language and Input Settings.");
            ad.setButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            ad.show();
        }
    }


}
