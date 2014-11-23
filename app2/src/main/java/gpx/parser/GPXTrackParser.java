package gpx.parser;

import android.graphics.Color;
import android.os.AsyncTask;
import android.util.Log;

import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.PolylineOptions;

import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import bikebadger.RideManager;
import util.Constants;
import util.SafeParse;


public class GPXTrackParser extends AsyncTask<File, Void, Void> {

    private static final SimpleDateFormat gpxDate = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
    public RideManager mRideManager = null;
    public static NodeList trkptNodes = null;

    public GPXTrackParser(RideManager rideManager)
    {
        mRideManager = rideManager;
    }

    @Override
    protected Void doInBackground(File... params) {

        buildLatLngPoints(params[0]);

        return null;
    }

    @Override
    protected void onPostExecute(Void result) {
        // UI Thread update...
        // TextView txt = (TextView) findViewById(R.id.output);
        //txt.setText("Executed"); // txt.setText(result);
        Log.d(Constants.APP.TAG,"Finished");
        // might want to change "executed" for the returned string passed
        // into onPostExecute() but that is upto you
            // Since this is ASYNC, check to see if the map has been initialized. If it has, plot the newly loaded line...
        if(mRideManager.mMap != null && mRideManager.mPolylineOptions == null && mRideManager.mTrkpts != null) {
            mRideManager.mPolylineOptions = new PolylineOptions();
            mRideManager.mPolylineOptions.addAll(mRideManager.mTrkpts);
            mRideManager.mPolylineOptions.width(6);
            mRideManager.mPolylineOptions.color(Color.BLUE);
        }
    }

    @Override
    protected void onPreExecute() {}

    @Override
    protected void onProgressUpdate(Void... values) {}


    private void buildLatLngPoints(File gpxFile)
    {

        try
        {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();

            FileInputStream fis = new FileInputStream(gpxFile);
            Document dom = builder.parse(fis);
            Element root = dom.getDocumentElement();

            trkptNodes = root.getElementsByTagName("trk");
            //org.jdom.Element myele = root.
            //builder.par
            trkptNodes = root.getElementsByTagName("trkpt");
            final int itemsCount = trkptNodes.getLength();
            if(trkptNodes != null && itemsCount > 0) {
                mRideManager.mTrkpts = new ArrayList<LatLng>();
                // change the increment based on the size of the trkpts for speed
                //int increment = (itemsCount < 1000) ? 1 : itemsCount / 1000;
                // changed this back. I chose fidelity over speed...
                int increment = 1;
                for (int j = 0; j < itemsCount; j += increment) {
                    try {
                        Node item = trkptNodes.item(j);
                        NamedNodeMap attrs = item.getAttributes();
                        NodeList props = item.getChildNodes();

                        final double lat = SafeParse.parseDouble( attrs.getNamedItem("lat").getTextContent(), 0.0);
                        final double lng = SafeParse.parseDouble( attrs.getNamedItem("lon").getTextContent(), 0.0);
                        mRideManager.mTrkpts.add(new LatLng(lat, lng));
                    } catch (DOMException de) {
                        de.printStackTrace();
                    }
                    //Log.d(Constants.APP.TAG, "lat=" + lat + ", lng=" + lng);

                /*
                for(int k = 0; k<props.getLength(); k++)

                {
                    Node item2 = props.item(k);
                    String name = item2.getNodeName();
                    if(!name.equalsIgnoreCase("time")) continue;
                    try
                    {
                        pt.setTime((getDateFormatter().parse(item2.getFirstChild().getNodeValue())).getTime());

                    }

                    catch(ParseException ex)
                    {
                        ex.printStackTrace();
                    }
                }

                for(int y = 0; y<props.getLength(); y++)
                {
                    Node item3 = props.item(y);
                    String name = item3.getNodeName();
                    if(!name.equalsIgnoreCase("ele")) continue;
                    pt.setAltitude(Double.parseDouble(item3.getFirstChild().getNodeValue()));
                }
*/

                }
            }
            fis.close();
        }

        catch(FileNotFoundException e)
        {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        catch(ParserConfigurationException ex)
        {
            ex.printStackTrace();
        }
        catch (SAXException ex) {
            ex.printStackTrace();
        }

    }

    public static SimpleDateFormat getDateFormatter()
    {
        return (SimpleDateFormat)gpxDate.clone();
    }

}