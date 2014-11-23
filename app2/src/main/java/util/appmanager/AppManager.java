package util.appmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.AssetFileDescriptor;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Build;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Log;

import java.io.File;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.text.ParseException;
import java.util.Date;

import us.theramsays.bikebadger.app2.R;
import util.Constants;
import util.FileUtils;

/**
 * Created by cramsay on 10/21/2014.
 */
public class AppManager {


    public enum AppStartEnum {
        FIRST_TIME_FREE, FIRST_TIME_PAID, FIRST_TIME_VERSION, NORMAL_FREE, NORMAL_PAID, EXPIRED_FREE, DISABLED;
    }

    private static final String LAST_APP_VERSION = "last_app_version";


    public static void RunOnce(Context context, SharedPreferences prefs)
    {
        switch (CheckAppStart(context, prefs)) {
            case NORMAL_FREE:
            case NORMAL_PAID:
                // We don't want to get on the user's nerves
                break;
            case FIRST_TIME_VERSION:
                // TODO show what's new
                break;
            case FIRST_TIME_FREE:
            case FIRST_TIME_PAID:

                CopyAsset("empty.gpx", context);
                CopyAsset("EnchiladaBuffet_Austin.gpx",  context);
                // TODO show a tutorial
                break;
            default:
                break;
        }
    }

