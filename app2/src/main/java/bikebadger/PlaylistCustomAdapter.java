package bikebadger;

/**
 * Created by cramsay on 9/26/2014.
 */

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import us.theramsays.bikebadger.app2.R;

public class PlaylistCustomAdapter extends ArrayAdapter<Playlist> {
    private final Context context;
    private final ArrayList<Playlist> values;

    public PlaylistCustomAdapter(Context context,  ArrayList<Playlist> values) {
        super(context, R.layout.playlists, values);
        this.context = context;
        this.values = values;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = (LayoutInflater) context
                .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        View rowView = inflater.inflate(R.layout.activity_playlists, parent, false);
        TextView textView = (TextView) rowView.findViewById(R.id.label);
        //TextView txtView2 = (TextView) rowView.findViewById(R.id.id);
        ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);
        textView.setText(values.get(position).GetName());
        // Change the icon based on from

        Playlist pl = values.get(position);
        final String from = pl.GetFrom();
        if(from.equals("GM"))
            imageView.setImageResource(R.drawable.google_play_music);
        else if(from.equals("PA"))
            imageView.setImageResource(R.drawable.poweramp);
        return rowView;
    }
}