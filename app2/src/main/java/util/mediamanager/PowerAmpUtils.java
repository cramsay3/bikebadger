package util.mediamanager;

import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;

import poweramp_api_lib.src.com.maxmpz.poweramp.player.PowerampAPI;
import poweramp_api_lib.src.com.maxmpz.poweramp.player.TableDefs;

/**
 * Created by cramsay on 9/26/2014.
 */
public class PowerAmpUtils {

    public static boolean ContainsPlaylist(Context context, String name) {
        long id = GetPlaylistId(context, name);
        return id != -1;
    }

    public static long GetPlaylistId(Context context, String name) {
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.parse("content://com.maxmpz.audioplayer.data/playlists");
        //String[] proj ={ "folder_files._id", "folder_files.played_at","folders.path", "folder_files.name" };
        String[] proj = {TableDefs.Playlists._ID, TableDefs.Playlists.NAME};
        Cursor c = cr.query(uri, proj, null, null, null);
        long returnid = -1;
        long id = 99999999;
        if (c != null) {
            while (c.moveToNext() && returnid == -1) {
                String str1 = c.getString(1);
                if(str1.equals(name))
                    returnid = c.getLong(0);
            }
        }
        c.close();

        return returnid;
    }

    public static void PlayPlaylist(Context context, String name) {

        long id = GetPlaylistId(context, name);
        if (id != -1) {
            context.startService(new Intent(PowerampAPI.ACTION_API_COMMAND)

                    .putExtra(PowerampAPI.COMMAND, PowerampAPI.Commands.OPEN_TO_PLAY)
                    .setData(PowerampAPI.ROOT_URI.buildUpon()
                            .appendEncodedPath("playlists")
                            .appendEncodedPath(Long.toString(id))
                            .appendEncodedPath("files")
                            .build()));
        }
    }

}
