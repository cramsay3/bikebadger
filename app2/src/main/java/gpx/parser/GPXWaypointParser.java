/*
 * Copyright (c) 2009 Martin Jansen
 * All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in the
 *    documentation and/or other materials provided with the distribution.
 * 3. The name of the author may not be used to endorse or promote products
 *    derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
 * INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package gpx.parser;

import android.app.ProgressDialog;
import android.util.Log;

import org.jdom2.DataConversionException;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.xml.sax.InputSource;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import gpx.GPX;
import gpx.Track;
import gpx.TrackSegment;
import gpx.Waypoint;
import util.Constants;


/**
 * GPX parser based on the GPXWaypointParser XML parsing toolkit
 * 
 * @author Martin Jansen <martin@divbyzero.net>
 * @since 0.1
 * @see <a href="http://jdom.org/">GPXWaypointParser</a>
 */

public class GPXWaypointParser implements Parser {
	private Namespace ns;
	private SAXBuilder parser = new SAXBuilder();
    public ProgressDialog progressDialog;

	public GPX parse(File file) throws ParsingException {
		try {
            InputStream in = new FileInputStream(file);
            InputSource inputSource = new InputSource(new InputStreamReader(in));
            inputSource.setEncoding("ISO-8859-1");
			Document doc = parser.build(inputSource);
            return parse(doc);
			//return parseWaypoints(doc);
            //return parse(doc);
		} catch (IOException e) {
            e.printStackTrace();
			throw new ParsingException("Unable to open input", e);
		} catch (JDOMException e) {
            e.printStackTrace();
			throw new ParsingException("Unable to parse input", e);
		}
	}
	
	public GPX parse(URL url) throws ParsingException {
		try {
			Document doc = parser.build(url);
			return parse(doc);
		} catch (IOException e) {
			throw new ParsingException("Unable to open input", e);
		} catch (JDOMException e) {
			throw new ParsingException("Unable to parse input", e);
		}		
	}

    private GPX parseWaypoints(Document doc) {
        GPX gpx = new GPX();

        Element rootNode = doc.getRootElement();
        ns = rootNode.getNamespace();

        List<Element> wpts = rootNode.getChildren("wpt", ns);
        for (int i = 0; i < wpts.size(); i++) {
            //gpx.addWayPoint()
            //gpx.addTrack(parseTrack(tracks.get(i)));
            Waypoint wp = parseWaypoint(wpts.get(i));
            if(wp != null && !wp.getName().isEmpty())
                gpx.addWaypoint(wp);
        }

        return gpx;
    }

	@SuppressWarnings("unchecked")
	private GPX parse(Document doc) {
		GPX gpx = new GPX();
		Element rootNode = doc.getRootElement();
		ns = rootNode.getNamespace();

// no need to load tracks until I start using them

       // List<Element> tracks = rootNode.getChildren("trk", ns);

//        for (int i = 0; i < tracks.size(); i++) {
  //          gpx.addTrack(parseTrack(tracks.get(i)));
    //    }


        List<Element> wpts = rootNode.getChildren("wpt", ns);
        for (int i = 0; i < wpts.size(); i++) {
            Waypoint wp = parseWaypoint(wpts.get(i));
            if (wp != null && !wp.getName().isEmpty()) {
                gpx.addWaypoint(parseWaypoint(wpts.get(i)));
            }

            if(progressDialog != null)
              progressDialog.setProgress(i);
        }

        return gpx;
	}
	
	@SuppressWarnings("unchecked")
    private Track parseTrack(Element trackXML) {
        Track track = new Track();

        List<Element> segments = trackXML.getChildren("trkseg", ns);

        if(segments != null)
            Log.d(Constants.APP.TAG, "Number of segments =" + segments.size());

        for (int i = 0; i < segments.size(); i++) {
            track.addSegment(parseTrackSegment(segments.get(i)));
        }

        return track;
    }

