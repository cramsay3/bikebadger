package bikebadger;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.location.Location;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TableRow;
import android.widget.TextView;

import gpx.Waypoint;
import us.theramsays.bikebadger.app2.R;
import util.appmanager.AppManager;
import util.Constants;
import util.TaskerIntent;

import static us.theramsays.bikebadger.app2.R.id.wpactivity_imgMarker;


public class WaypointActivity extends Activity {

    private Spinner mSpinner;
    private Button mBtnSubmit, btnCancel;
    private EditText mArgEditView;
    private ImageView mIconMarker;
    private TextView mTxtArg;
    private SeekBar mVolumeControl;
    private Location mLocation;
    private String mName;
    private AudioManager mAudioManager;
    public static final int  MY_PLAYLIST_DIALOG_REQUEST_CODE = 9101;
    public static final int MY_TASKER_DIALOG_REQUEST_CODE = 90123;
    private TableRow mArgTableRow;
    private Button mSelectBtn;
    private View.OnClickListener mTaskerOnClickListener;
    private View.OnClickListener mPlaylistOnClickListener;
    private Waypoint mWaypoint;
    private int mCurrentVolume = 0;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activitywaypoint);

        mArgEditView = (EditText) this.findViewById(R.id.waypoint_ArgEdit);
        mArgEditView.setEnabled(true);
        mIconMarker = (ImageView)this.findViewById(wpactivity_imgMarker);
        mTxtArg = (TextView) findViewById(R.id.wpactivity_txtArg);
        mArgTableRow =  (TableRow) this.findViewById(R.id.wpactivity_argumentTableRow);
        mSelectBtn = (Button) this.findViewById(R.id.wpactivity_SelectBtn);
        mSpinner = (Spinner) findViewById(R.id.spinner1);
        mBtnSubmit = (Button) findViewById(R.id.btnSubmit);

        addListenerSubmitButton();
        addListenerOnSpinnerItemSelection();

        mSpinner.setSelection(0, true);

        //mArgTextView = (TextView) findViewById(R.id.wa);
        //mArgEditView.selectAll();
        //mArgEditView.setText("");
         mLocation = getIntent().getExtras().getParcelable("location");
         mWaypoint = getIntent().getExtras().getParcelable("waypoint");

        mAudioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
        mCurrentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);

        mVolumeControl = (SeekBar)findViewById(R.id.volbar);
        mVolumeControl.setMax(maxVolume);
        mVolumeControl.setProgress(mCurrentVolume);
        mVolumeControl.setEnabled(true);
        mVolumeControl.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onStopTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onStartTrackingTouch(SeekBar arg0) {
                // TODO Auto-generated method stub

            }

            @Override
            public void onProgressChanged(SeekBar arg0, int arg1, boolean arg2) {

                //mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, arg1, AudioManager.ADJUST_SAME);
                //mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, arg1, AudioManager.FLAG_PLAY_SOUND);
                //mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, arg1, AudioManager.FLAG_SHOW_UI);
                //mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, arg1, AudioManager.FLAG_VIBRATE);
                mArgEditView.setText(Integer.toString(arg1));
            }
        });

       mPlaylistOnClickListener =  new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                startActivityForResult( new Intent(getApplicationContext(), PlaylistsActivity.class), MY_PLAYLIST_DIALOG_REQUEST_CODE);
            }
        };

        mTaskerOnClickListener =  new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                startActivityForResult(TaskerIntent.getTaskSelectIntent(), MY_TASKER_DIALOG_REQUEST_CODE);
            }
        };

        mSelectBtn.setText("Select");

        // populate mask
        if(mWaypoint != null) {
            int id = GetPosition( mWaypoint.getCommand() );
            if(id != -1) {
                mSpinner.setSelection(id);
            }
            }
    }



    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.waypoint, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {

            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void addListenerOnSpinnerItemSelection() {

        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                mName = adapterView.getItemAtPosition(i).toString();

                if (mName.equals("START")) {
                     //mIconMarker.setImageDrawable(drawableId);
                    mIconMarker.setImageDrawable(getResources().getDrawable(R.drawable.start_blue));
                    mTxtArg.setText("Target Speed:");
                    //mArgEditView = (EditText) view.findViewById(R.id.waypoint_ArgEdit);
                    //mArgTableRow.setVisibility(View.VISIBLE);
                    mArgEditView.setVisibility(View.VISIBLE);
                    mSelectBtn.setVisibility(View.GONE);
                    mVolumeControl.setVisibility(View.GONE);
                    mArgEditView.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                    //mArgEditView.setText("");
                    mArgEditView.setFocusable(true);
                    InputMethodManager keyboard = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                    keyboard.showSoftInput(mArgEditView, 0);
                } else if (mName.equals("PROMPT")) {
                    mIconMarker.setImageDrawable(getResources().getDrawable(R.drawable.prompt_blue));
                    //mArgTableRow.setVisibility(View.VISIBLE);
                    mTxtArg.setText("Prompt:");
                    mArgEditView.setVisibility(View.VISIBLE);
                    mSelectBtn.setVisibility(View.GONE);
                    mVolumeControl.setVisibility(View.GONE);
                    mArgEditView.setFocusable(true);
                    // mTxtArg.setVisibility(View.VISIBLE);
                    //mArgEditView.setVisibility(View.VISIBLE);
                    mArgEditView.setInputType(InputType.TYPE_CLASS_TEXT);
                } else if (mName.equals("PLAYLIST")) {
                        //PlaylistUtils.ListMediaButtonReceivers(this, "");
                        mSelectBtn.setOnClickListener(mPlaylistOnClickListener);
                        mIconMarker.setImageDrawable(getResources().getDrawable(R.drawable.music_blue));
                        mTxtArg.setText("Select Playlist:");
                        mArgEditView.setVisibility(View.GONE);
                        mSelectBtn.setVisibility(View.VISIBLE);
                        mVolumeControl.setVisibility(View.GONE);

                        //startActivityForResult( new Intent(getApplicationContext(), PlaylistsActivity.class), MY_PLAYLIST_DIALOG_REQUEST_CODE);

                } else if (mName.equals("TASKER")) {
                    if( !AppManager.IsPackageInstalled("net.dinglisch.android.taskerm", getApplicationContext())) {
                        AppNotInstalledAlert("Application Not Found",
                                "Tasker is not installed. Install it now?", "market://details?id=net.dinglisch.android.taskerm");
                    } else {
                        mSelectBtn.setOnClickListener(mTaskerOnClickListener);
                        mIconMarker.setImageDrawable(getResources().getDrawable(R.drawable.tasker_blue));
                        mTxtArg.setText("Select Task:");
                        mArgEditView.setVisibility(View.GONE);
                        mSelectBtn.setVisibility(View.VISIBLE);
                        mVolumeControl.setVisibility(View.GONE);

                       // startActivityForResult(TaskerIntent.getTaskSelectIntent(), MY_TASKER_DIALOG_REQUEST_CODE);
                    }
                } else if (mName.equals("VOLUME")) {
                        mIconMarker.setImageDrawable(getResources().getDrawable(R.drawable.quiet_blue));
                    mTxtArg.setText("Select Volume:");
                    mArgEditView.setVisibility(View.GONE);
                    mSelectBtn.setVisibility(View.GONE);
                    mVolumeControl.setVisibility(View.VISIBLE);
                 //   mVolumeControl.setVisibility(View.VISIBLE);
                        // / mTxtArg.setVisibility(View.INVISIBLE);
                        //mArgEditView.setVisibility(View.GONE);

                       // InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                       // keyboard.hideSoftInputFromWindow(mArgEditView.getWindowToken(), 0);
                        // mArgEditView.setFocusable(false);
                        //mArgEditView.setClickable(true);
                        //mArgEditView.setEnabled(true);
                        /*
                        mArgEditView.setOnClickListener(new View.OnClickListener() {

                            @Override
                            public void onClick(View view) {
                               // AudioManager mAudioManager = (AudioManager) getApplicationContext().getSystemService(Context.AUDIO_SERVICE);
                                int maxVolume = mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                                mAudioManager.adjustStreamVolume(AudioManager.STREAM_MUSIC, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI);
                                //mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_PLAY_SOUND);
                                //mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_SHOW_UI);
                                //mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, maxVolume, AudioManager.FLAG_VIBRATE);

                                mSetVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
                                mArgEditView.setText(Integer.toString(mSetVolume));
                            }
                        });
                        */

                    } else if (mName.equals("STRAVA REC") || mName.equals("STRAVA STOP")) {

                        if( !AppManager.IsPackageInstalled("com.strava", getApplicationContext()) ) {
                            AppNotInstalledAlert("Application Not Found",
                                    "Strava is not installed. Install it now?", "market://details?id=com.strava");
                        } else {
                            mIconMarker.setImageDrawable(getResources().getDrawable(R.drawable.blue_strava));
                            //mTxtArg.setVisibility(View.GONE);
                            mTxtArg.setText("Strava");
                            mArgEditView.setVisibility(View.GONE);
                            mSelectBtn.setVisibility(View.GONE);
                            mVolumeControl.setVisibility(View.GONE);
                        }
                    // InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                       // keyboard.hideSoftInputFromWindow(mArgEditView.getWindowToken(), 0);
                    } else if (mName.equals("SPEED")) {
                        mIconMarker.setImageDrawable(getResources().getDrawable(R.drawable.speed_blue));
                        mTxtArg.setText("Speaks status");
                        mArgEditView.setVisibility(View.GONE);
                        mSelectBtn.setVisibility(View.GONE);
                        mVolumeControl.setVisibility(View.GONE);
                        // mTxtArg.setText("Speed:");
                        InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                        keyboard.hideSoftInputFromWindow(mArgEditView.getWindowToken(), 0);
                        //keyboard.show (mArgEditView, 0);
                } else if (mName.equals("MUTE")) {
                    mIconMarker.setImageDrawable(getResources().getDrawable(R.drawable.quiet_blue));
                    //mTxtArg.setVisibility(View.GONE);
                    mTxtArg.setText("Mutes volume");
                    mArgEditView.setVisibility(View.GONE);
                    mSelectBtn.setVisibility(View.GONE);
                    mVolumeControl.setVisibility(View.GONE);

                    InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    keyboard.hideSoftInputFromWindow(mArgEditView.getWindowToken(), 0);
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });
    }

    // get the selected dropdown list value
    public void addListenerSubmitButton() {

        mBtnSubmit.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View v) {

                String argument = mArgEditView.getText().toString();
                String command = mName;
                Intent intent = getIntent();
                intent.putExtra("location", mLocation);

                if(mName.equals("STRAVA REC")) {
                    command = "STRAVA";
                    argument = "RECORD";
                } else if(mName.equals("STRAVA STOP")) {
                    command = "STRAVA";
                    argument = "STOP";
                } else if (mName.equals("START") ) {
                    command = "START";
                    argument = mArgEditView.getText().toString();
                } else if (mName.equals("PLAY") || mName.equals("PLAYLIST")) {
                    command = "PLAYLIST";
                    argument = mSelectBtn.getText().toString();
                } else if (mName.equals("PLAY") || mName.equals("TASKER")) {
                    command = "TASKER";
                    argument = mSelectBtn.getText().toString();
                } else if(mName.equals("MUTE") || mName.equals("QUIET")) {
                    command = "MUTE";
                    argument = "1";
                }  else if(mName.equals("VOLUME")) {
                    command = "VOLUME";
                    argument = mArgEditView.getText().toString();
            }

                intent.putExtra("command", command);
                intent.putExtra("argument", argument);
                setResult(RESULT_OK, intent);
                finish();
            }
        });
}

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case MY_PLAYLIST_DIALOG_REQUEST_CODE:
                if (resultCode == this.RESULT_OK) {
                    if (data != null) {
                        String name = data.getStringExtra("name");
                        final String from = data.getStringExtra("from");
                        //mArgEditView.setText(name);
                        mSelectBtn.setText(name);
                    }
                }
                break;
            case MY_TASKER_DIALOG_REQUEST_CODE:
                if (resultCode == this.RESULT_OK) {
                    if (data != null) {
                        String name = data.getDataString();
                        //final String from = data.getStringExtra("from");
                        //mArgEditView.setText(name);
                        mSelectBtn.setText(name);
                    }
                }
                break;
        }
    }

    public int GetPosition(String command) {

        int id = -1;

        do {
            id += 1;
        } while( id < (Constants.APP.WAYPOINT_COMMANDS.length - 1) && !command.equals(Constants.APP.WAYPOINT_COMMANDS[id]) );

        return command.equals(Constants.APP.WAYPOINT_COMMANDS[id]) ? id : -1;
    }


    public void AppNotInstalledAlert(final String title, final String msg, final String  uriString) {
        new AlertDialog.Builder(this)
                .setTitle(title)
                .setMessage(msg)
                .setCancelable(true)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(uriString));
                        startActivity(intent);
                       finish(); // bail out of the dialog so we don't bounce back to same entry
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        finish(); // bail out of the dialog so we don't bounce back to same entry
                    }
                })
                .show();
           }

}
