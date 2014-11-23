package bikebadger;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ListView;

import java.util.ArrayList;

import us.theramsays.bikebadger.app2.R;
import util.mediamanager.PlaylistUtils;

public class PlaylistsActivity extends ListActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playlists);
        ArrayList<Playlist> allPlaylists = PlaylistUtils.GetAllPlaylists(getApplicationContext());

        if(allPlaylists == null || allPlaylists.isEmpty()) {
            AlertDialog ad = new AlertDialog.Builder(this)
                    .create();
            ad.setCancelable(false);
            ad.setTitle("No Playlist Found");
            ad.setMessage("Create a playlist in Google Music or PowerAmp.");
            ad.setButton("Ok", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                    finish();
                }
            });
            ad.show();
        } else {
            PlaylistCustomAdapter adapter = new PlaylistCustomAdapter(this, allPlaylists);
            setListAdapter(adapter);
        }
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.playlists, menu);
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

    @Override
    protected void onListItemClick(ListView l, View v, int position, long id) {
        Playlist item = (Playlist) getListAdapter().getItem(position);

        //if(item.GetFrom().equals("GM"))
          //  PlaylistUtils.PlayGMPlaylistUsingSearch(getApplicationContext(), item.GetName());
        //else if(item.GetFrom().equals("PA"))
          //  PowerAmpUtils.PlayPowerAmpPlaylist(getApplicationContext(), item.GetName() );

        Intent intent = getIntent();
        intent.putExtra("name", item.GetName() );
        intent.putExtra("from", item.GetFrom());
        setResult(RESULT_OK, intent);
        finish();

       // Toast.makeText(this, item.GetName() + " selected with id " + item.GetId(), Toast.LENGTH_LONG).show();
    }
}