    private Waypoint parseWaypoint(Element wptXML) {

        double latitude = 0.0;
        double longitude = 0.0;
        double elevation = 0.0;
        String nm = "";
        String desc = "";
        Waypoint waypoint = null;

        try {
            latitude = wptXML.getAttribute("lat").getDoubleValue();
            longitude = wptXML.getAttribute("lon").getDoubleValue();
        } catch (DataConversionException e) {
           // continue;
        }

        // couldn't get xpath syntax to work
        // check if label_text is set ExpertGPS main label
        if(wptXML.getChild("name", ns) == null)
        {
            Element gpxElem = wptXML.getChild("extensions", ns);
            if(gpxElem != null) {
                List<Element> elist = gpxElem.getChildren();
                //xpath.addNamespace(Namespace.getNamespace("xpns", "http://www.topografix.com/GPX/gpx_overlay/0/3");
                //Element labelElem = gpxElem.getChild("label");
                Element labElem = elist.get(0);
                if (labElem != null) {
                    Namespace labns = labElem.getNamespace();
                    if(labElem.getChild("label_text", labns) != null)
                        nm = new String(labElem.getChildText("label_text", labns));
                }
            }
        } else  if (wptXML.getChild("name", ns) != null) {
            nm = new String(wptXML.getChildText("name", ns));
        }

        if(!nm.isEmpty()) {


            if (wptXML.getChild("ele", ns) != null) {
                elevation = Double.valueOf(wptXML.getChildText("ele", ns));
            }

            waypoint = new Waypoint();
            waypoint.setName(nm);

            waypoint.setLatitude(latitude);
            waypoint.setLongitude(longitude);
            waypoint.setElevation(elevation);

            if (wptXML.getChild("time", ns) != null) {
                try {
                    SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'"); //2014-08-18T15:07:55.674Z // 2014-08-20T20:46:56Z
                    String timeTag = wptXML.getChildText("time", ns);
                    Date time = dateFormat.parse(timeTag);
                    waypoint.setTime(time);
                } catch (ParseException e) {
                }
            }

            if (wptXML.getChild("desc", ns) != null) {
                desc = new String(wptXML.getChildText("desc", ns));
                waypoint.setDesc(desc);
            } else if (wptXML.getChild("cmt", ns) != null) {
                desc = new String(wptXML.getChildText("cmt", ns));
                waypoint.setDesc(desc);
            }
            // parse name and argument TODO move to ActionWaypoint subclass
            String wptArgument = " ";
            String[] sSplits = nm.split(" ");
            if (sSplits.length > 0 && sSplits[0] != null)
                waypoint.setCommand(sSplits[0]);
            if (sSplits.length > 1 && sSplits[1] != null) {
                wptArgument = sSplits[1].trim();
                waypoint.setArgument(wptArgument.replaceAll("\"", ""));
            }

            // if we don't have an argument, try the description field....
            if (waypoint.getArgument().isEmpty() && !waypoint.getDesc().isEmpty()) {
                waypoint.setArgument(waypoint.getDesc());
            }
        } // return an null waypoint if no nm

        return waypoint;
    }


    @SuppressWarnings("unchecked")
	private TrackSegment parseTrackSegment(Element segmentXML) {
		TrackSegment segment = new TrackSegment();
		List<Element> waypoints = segmentXML.getChildren("trkpt", ns);
        if(waypoints != null)
            Log.d(Constants.APP.TAG, "trkpt size =" + waypoints.size() );

        for (int i = 0; i < waypoints.size(); i++) {
			Element pointXML = waypoints.get(i);
			double latitude = 0.0;
			double longitude = 0.0;
			double elevation = 0.0;

			try {
				latitude = pointXML.getAttribute("lat").getDoubleValue();
				longitude = pointXML.getAttribute("lon").getDoubleValue();
			} catch (DataConversionException e) {
				continue;
			}

            // skip for now
		//	if (pointXML.getChild("ele", ns) != null) {
		//		elevation = new Double(pointXML.getChildText("ele", ns));
		//	}
			
			Waypoint waypoint = new Waypoint();
			
            waypoint.setLatitude(latitude);
            waypoint.setLongitude(longitude);
			waypoint.setElevation(elevation);

			if (pointXML.getChild("time", ns) != null) {
				try {
					SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
					Date time = dateFormat.parse(pointXML.getChildText("time", ns));
					waypoint.setTime(time);				
				} catch (ParseException e) {
                    Log.d(Constants.APP.TAG, "Error parsing time");
				}				
			}

            //Log.d(Constants.APP.TAG, "Adding waypoint");
			segment.addWaypoint(waypoint);
		}

        Log.d(Constants.APP.TAG, "Adding segment");
		return segment;

	}
}
