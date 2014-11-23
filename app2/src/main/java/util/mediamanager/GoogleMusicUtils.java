package util.mediamanager;

import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;


public class GoogleMusicUtils {

    public static void TryDifferentIntents(Context context) {
        try {
            String pkgname = "com.sec.android.app.music";
            PackageManager pkgmanager = context.getPackageManager();
            Intent intent = pkgmanager.getLaunchIntentForPackage(pkgname);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch(Exception e) {
            // music player not found
        }

        // launches the player
        // Intent intent = new Intent("android.intent.action.MUSIC_PLAYER");
        //context.startActivity(intent);

        Intent intent = new Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
        intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS,
                MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE);
        intent.putExtra("android.intent.extra.playlist", "GM1");
        intent.putExtra(SearchManager.QUERY, "GM1");
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }

        // works!
        Intent intent1 = new Intent( MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
        intent1.putExtra(MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE,
                "android.intent.extra.playlist" );
        intent1.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/playlist");
        intent1.putExtra(SearchManager.QUERY, "GM1");
        if (intent1.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }else{
            //doToast("Sorry, no app was found to service this request", context);
        }

    }

    public static void PlayPLaylist(Context context, String playlistName) {

        Intent intent1 = new Intent( MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
        intent1.putExtra(MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE,
                "android.intent.extra.playlist" );
        intent1.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/playlist");
        intent1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // not from an activity
        intent1.putExtra(SearchManager.QUERY, playlistName);
        if (intent1.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent1);
        }else{
            //doToast("Sorry, no app was found to service this request", context);
        }

        /*
        DOESN't WORK
     long id = GetPlaylistId(context, playlistName);
        if (id != -1) {
            Uri contentUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.valueOf(id));
            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setData(contentUri);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            PackageManager pm = context.getPackageManager();
            ComponentName cn = intent.resolveActivity(pm);
           Log.d(Constants.APP.TAG, "cn = " + cn.getClassName() );

            if (intent.resolveActivity(pm) != null) {
                Log.d(Constants.APP.TAG, "resolveActivity ok");
                context.startActivity(intent);
            } else
                Log.d(Constants.APP.TAG, "resolveActivity returns null");
        }
    */

    }

    public static boolean ContainsPlaylist(Context context, String name) {
        long id = GetPlaylistId(context, name);
        return id != -1;
    }

    public static long GetPlaylistId(Context context, String name) {
        Uri playlistUri = Uri.parse("content://com.google.android.music.MusicContent/playlists");
        Cursor c = context.getContentResolver().query(playlistUri, new String[] {"_id", "playlist_name", "SourceId" }, null, null, null);
        long playListId = -1;
        String sPlaylistName = "";
        long id = 99999999;
        if (c != null) {
            while (c.moveToNext() && playListId == -1) {
                String str1 = c.getString(1);
                id = c.getLong(c.getColumnIndex("SourceId"));
                if(str1.equals(name))
                    playListId = c.getLong(0);
            }
        }
        c.close();

       /* DOESN"T WORK
        MediaPlayer mpObject = new MediaPlayer();


        try {
            if (id > 0) {
                Uri contentUri = ContentUris.withAppendedId(android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, Long.valueOf(id));
                mpObject.setAudioStreamType(AudioManager.STREAM_MUSIC);
                mpObject.setDataSource(context, contentUri);
                mpObject.prepare();
                mpObject.start();

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        */

        return playListId;
        //return 277;
    }

}
