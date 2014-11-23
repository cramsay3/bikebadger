package bikebadger;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;

import us.theramsays.bikebadger.app2.R;
import util.vending.licensing.AESObfuscator;
import util.vending.licensing.LicenseChecker;
import util.vending.licensing.LicenseCheckerCallback;
import util.vending.licensing.ServerManagedPolicy;
//import android.view.MotionEvent;
//import android.widget.TextView;
//import com.android.vending.licensing.*;

public abstract class LicenseCheckActivity extends SingleFragmentActivity {

    static boolean licensed = true;
    static boolean didCheck = false;
    static boolean checkingLicense = false;
    // Full license
    static final String BASE64_PUBLIC_KEY = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAtK+Tzdofuz5D6i8Cz9sZaUL5f1pAOzNTYK9YVvpc3aNe8FaI36d4ozYaXFrAckoFaY89lozRlhWeQNImwmYwWrD3574qMSyUdDg0CbceB0owhEAa8iRebAgHbd7AH3GtF4WzRyaHnCGOzB+m9zd0SK5Y9SAFgbi2v0IEuZOMtmkZVk5o0eWqKGWINxxnMTwyqk30ArHRSPpVD2slcVSgySbc4QllfeXexZjWhWEZicdJJ9Clx4XmClI8yKDjQcMxexQb/+JeHp4OAbduInzkS8Dl0VeG4yGZb0O33I8VumScH+zmQAPFkaWg0RFOq+DM9QetWdfIihW94NN7vyId6QIDAQAB";

    LicenseCheckerCallback mLicenseCheckerCallback;
    LicenseChecker mChecker;

    Handler mHandler;

    SharedPreferences prefs;

    // REPLACE WITH YOUR OWN SALT , THIS IS FROM EXAMPLE
    private static final byte[] SALT = new byte[]{
            -46, 65, 30, -128, -103, -57, 74, -64, 51, 88, -95, -45, 77, -117, -36, -113, -11, 32, -64,
            89
    };

    private void displayResult(final String result) {
        mHandler.post(new Runnable() {
            public void run() {

                setProgressBarIndeterminateVisibility(false);

            }
        });
    }

    protected void doCheck() {

        didCheck = false;
        checkingLicense = true;
        setProgressBarIndeterminateVisibility(true);

        mChecker.checkAccess(mLicenseCheckerCallback);
    }

    protected void checkLicense() {

        Log.i("LICENSE", "checkLicense");
        mHandler = new Handler();

        // Try to use more data here. ANDROID_ID is a single point of attack.
        String deviceId = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);

        // Library calls this when it's done.
        mLicenseCheckerCallback = new MyLicenseCheckerCallback();
        // Construct the LicenseChecker with a policy.
        mChecker = new LicenseChecker(
                this, new ServerManagedPolicy(this,
                new AESObfuscator(SALT, getPackageName(), deviceId)),
                BASE64_PUBLIC_KEY);

//        mChecker = new LicenseChecker(
//                this, new StrictPolicy(),
//                BASE64_PUBLIC_KEY);

        doCheck();
    }

    protected class MyLicenseCheckerCallback implements LicenseCheckerCallback {

        @Override
        public void allow(int reason) {
            Log.i("LICENSE", "allow");
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            // Should allow user access.
            displayResult(getString(R.string.allow));
            licensed = true;
            checkingLicense = false;
            didCheck = true;
        }

        @Override
        public void dontAllow(int reason) {
            Log.i("LICENSE", "dontAllow");
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            displayResult(getString(R.string.dont_allow));
            licensed = false;
            // Should not allow access. In most cases, the app should assume
            // the user has access unless it encounters this. If it does,
            // the app should inform the user of their unlicensed ways
            // and then either shut down the app or limit the user to a
            // restricted set of features.
            // In this example, we show a dialog that takes the user to Market.
            checkingLicense = false;
            didCheck = true;

            showDialog(0);
        }

        @Override
        public void applicationError(int errorCode) {

         Log.i("LICENSE", "error: " + errorCode);
            if (isFinishing()) {
                // Don't update UI if Activity is finishing.
                return;
            }
            licensed = false;
            // This is a polite way of saying the developer made a mistake
            // while setting up or calling the license checker library.
            // Please examine the error code and fix the error.
            String result = String.format(getString(R.string.application_error), errorCode);
            checkingLicense = false;
            didCheck = true;

            displayResult(result);
            showDialog(0);
        }



    }

    protected Dialog onCreateDialog(int id) {
        // We have only one dialog.
        return new AlertDialog.Builder(this)
                .setTitle(R.string.unlicensed_dialog_title)
                .setMessage(R.string.unlicensed_dialog_body)
                .setPositiveButton(R.string.buy_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        Intent marketIntent = new Intent(Intent.ACTION_VIEW, Uri.parse(
                                "http://market.android.com/details?id=" + getPackageName()));
                        startActivity(marketIntent);
                        finish();
                    }
                })
                .setNegativeButton(R.string.quit_button, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        finish();
                    }
                })

                .setCancelable(false)
                .setOnKeyListener(new DialogInterface.OnKeyListener(){
                    public boolean onKey(DialogInterface dialogInterface, int i, KeyEvent keyEvent) {
                        Log.i("License", "Key Listener");
                        finish();
                        return true;
                    }
                })
                .create();

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mChecker != null) {
            Log.i("LIcense", "distroy checker");
            mChecker.onDestroy();
        }
    }
}