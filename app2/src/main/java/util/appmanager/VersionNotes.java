package util.appmanager;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import us.theramsays.bikebadger.app2.R;

/**
 * MagicEula provides a simple way of presenting what has been updated
 * when a user installs or updates an app.
 * @author dream09
 *
 */
public class VersionNotes {

	public static final String NOTES_VERSION_KEY = ".notesversion";

	/* Variables */
	private Activity mActivity;

	/**
	 * Constructor
	 */
	public VersionNotes(Activity context) {
		mActivity = context;
	}

	/* Methods */
	
	/**
	 * Method only shows the version notes on first run
	 * or update.
	 */
	public void showVersionNotes() {

		final SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
        final String version = AppManager.GetCurrentVersionName(mActivity);
		if (!version.equals(myPrefs.getString(NOTES_VERSION_KEY, "NONE"))) {
			
			// Inflate and setup the view.
			LayoutInflater inflater = (LayoutInflater) mActivity.getLayoutInflater();
			View notesView = inflater.inflate(R.layout.notes_view, null);
			TextView notesText = (TextView) notesView.findViewById(R.id.notes_message);

            String notes = AppManager.BuildCreditVersionNotes(mActivity);
			notesText.setText(Html.fromHtml(notes));
			notesText.setMovementMethod(LinkMovementMethod.getInstance());
            notesText.setClickable(true);
			AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
    		builder.setView(notesView)

    			.setPositiveButton(mActivity.getString(R.string.button_dimiss), new DialogInterface.OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        SharedPreferences.Editor editor = myPrefs.edit();
                        editor.putString(NOTES_VERSION_KEY, version);
                        editor.commit();
                        dialog.dismiss();
                    }
                })
				.setOnCancelListener(new DialogInterface.OnCancelListener() {
					
					@Override
					public void onCancel(DialogInterface dialog) {
						SharedPreferences.Editor editor = myPrefs.edit();
						editor.putString(NOTES_VERSION_KEY, version);
						editor.commit();
						dialog.dismiss();
					}
				});
    		
    		AlertDialog dialog = builder.create();
    		dialog.show();
		}
	}
	

	/**
	 * Method checks the version of notes the user has seen
	 * against the argument version and returns true if they
	 * match otherwise false.
	 * @return
	 */
	public boolean versionCheck(String version) {
		SharedPreferences myPrefs = PreferenceManager.getDefaultSharedPreferences(mActivity);
		return (version.equals(myPrefs.getString(NOTES_VERSION_KEY, "NONE")));
	}
}
