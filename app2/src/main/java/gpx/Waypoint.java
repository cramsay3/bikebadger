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
package gpx;

import android.location.Location;
import android.os.Parcel;
import android.os.Parcelable;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Way point for the GPS tracks.
 * 
 * <p>Way points represent the smallest unit of which GPS track are
 * composed.  They are made up of a coordinate, a time stamp, an
 * optional name and an elevation.</p>
 * 
 * @author Martin Jansen <martin@divbyzero.net>
 * @since 0.1
 */

public class Waypoint implements Parcelable {

    private Date dtg;
    private double longitude = 0.0;
    private double latitude = 0.0;
	private String name = "";
	private double elevation = .0;
    private String desc = "";
    private boolean triggered = false;

    public String key; // map key TODO get this out into a MAP<>

    // action waypoint TODO make a sub class
    private String command = "";
    private String argument = "";

    public String getCommand() {
        return command;
    }

    public void setCommand(String command) {
        this.command = command;
    }

    public String getArgument() {
        return argument;
    }

    public void setArgument(String argument) {
        this.argument = argument;
    }



    public int describeContents() {
        return 0;
    }

    public Waypoint()
    {
    }

    public Waypoint(String name, String desc, Location location)
    {
        setLatitude(location.getLatitude());
        setLongitude(location.getLongitude());
        setName(name);
        setDesc(desc);
        if(location.hasAltitude())
            setElevation(location.getAltitude());
        long epoch = location.getTime();
        // time may not have been set...
        // therfore set it to current time
        if(epoch < 1) {
            epoch = System.currentTimeMillis();
        }
        Date date = new Date( epoch );
        setTime( date );

    }

    public void writeToParcel(Parcel out, int flags) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        out.writeString(dateFormat.format(getTime()));
        out.writeDouble(getLatitude());
        out.writeDouble(getLongitude());
        out.writeString(getName());
        out.writeString(getCommand());
        out.writeString(getArgument());
        out.writeDouble(getElevation());
        out.writeString(getDesc());
        //out.writeInt((int) triggered) // TODO
    }

    private Waypoint(Parcel in) {
        String dtg = in.readString();
        try {
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
            Date dtgFormated = dateFormat.parse(dtg);
        } catch (ParseException e) {
        }

        setLatitude( in.readDouble() );
        setLongitude( in.readDouble() );
        setName (in.readString() );
        setCommand(in.readString());
        setArgument(in.readString());
        setElevation( in.readDouble() );
        setDesc( in.readString() );
    }

    public static final Creator<Waypoint> CREATOR
            = new Creator<Waypoint>() {
        public Waypoint createFromParcel(Parcel in) {
            return new Waypoint(in);
        }

        public Waypoint[] newArray(int size) {
            return new Waypoint[size];
        }
    };


    public double getLongitude() {
        return longitude;
    }

    /**
     * Sets the longitude of the coordinate
     *
     * @param longitude the longitude of the coordinate
     */
    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    /**
     * Returns the value for the latitude of the coordinate
     *
     * @return the latitude of the coordinate
     */
    public double getLatitude() {
        return latitude;
    }

    /**
     * Sets the latitude of the coordinate
     *
     * @param latitude the latitude of the coordinate
     */
    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

	/**
	 * Sets the time stamp of the way point.
	 * 
	 * <p>The time stamp denotes the point in time when a way point was
	 * recorded. The library does not assume anything regarding the use
	 * of time zones. Instead time stamps are accepted "as is".</p>
	 *  
	 * @param time the time stamp of the way point
	 */
	public void setTime(Date time) {
		this.dtg = time;
	}

	/**
	 * Returns the time stamp of the way point.
	 * 
	 * @return the time stamp of the way point
	 */
	public Date getTime() {
		return dtg;
	}

	/**
	 * Sets the name of the way point.
	 * 
	 * <p>Optionally a way point can be label with a name in order to
	 * describe it further.</p>
	 * 
	 * @param name the name of the waypoint
	 */
	public void setName(String name) {
		this.name = name;
	}

    public void setDesc(String desc) { this.desc = desc; }

	/**
	 * Returns the name of the way point.
	 * 
	 * @return the name of the way point
	 */
	public String getName() {
		return name;
	}

    public String getDesc() { return desc; }

	/**
	 * Sets the elevation of the way point in meters
	 * 
	 * <p>Internally the library expects the elevation to be in meters
	 * instead of feet or anything else. No conversion is applied to
	 * ensure this though.</p>
	 * 
	 * @param elevation the way point's elevation in meters
	 */
	public void setElevation(double elevation) {
		this.elevation = elevation;
	}

	/**
	 * Returns the elevation of the way point
	 * 
	 * @return the way point's elevation in meters
	 */
	public double getElevation() {
		return elevation;
	}

    public boolean IsTriggered() { return triggered; }

    public void SetTriggered(boolean onoff) { triggered = onoff; }

	/**
	 * Calculates the distance between this way point and another one
	 * 
	 * <p>In order to calculate the distance, the Spherical Law of Cosines
	 * is used. An equatorial radius of 6,378.137 kilometers is assumed.</p> 
	 * 
	 * @see <a href="http://en.wikipedia.org/wiki/Spherical_law_of_cosines">Wikipedia on the Spherical Law Of Cosines</a>
	 * @see <a href="http://www.movable-type.co.uk/scripts/latlong.html">Implementation notes</a>
	 * @param otherPoint The other way point
	 * @return the distance in meters
	 */
	public double calculateDistanceTo(Waypoint otherPoint) {
		// According to http://en.wikipedia.org/wiki/Earth_radius#Mean_radii
		// earth has an equatorial radius of 6,378.137 kilometers. 
		final int R = 6378137;

		if (otherPoint == null) {
			return 0.0;
		}

        double result = Math.acos(
				Math.sin(Math.toRadians(getLongitude())) * Math.sin(Math.toRadians(otherPoint.getLatitude())) +
                Math.cos(Math.toRadians(getLongitude())) * Math.cos(Math.toRadians(otherPoint.getLatitude())) *
                Math.cos(Math.toRadians(otherPoint.getLongitude() - getLongitude()))) * R;
	    return result * 3.2808; // feet
    }

    public double calculateDistanceTo(Location location) {
        // According to http://en.wikipedia.org/wiki/Earth_radius#Mean_radii
        // earth has an equatorial radius of 6,378.137 kilometers.
        final int R = 6378137;
        double lat = (double)Math.round(location.getLatitude() * 1000000) / 1000000;
        double lng = (double)Math.round(location.getLongitude() * 1000000) / 1000000;
        double result = Math.acos(
                Math.sin(Math.toRadians(getLatitude())) * Math.sin(Math.toRadians(lat)) +
                        Math.cos(Math.toRadians(getLatitude())) * Math.cos(Math.toRadians(lat)) *
                                Math.cos(Math.toRadians(lng - getLongitude()))) * R;
        return result * 3.2808; // feet
    }
}