    // This doesn't work. It keeps the asset crunch or compression and corrupts the file
    public static void CopyAsset(String assetFilename, Context context) {
        AssetManager am = context.getAssets();
        AssetFileDescriptor afd = null;

        try {
            afd = am.openFd(assetFilename);

            File externalAppsDirectory = Constants.APP.EXTERNAL_APP_DIR;
            if (!externalAppsDirectory.exists()) {
                externalAppsDirectory.mkdirs(); //make if not exist
            }

            // Create new file to copy into.
            File file = new File(Constants.APP.EXTERNAL_APP_DIR + java.io.File.separator + assetFilename);
            file.createNewFile();

            copyFdToFile(afd.getFileDescriptor(), file);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static boolean IsPackageInstalled(String packagename, Context context) {
        PackageManager pm = context.getPackageManager();
        try {
            pm.getPackageInfo(packagename, PackageManager.GET_META_DATA);

            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    public static String BuildCreditVersionNotes(Context context) {

        final SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean isFreeVersion = IsFreeVersion(context);
        final boolean isHackedVersion = myPrefs.getBoolean("Hacked", false);

        String text = "";

        text += "<br/>Version: " + AppManager.GetCurrentVersionName(context);
        if(isHackedVersion)
            text += " (Hacked Full Version)";

        text += "<br/>Build: " + Build.VERSION.INCREMENTAL;

        text += "<br/><br/>Bike Badger is an application that \"badgers\" you while you ride or run. ";
        text += "It is meant only for entertainment purposes and the developer is not liable. ";
        text += "Please ride safe and responsibly. <br/><br/>";
        text += "Visit <a href=\"http://bikebadger.ramsays.us\">http://bikebadger.ramsays.us</a> for instructions and other notes.<br/>";
        text += "<br/>For the best experience the following applications should be installed:<br/>";
        text += "<br/>Required:";

        //Integer resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context);
        //if(resultCode == ConnectionResult.SUCCESS) {
         //   text += "<br/><ul><li>Google Play Services is installed.</li>";
        //} else {
          //  text += "<br/><li>Google Play Services is NOT installed.</li>";
        //}
        boolean TTSInstalled = IsTextToSpeechInstalled(context);

        if(IsTextToSpeechInstalled(context)) {
            text += "<br/>* Text To Speech Service installed";
        } else {
            text += "<br/>* Text To Speech Service NOT installed";
        }

        text += "<br/>* " + FormatPackageInstalledHtml("Google Play Services", R.string.package_name_google_gms, context);
        text += "<br/>* " + FormatPackageInstalledHtml("Google Map", R.string.package_name_google_map, context);
        text += "<br/><br/>Recommended:";
        text += "<br/>* " + FormatPackageInstalledHtml("PowerAMP", R.string.package_name_poweramp, context);
        text += "<br/>* " + FormatPackageInstalledHtml("Tasker", R.string.package_name_tasker, context);
        text += "<br/>* " + FormatPackageInstalledHtml("Strava", R.string.package_name_strava, context);
        //text += "<br/>* " + FormatPackageInstalledHtml("Google Music Player", R.string.package_name_google_music, context);
       // text += "<br/><br/>Bike Badger Version: " + GetCurrentVersionName(context);
        //text += "<br/><br/>Package Name: " + context.getPackageName();
        text += "<br/><br/>Date Installed: " + GetAppFirstInstallTime(context);
        if(isFreeVersion) {
            text += "<br/>Days left: " + (Constants.APP.EXPIRY_DAYS - DaysSinceInstalled(context, myPrefs));
            text += "<br/>NOTE: The free version does not save.";
        }

        //text += "<br/>Date Updated" + AppManager.GetAppLastUpdateTime(context);
        return text;
    }


    /**
     * The time at which the app was first installed. Units are as per currentTimeMillis().
     * @param context
     * @return
     */
    public static String GetAppFirstInstallTime(Context context){
        PackageInfo packageInfo;
        String retString = "";
        try {
            if(Build.VERSION.SDK_INT > 8/*Build.VERSION_CODES.FROYO*/ ){
                packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                retString = Constants.APP.SIMPLE_DTG_FORMAT.format(new Date(packageInfo.firstInstallTime));
            }else{
                //firstinstalltime unsupported return last update time not first install time
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
                String sAppFile = appInfo.sourceDir;

                retString = new Date( new File(sAppFile).lastModified() ).toString();
            }
        } catch (PackageManager.NameNotFoundException e) {
            //should never happen

        }
        return retString;
    }
    /**
     * The time at which the app was last updated. Units are as per currentTimeMillis().
     * @param context
     * @return
     */
    public static String GetAppLastUpdateTime(Context context){
        String retString = "";

        try {
            if(Build.VERSION.SDK_INT>8/*Build.VERSION_CODES.FROYO*/ ){
                PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
                retString = Constants.APP.SIMPLE_DTG_FORMAT.format(new Date(packageInfo.lastUpdateTime));
                //retString = new Date( packageInfo.lastUpdateTime * 1000).toString();
            }else{
                ApplicationInfo appInfo = context.getPackageManager().getApplicationInfo(context.getPackageName(), 0);
                String sAppFile = appInfo.sourceDir;

                retString = new Date( new File(sAppFile).lastModified() * 1000).toString();
            }
        } catch (PackageManager.NameNotFoundException e) {
            //should never happen

        }
        return retString;
    }

    public static String GetCurrentVersionName(Context context) {
        PackageInfo pInfo;
        String retString = "";

        try {
            pInfo = context.getPackageManager().getPackageInfo(
                context.getPackageName(), 0);
            retString = pInfo.versionName;

        } catch (PackageManager.NameNotFoundException e) {
            //Log.w(Constants.LOG,"Unable to determine current app version from pacakge manager. Defenisvely assuming normal app start.");

        }
        return retString;
    }

    public static void copyFdToFile(FileDescriptor src, File dst) throws IOException {
        FileChannel inChannel = new FileInputStream(src).getChannel();
        FileChannel outChannel = new FileOutputStream(dst).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            if (inChannel != null)
                inChannel.close();
            if (outChannel != null)
                outChannel.close();
        }
    }

    public static AppStartEnum CheckAppStart(Context context, SharedPreferences prefs) {

        PackageInfo pInfo;
        AppStartEnum appStart = AppStartEnum.NORMAL_FREE;

        // The user has acknowledged the expiration
        boolean isDisabled = prefs.getBoolean("Expired", false);

        if(isDisabled) {
            return AppStartEnum.DISABLED;
        }

        if(IsFreeVersion(context) && IsTimetrialExpired(context, prefs)) {
           return  AppStartEnum.EXPIRED_FREE;
        }

        try {
            pInfo = context.getPackageManager().getPackageInfo(
                    context.getPackageName(), 0);
            int lastVersionCode = prefs.getInt(LAST_APP_VERSION, -1);
            // String versionName = pInfo.versionName;
            int currentVersionCode = pInfo.versionCode;
            appStart = checkAppStart(context, currentVersionCode, lastVersionCode);
            // Update version in preferences
            prefs.edit().putInt(LAST_APP_VERSION, currentVersionCode).commit();
        } catch (PackageManager.NameNotFoundException e) {
            //Log.w(Constants.LOG,"Unable to determine current app version from pacakge manager. Defenisvely assuming normal app start.");
        }
        return appStart;
    }

    private static AppStartEnum checkAppStart(Context context, int currentVersionCode, int lastVersionCode) {
        boolean free = IsFreeVersion(context);

        if (lastVersionCode == -1) {
            return free ? AppStartEnum.FIRST_TIME_FREE : AppStartEnum.FIRST_TIME_PAID;
        } else if (lastVersionCode < currentVersionCode) {
            return AppStartEnum.FIRST_TIME_VERSION ;
        } else if (lastVersionCode > currentVersionCode) {
              return free ? AppStartEnum.NORMAL_FREE : AppStartEnum.NORMAL_PAID ;
        } else {
            return  free ? AppStartEnum.NORMAL_FREE : AppStartEnum.NORMAL_PAID ;
        }
    }

    public static void  CopyAssetFileOrDir(String path, Context context) {
        AssetManager assetManager = context.getAssets();
        String assets[] = null;

        File externalAppsDirectory = Constants.APP.EXTERNAL_APP_DIR;
        if (!externalAppsDirectory.exists()) {
            externalAppsDirectory.mkdirs(); //make if not exist
        }

        try {
            Log.i("tag", "copyFileOrDir() " + path);
            assets = assetManager.list(path);
            if (assets.length == 0) {
                copyFile(path, context);
            } else {
                String fullPath =  Constants.APP.EXTERNAL_APP_DIR + java.io.File.separator + path;
                Log.i("tag", "path="+fullPath);
                File dir = new File(fullPath);
                if (!dir.exists() && !path.startsWith("images") && !path.startsWith("sounds") && !path.startsWith("webkit"))
                    if (!dir.mkdirs())
                        Log.i("tag", "could not create dir "+fullPath);
                for (int i = 0; i < assets.length; ++i) {
                    String p;
                    if (path.equals(""))
                        p = "";
                    else
                        p = path + "/";

                    if (!path.startsWith("images") && !path.startsWith("sounds") && !path.startsWith("webkit"))
                        CopyAssetFileOrDir(p + assets[i], context);
                }
            }
        } catch (IOException ex) {
            Log.e("tag", "I/O Exception", ex);
        }
    }

    private static void copyFile(String filename, Context context) {
        AssetManager assetManager = context.getAssets();

        InputStream in = null;
        OutputStream out = null;
        String newFileName = null;
        try {
            Log.i("tag", "copyFile() "+filename);
            in = assetManager.open(filename);
            if (filename.endsWith(".jpg")) // extension was added to avoid compression on APK file
                newFileName = Constants.APP.EXTERNAL_APP_DIR + java.io.File.separator + filename.substring(0, filename.length()-4);
            else
                newFileName = Constants.APP.EXTERNAL_APP_DIR + java.io.File.separator + filename;
            out = new FileOutputStream(newFileName);

            byte[] buffer = new byte[1024];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            in.close();
            in = null;
            out.flush();
            out.close();
            out = null;
        } catch (Exception e) {
            e.printStackTrace();
            Log.e("tag", "Exception in copyFile() of "+newFileName);
            Log.e("tag", "Exception in copyFile() "+e.toString());
        }

    }

    public static String FormatPackageInstalledHtml(String name, int packageNameId, Context context)
    {
        String sPackageName = context.getResources().getString(packageNameId);
        String txt = "";
        txt += "<a href=\"market://details?id=" + sPackageName + "\">" + name + "</a> " +
                (AppManager.IsPackageInstalled(sPackageName, context) ? " installed" : " <em><bold>NOT</bold></em> installed");

        return txt;
    }


    public static boolean IsTextToSpeechInstalled(Context context) {
        Intent checkTTSIntent = new Intent();
        checkTTSIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);

        return checkTTSIntent.resolveActivity(context.getPackageManager()) != null;
    }

    public static boolean IsFreeVersion(Context context)
    {
        final String verName = AppManager.GetCurrentVersionName(context);
        final String ext = FileUtils.getExtension( verName );
        final SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        final boolean isHacked = myPrefs.getBoolean("Hacked", false);

        // if the free version is hacked to paid, return paid version
        if(isHacked) {
            return false;
        }

        if(ext != null && ".free".equals(ext))
            return true;
        else
            return false;
    }

    private static boolean IsTimetrialExpired(Context context, SharedPreferences preferences) {

            long days = DaysSinceInstalled(context, preferences);
            Log.d(Constants.APP.TAG, "days = " + days);

            if (days > Constants.APP.EXPIRY_DAYS) { // More than 7 days?
                return true;
            } else
                return false;
    }

    public static void PurchaseDialog(final Activity activity) {
        String msg = "Your trial has expired. Purchase the full version?";
        new AlertDialog.Builder(activity)
                .setMessage(msg)
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        final String sMarketUri = "market://details?id=" + activity.getResources().getString(R.string.package_name_bikebadger_paid);
                        final Intent intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(sMarketUri));
                        activity.startActivity(intent);
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                    }
                })
                .show();

        // flag that the free version has expired and has been acknowledged
        final SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(activity.getApplicationContext());
        myPrefs.edit().putBoolean("Expired", true).commit();
    }

    private static long DaysSinceInstalled(Context context, SharedPreferences preferences) {

        //SharedPreferences preferences = getPreferences(MODE_PRIVATE);
        String installDate = preferences.getString("InstallDate", null);
        long days = Constants.APP.EXPIRY_DAYS;
        if (installDate == null) {
            // First run, so save the current date
            SharedPreferences.Editor editor = preferences.edit();
            Date now = new Date();
            String dateString = Constants.APP.SIMPLE_DTG_FORMAT.format(now);
            editor.putString("InstallDate", dateString);
            // Commit the edits!
            editor.commit();

        } else {
            // This is not the 1st run, check install date
            Date before = null;
            try {
                before = (Date) Constants.APP.SIMPLE_DTG_FORMAT.parse(installDate);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            Date now = new Date();
            long diff = now.getTime() - before.getTime();
            days = diff / Constants.APP.ONE_DAY;
            Log.d(Constants.APP.TAG, "days = " + days);
        }
        return days;
    }

    public static void SimpleNotice(Context context, String title, String notice) {
        AlertDialog ad1 = new AlertDialog.Builder(context)
                .create();
        ad1.setCancelable(false);
        ad1.setTitle(title);
        ad1.setMessage(notice);
        ad1.setButton("Ok", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
                dialog.dismiss();
            }
        });
        ad1.show();
    }
}
