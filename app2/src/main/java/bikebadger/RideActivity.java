package bikebadger;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.util.Log;

import util.Constants;
import util.appmanager.AppManager;
import util.appmanager.VersionNotes;

public class RideActivity extends LicenseCheckActivity {
    //private RideManager mRideManager;

    /** A key for passing a run ID as a long */
     public static final String EXTRA_RUN_ID = "RUN_ID";
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d("Bike Badger", "RideActivity::onCreate");
        super.onCreate(savedInstanceState);

        Context context = getApplicationContext();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        AppManager.AppStartEnum startMode = AppManager.CheckAppStart(getApplicationContext(), preferences);
        //startMode = AppManager.AppStartEnum.EXPIRED_FREE;
        //startMode = AppManager.AppStartEnum.DISABLED;
        switch (startMode) {
            case DISABLED:
                AlertDialog finishAlert = new AlertDialog.Builder(this)
                        .create();
                    finishAlert.setCancelable(false);
          finishAlert.setTitle("Trial Expired");
                finishAlert.setMessage("Your trial has expired. Please consider purchasing the full version.");
                finishAlert.setButton("Ok", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        //RideManager rm = RideManager.get(RideActivity.this);
                        //if(rm != null)
                          //  rm.Shutdown();
                        RideActivity.this.finish();
                        //dialog.dismiss();
                    }
                });
                finishAlert.show();
                break;
            case EXPIRED_FREE: // expired trail. Let them purchase or do nothing.
                AppManager.PurchaseDialog(this);
                break;
            case NORMAL_FREE:
                // Still counting down the expiry. Do nothing
                break;
            case NORMAL_PAID:
                // TODO Until I figure out what this overly complicated thing does, don't use it
                // If it is merely to prevent folks from side-loading a copy they somehow get a hold of, they
                // deserve a free version
                //  checkLicense();
                // all is well. Do nothing
                break;
            case FIRST_TIME_FREE:
                // the version number ends in .free
                AppManager.CopyAssetFileOrDir("empty.gpx", context);
                AppManager.CopyAssetFileOrDir("EnchiladaBuffet_Austin.gpx", context);
                AppManager.CopyAssetFileOrDir("BikeBadger.pdf", context);

                // conditional shows the version notes (only if the version has changed)
                VersionNotes myNotes = new VersionNotes(this);
                myNotes.showVersionNotes();
                break;
            case FIRST_TIME_PAID:
                AppManager.CopyAssetFileOrDir("empty.gpx", context);
                AppManager.CopyAssetFileOrDir("EnchiladaBuffet_Austin.gpx", context);
                AppManager.CopyAssetFileOrDir("BikeBadger.pdf", context);

                // TODO Until I figure out what this overly complicated thing does, don't use it
                // If it is merely to prevent folks from side-loading a copy they somehow get a hold of, they
                // deserve a free version
                // checkLicense();

                // conditional shows the version notes (only if the version has changed)
                VersionNotes myNotes2 = new VersionNotes(this);
                myNotes2.showVersionNotes();

                break;
            case FIRST_TIME_VERSION:
                AppManager.CopyAssetFileOrDir("BikeBadger.pdf", context);
                // conditional shows the version notes (only if the version has changed)
                VersionNotes myNotes1 = new VersionNotes(this);
                myNotes1.showVersionNotes();
                break;
        }

        if (AppManager.IsPackageInstalled("net.dinglisch.android.taskerm", context)) {
            Log.d(Constants.APP.TAG, "Tasker Installed");
        }

        if (AppManager.IsPackageInstalled("com.strava", context)) {
            Log.d(Constants.APP.TAG, "Strava Installed");
        } else {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.google.android.apps.maps"));
            startActivity(intent);
        }

        if (AppManager.IsPackageInstalled("com.maxmpz.audioplayer", context)) {
            Log.d(Constants.APP.TAG, "PowerAMP Installed");
        }


    }

    @Override
    protected Fragment createFragment() {
        Log.d("Bike Badger", "RideActivity::createFragment");
        long runId = getIntent().getLongExtra(EXTRA_RUN_ID, -1);
        if (runId != -1) {
            Log.d("Bike Badger", "RideActivity::createFragment - newInstance");
            return RideFragment.newInstance(runId);
        } else {
            Log.d("Bike Badger", "RideActivity::createFragment - new RideFragment");
            return new RideFragment();
        }
    }

    @Override
    public void onBackPressed() {

        String msg = "Are you sure you want to exit?";
        // free version doesn't get to save
        if(!AppManager.IsFreeVersion(getApplicationContext()) && RideManager.get(RideActivity.this).IsDirtyGPX()) {
            msg = "Overwrite \"" + RideManager.get(RideActivity.this).GetGPXFileName() + "?\"  Select the back key to return the application.";
            new AlertDialog.Builder(this)
                    .setMessage(msg)
                    .setCancelable(true)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            RideManager mRideManager = RideManager.get(RideActivity.this);
                            mRideManager.ExportWaypointsTrack();
                            mRideManager.Shutdown();
                            RideActivity.this.finish();
                        }
                    })
                    .setNegativeButton("No", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {

                            RideManager mRideManager = RideManager.get(RideActivity.this);
                            mRideManager.Shutdown();
                            RideActivity.this.finish();
                        }
                    })
                    .show();
        } else {
            msg = "Are you sure you want to exit?";
            new AlertDialog.Builder(this)
                    .setMessage(msg)
                    .setCancelable(false)
                    .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            RideManager mRideManager = RideManager.get(RideActivity.this);
                            mRideManager.Shutdown();
                            RideActivity.this.finish();
                        }
                    })
                    .setNegativeButton("No", null)
                    .show();
        }
     }
}
