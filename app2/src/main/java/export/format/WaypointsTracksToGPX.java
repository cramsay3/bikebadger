package export.format;

import android.util.Log;
import android.util.Xml;

import com.google.android.gms.maps.model.LatLng;

import org.xmlpull.v1.XmlSerializer;

import java.io.IOException;
import java.io.Writer;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import gpx.Waypoint;
import util.Constants;

/**
 * Created by cramsay on 9/22/2014.
 */
public class WaypointsTracksToGPX {
    String notes = null;
    XmlSerializer mXML = null;
    SimpleDateFormat simpleDateFormat = null;
    public WaypointsTracksToGPX()
    {
        mXML = null;
        simpleDateFormat = new SimpleDateFormat(Constants.APP.GPX_11_SIMPLEDATEFORMAT, Locale.getDefault());
        simpleDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    public void Export(ArrayList<Waypoint> waypoints, List<LatLng> tracks, Writer writer) throws IOException {
    // TODO add tracks to this too!
        try {
            mXML = Xml.newSerializer();
            mXML.setFeature("http://xmlpull.org/v1/doc/features.html#indent-output", true);
            mXML.setOutput(writer);
            mXML.startDocument("UTF-8", true);
            mXML.startTag("", "gpx");
            mXML.attribute("", "version", "1.1");
            mXML.attribute("", "creator", "BikeBadger");
            mXML.attribute("", "xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
            mXML.attribute("", "xmlns", "http://www.topografix.com/GPX/1/1");
            mXML.attribute("", "xsi:schemaLocation",
                    "http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd");
           // mXML.attribute("", "xmlns:gpxtpx",
             //       "http://www.garmin.com/xmlschemas/TrackPointExtension/v1");

            mXML.startTag("", "metadata");
            mXML.startTag("", "extensions");
            mXML.startTag("", "time");
            mXML.attribute("", "xmlns", "http://www.topografix.com/GPX/gpx_modified/0/1");
            final String time = simpleDateFormat.format(new Date(System.currentTimeMillis()));
            mXML.text(time);
            mXML.endTag("", "time");
            mXML.endTag("", "extensions");
            mXML.endTag("", "metadata");
            if( tracks != null && !tracks.isEmpty() ) {
                mXML.startTag("", "trk");
                mXML.attribute("", "name", "Bike Badger Combined Track");
                mXML.startTag("", "trkseg");
                int trkptCount = tracks.size();
                for (int idx = 0; idx < trkptCount; idx++) {
                    LatLng trkPt = tracks.get(idx);
                    if(trkPt != null) {
                        mXML.startTag("", "trkpt");
                        mXML.attribute("", "lat", Double.toString(trkPt.latitude));
                        mXML.attribute("", "lon", Double.toString(trkPt.longitude));
                        mXML.endTag("", "trkpt");
                    }
                }
                mXML.endTag("", "trkseg");
                mXML.endTag("", "trk");
            }

            // waypoints
            if(waypoints != null) {
                for (int idx = 0; idx < (waypoints.size()); idx++) {
                    Waypoint wp = waypoints.get(idx);
                    mXML.startTag("", "wpt");
                    String latString = Double.toString(wp.getLatitude());
                    String lonString = Double.toString(wp.getLongitude());
                    mXML.attribute("", "lat", latString);
                    mXML.attribute("", "lon", lonString);
                    mXML.startTag("", "time");
                    String dtgString;

                    if (wp.getTime() != null) {
                        dtgString = simpleDateFormat.format(wp.getTime());
                    } else {
                        dtgString = simpleDateFormat.format( System.currentTimeMillis()  );
                    }

                    mXML.text(dtgString);
                    mXML.endTag("", "time");
                    mXML.startTag("", "name");
                    mXML.text(wp.getName());
                    mXML.endTag("", "name");
                    mXML.startTag("", "desc");
                    mXML.text(wp.getDesc());
                    mXML.endTag("", "desc");
                    mXML.startTag("", "sym");
                    mXML.text("Pin, Red");
                    mXML.endTag("", "sym");
                    mXML.startTag("", "type");
                    mXML.text("Waypoint");
                    mXML.endTag("", "type");
                    mXML.endTag("", "wpt");
                }
            }

            mXML.endTag("", "gpx");
            mXML.flush();
            Log.d(Constants.APP.TAG, "mXML.endDocument()");
            mXML.endDocument();
            Log.d(Constants.APP.TAG, "mXML.endDocument()");
            mXML = null;
        } catch (IOException e) {
            mXML = null;
            e.printStackTrace();
        } catch (Exception er) {
            er.printStackTrace();
        }
    }

}
