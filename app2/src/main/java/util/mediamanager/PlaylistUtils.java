package util.mediamanager;
/*
 * Copyright (C) 2012 Google Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */


import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.ActivityNotFoundException;
import android.content.ContentResolver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.content.CursorLoader;
import android.util.Log;
import android.widget.EditText;

import java.util.ArrayList;
import java.util.List;

import poweramp_api_lib.src.com.maxmpz.poweramp.player.TableDefs;
import bikebadger.Playlist;
import util.Constants;

/**
 * A class with static methods for building and editing playlists
 *
 * @author sainsley@google.com (Sam Ainsley)
 */
public class PlaylistUtils {

    public static void PlayMusic(String DataStream){
        MediaPlayer mpObject = new MediaPlayer();
        if(DataStream == null)
            return;
        try {
            mpObject.setDataSource(DataStream);
            mpObject.prepare();
            mpObject.start();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Gets the playlist ID given it's name
     *
     * @param context The activity calling this method
     * @param name The playlist name
     * @return The playlist ID
     */
    public static int getPlaylistId(Context context, String name) {
        String[] projExternalFilePlaylist = { MediaStore.Audio.Playlists._ID, MediaStore.Audio.Playlists.NAME };

        // filter
        String formattedName = name.replace("'", "''");
        String filter = MediaStore.Audio.Playlists.NAME + "= '" + formattedName + "'";

        CursorLoader loader = new CursorLoader(
                context, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, projExternalFilePlaylist, /*filter*/ null, null, null);


        Cursor testCursor = loader.loadInBackground();

        int idx = -1;
       boolean hasFirst = testCursor.moveToFirst();
        String sName = "";
       if (hasFirst) {
            idx = testCursor.getInt(0);
            sName = testCursor.getString(1);
            Log.d(Constants.APP.TAG,"idx=" + idx + ", sName = " + sName);
        }
        while (testCursor.moveToNext()) {
            idx = testCursor.getInt(0);
            sName = testCursor.getString(1);
            Log.d(Constants.APP.TAG,"idx=" + idx + ", sName = " + sName);
        }
        testCursor.close();

        String[] projGMP = {MediaStore.Audio.Media._ID, MediaStore.Audio.AudioColumns.TITLE, MediaStore.Audio.Media.ARTIST };
        Uri gMusicUri = Uri.parse("content://com.google.android.music.MusicContent/playlists");
        Cursor cur2 = context.getContentResolver()
                .query(gMusicUri, new String[] {"_id", "playlist_name" }, null, null, null);

        hasFirst = cur2.moveToFirst();
        if (hasFirst) {
            idx = cur2.getInt(0);
            sName = cur2.getString(1);
            Log.d(Constants.APP.TAG,"idx=" + idx + ", sName = " + sName);
        }
        while (cur2.moveToNext()) {
            idx = cur2.getInt(0);
            sName = cur2.getString(1);
            Log.d(Constants.APP.TAG,"idx=" + idx + ", sName = " + sName);
        }
        cur2.close();
        return idx;
    }



    /**
     * Adds a given song to an existing playlist
     *
     * @param context the managing activity
     * @param id the song id to add
     */
    public static void showPlaylistDialog(final Context context, final String id) {

        // Get list of playlists
        String[] proj = {
                MediaStore.Audio.Playlists._ID,
                "playlist_name" };
        CursorLoader loader = new CursorLoader(
                context, MediaStore.Audio.Playlists.EXTERNAL_CONTENT_URI, proj, null, null, null);
        final Cursor playlistCursor = loader.loadInBackground();
        // Show playlists
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        if (playlistCursor.moveToFirst()) {
            DialogInterface.OnClickListener clickListener = new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    //addToPlaylist(context, playlistCursor.getInt(0), id);
                }
            };
            builder.setCursor(playlistCursor, clickListener,
                    "playlist_name");
        } else {
            // No playlists: show create dialog
            builder.setTitle("Playlist");
            // TODO(sainsley): add default name based on
            // number of playlists in directory
            builder.setMessage("Enter Playlist Name");

            final EditText input = new EditText(context);
            builder.setView(input);

            builder.setPositiveButton(
                    "Ok", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String name = input.getText().toString();
                            ArrayList<String> ids = new ArrayList<String>();
                            ids.add(id);
                            //writePlaylist(context, name, ids);
                            return;
                        }
                    });
        }
        builder.show();
    }


    public static boolean IsPlaylistEmpty(Context context) {
        ArrayList<Playlist> aplaylist = GetAllPlaylists(context);
        return aplaylist == null || aplaylist.isEmpty();
    }

    public static ArrayList<Playlist> GetAllPlaylists(Context context) {

        ArrayList<Playlist> temp = new ArrayList<Playlist>();

        // Google Music Play
        Uri playlistUri = Uri.parse("content://com.google.android.music.MusicContent/playlists");
        Cursor playlistCursor = context.getContentResolver().query(playlistUri, new String[] {"_id", "playlist_name" }, null, null, null);
        long playListId = -1;
        String sPlaylistName = "";
        if (playlistCursor != null) {
            if (playlistCursor.moveToFirst()) {
                long id = playlistCursor.getInt(0);
                sPlaylistName =  playlistCursor.getString(1);
                Playlist pl = new Playlist();
                pl.SetFrom("GM");
                pl.SetId(id);
                pl.SetName(sPlaylistName);
                temp.add(pl);
            }
            playlistCursor.close();
        }

        // PowerAmp
        ContentResolver cr = context.getContentResolver();
        Uri uri = Uri.parse("content://com.maxmpz.audioplayer.data/playlists");
        //String[] proj ={ "folder_files._id", "folder_files.played_at","folders.path", "folder_files.name" };
        String[] proj = {TableDefs.Playlists._ID, TableDefs.Playlists.NAME};
        Cursor c = cr.query(uri, proj, null, null, null);
        long id = 99999999;
        if (c != null) {
            while (c.moveToNext()) {
                long plid = c.getLong(0);
                String str1 = c.getString(1);
                Playlist pl = new Playlist();
                pl.SetFrom("PA");
                pl.SetId(plid);
                pl.SetName(str1);
                temp.add(pl);
            }
        }
        c.close();

        return temp;
    }

    public static void PlaySearchArtist(Context context, String artist) {
        Intent intent = new Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
        intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE);
        intent.putExtra(MediaStore.EXTRA_MEDIA_ARTIST, artist);
        intent.putExtra(SearchManager.QUERY, artist);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }

    public static void PlayGMPlaylistUsingSearch(Context context, String playlist) {
        Intent intent = new Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
        intent.putExtra(MediaStore.EXTRA_MEDIA_FOCUS, MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE);
        intent.putExtra(SearchManager.QUERY, playlist);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        if (intent.resolveActivity(context.getPackageManager()) != null) {
            context.startActivity(intent);
        }
    }

    public static void ListMediaButtonReceivers(Context context, String query) {

        try {

            final Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
            PackageManager packageManager = context.getPackageManager();

            List<ResolveInfo> mediaReceivers = packageManager.queryBroadcastReceivers(mediaButtonIntent,
                    PackageManager.GET_INTENT_FILTERS | PackageManager.GET_RESOLVED_FILTER);

            for (int i = mediaReceivers.size() - 1; i >= 0; i--) {
                ResolveInfo mediaReceiverResolveInfo = mediaReceivers.get(i);
                String name = mediaReceiverResolveInfo.activityInfo.applicationInfo.sourceDir
                        + ", " + mediaReceiverResolveInfo.activityInfo.name;
                String cn = mediaReceiverResolveInfo.resolvePackageName;
                String cn1 = mediaReceiverResolveInfo.activityInfo.toString();
                Log.d(Constants.APP.TAG, "resolvePackageName media receivers = " + cn);
                Log.d(Constants.APP.TAG, "activityInfo = " + cn1);
            }

            mediaButtonIntent.setAction(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH);
            //mediaButtonIntent.setComponent(new ComponentName("com.spotify.music", "com.spotify.music.MainActivity"));
            mediaButtonIntent.putExtra(SearchManager.QUERY, "GM1");
            mediaButtonIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
            context.startActivity(mediaButtonIntent);
        } catch (ActivityNotFoundException e) {
            Log.e(Constants.APP.TAG, "Error searching Spotify w/ query '" + query + "'");
            //Toast.makeText(mRideManager.mAppContext, String.format("Error parsing query \"%s\"", query), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }




}