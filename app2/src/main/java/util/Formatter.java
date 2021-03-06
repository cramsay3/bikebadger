/*
 * Copyright (C) 2013 jonas.oreland@gmail.com
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package util;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.content.res.Resources;
import android.os.Build;
import android.preference.PreferenceManager;
import android.text.format.DateUtils;

import java.util.Locale;

import us.theramsays.bikebadger.app2.R;

@TargetApi(Build.VERSION_CODES.FROYO)
public class Formatter implements OnSharedPreferenceChangeListener {

    Context context = null;
    Resources resources = null;
    SharedPreferences sharedPreferences = null;
    java.text.DateFormat dateFormat = null;
    java.text.DateFormat timeFormat = null;
    //HRZones hrZones = null;

    boolean km = false;
    String base_unit = "km";
    double base_meters = km_meters;

    public final static double km_meters = 1000.0;
    public final static double mi_meters = 1609.34;
    public final static double FEETS_PER_METER = 3.2808;
    public final static double mi_feet = 5280;

    public static final int CUE = 1; // for text to speech
    public static final int CUE_SHORT = 2; // brief for tts
    public static final int CUE_LONG = 3; // long for tts
    public static final int TXT = 4; // same as TXT_SHORT
    public static final int TXT_SHORT = 5; // brief for printing
    public static final int TXT_LONG = 6; // long for printing

    public Formatter(Context ctx) {
        context = ctx;
        resources = ctx.getResources();
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(ctx);
        sharedPreferences.registerOnSharedPreferenceChangeListener(this);

        dateFormat = android.text.format.DateFormat.getDateFormat(ctx);
        timeFormat = android.text.format.DateFormat.getTimeFormat(ctx);
        // hrZones = new HRZones(context);

        setUnit();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences,
                                          String key) {
        if (key != null && "pref_unit".contentEquals(key))
            setUnit();
    }

    private void setUnit() {
        km = getUseKilometers(sharedPreferences, null);

        if (km) {
            base_unit = "km";
            base_meters = km_meters;
        } else {
            base_unit = "mi";
            base_meters = mi_meters;
        }
    }

    public static boolean getUseKilometers(SharedPreferences prefs, Editor editor) {
        boolean _km = true;
        String unit = prefs.getString("pref_unit", null);
        if (unit == null)
            _km = guessDefaultUnit(prefs, editor);
        else if (unit.contentEquals("km"))
            _km = true;
        else if (unit.contentEquals("mi"))
            _km = false;
        else
            _km = guessDefaultUnit(prefs, editor);

        return _km;
    }

    private static boolean guessDefaultUnit(SharedPreferences prefs, Editor editor) {
        String countryCode = Locale.getDefault().getCountry();
        System.err.println("guessDefaultUnit: countryCode: " + countryCode);
        if (countryCode == null)
            return true; // km;
        if ("US".contentEquals(countryCode) ||
                "GB".contentEquals(countryCode)) {
            if (editor != null)
                editor.putString("pref_unit", "mi");
            return false;
        } else {
            if (editor != null)
                editor.putString("pref_unit", "km");
        }
        return true;
    }

    public double getUnitMeters() {
        return this.base_meters;
    }

    public static double getUnitMeters(SharedPreferences prefs) {
        if (getUseKilometers(prefs, null))
            return km_meters;
        else
            return mi_meters;
    }

    public String getUnitString() {
        return this.base_unit;
    }

    public String format(int target, Dimension dimension, double value) {
        switch (dimension) {
            case DISTANCE:
                return formatDistance(target, Math.round(value));
            case TIME:
                return formatElapsedTime(target, Math.round(value));
            case PACE:
                return formatPace(target, value);
            case HR:
                return formatHeartRate(target, value);
            case HRZ:
                return formatHeartRateZone(target, value);
            case SPEED:
                // TODO
                return "";
        }
        return "";
    }

    public String formatElapsedTime(int target, long seconds) {
        switch (target) {
            case CUE:
            case CUE_SHORT:
                return cueElapsedTime(seconds, false);
            case CUE_LONG:
                return cueElapsedTime(seconds, true);
            case TXT:
            case TXT_SHORT:
                return DateUtils.formatElapsedTime(seconds);
            case TXT_LONG:
                return txtElapsedTime(seconds);
        }
        return "";
    }

    private String cueElapsedTime(long seconds, boolean includeDimension) {
        long hours = 0;
        long minutes = 0;
        if (seconds >= 3600) {
            hours = seconds / 3600;
            seconds -= hours * 3600;
        }
        if (seconds >= 60) {
            minutes = seconds / 60;
            seconds -= minutes * 60;
        }
        StringBuilder s = new StringBuilder();
        if (hours > 0) {
            includeDimension = true;
            s.append(hours)
                    .append(" ")
                    .append(resources.getString(hours > 1 ? R.string.cue_hours : R.string.cue_hour));
        }
        if (minutes > 0) {
            if (hours > 0)
                s.append(" ");
            includeDimension = true;
            s.append(minutes)
                    .append(" ")
                    .append(resources.getString(minutes > 1 ? R.string.cue_minutes
                            : R.string.cue_minute));
        }
        if (seconds > 0) {
            if (hours > 0 || minutes > 0)
                s.append(" ");

            if (includeDimension) {
                s.append(seconds)
                        .append(" ")
                        .append(resources.getString(seconds > 1 ? R.string.cue_seconds
                                : R.string.cue_second));
            } else {
                s.append(seconds);
            }
        }
        return s.toString();
    }

    private String txtElapsedTime(long seconds) {
        long hours = 0;
        long minutes = 0;
        if (seconds >= 3600) {
            hours = seconds / 3600;
            seconds -= hours * 3600;
        }
        if (seconds >= 60) {
            minutes = seconds / 60;
            seconds -= minutes * 60;
        }
        StringBuilder s = new StringBuilder();
        if (hours > 0) {
            s.append(hours).append(" ").append(resources.getString(R.string.txt_elapsed_h));
        }
        if (minutes > 0) {
            if (hours > 0)
                s.append(" ");
            if (hours > 0 || seconds > 0)
                s.append(minutes).append(" ").append(resources.getString(R.string.txt_elapsed_m));
            else
                s.append(minutes).append(" ").append(resources.getString(R.string.txt_elapsed_min));
        }
        if (seconds > 0) {
            if (hours > 0 || minutes > 0)
                s.append(" ");
            s.append(seconds).append(" ").append(resources.getString(R.string.txt_elapsed_s));
        }
        return s.toString();
    }


    public String formatHeartRate(int target, double heart_rate) {
        switch (target) {
            case CUE:
            case CUE_SHORT:
            case CUE_LONG:
                return Integer.toString((int) Math.round(heart_rate)) + " "
                        + resources.getString(R.string.txt_heartrate_bpm);
            case TXT:
            case TXT_SHORT:
            case TXT_LONG:
                return Integer.toString((int) Math.round(heart_rate));
        }
        return "";
    }

    private String formatHeartRateZone(int target, double hrZone) {
        switch (target) {
            case TXT:
            case TXT_SHORT:
                return Integer.toString((int) Math.round(hrZone));
            case TXT_LONG:
                return Double.toString(Math.round(10.0 * hrZone) / 10.0);
            case CUE_SHORT:
                return resources.getString(R.string.txt_dimension_heartratezone) + " "
                        + Integer.toString((int) Math.floor(hrZone));
            case CUE:
            case CUE_LONG:
                return resources.getString(R.string.txt_dimension_heartratezone) + " "
                        + Double.toString(Math.floor(10.0 * hrZone) / 10.0);
        }
        return "";
    }

    /**
     * Format pace
     *
     * @param target
     * @param seconds_per_meter
     * @return
     */
    public String formatPace(int target, double seconds_per_meter) {
        switch (target) {
            case CUE:
            case CUE_SHORT:
            case CUE_LONG:
                return cuePace(seconds_per_meter);
            case TXT:
            case TXT_SHORT:
                return txtPace(seconds_per_meter, false);
            case TXT_LONG:
                return txtPace(seconds_per_meter, true);
        }
        return "";
    }


    private String txtPace(double seconds_per_meter, boolean includeUnit) {
        long val = Math.round(base_meters * seconds_per_meter);
        String str = DateUtils.formatElapsedTime(val);
        if (includeUnit == false)
            return str;
        else {
            int res = km ? R.string.txt_distance_km : R.string.txt_distance_mi;
            return str + "/" + resources.getString(res);
        }
    }

    private String cuePace(double seconds_per_meter) {
        long seconds_per_unit = Math.round(base_meters * seconds_per_meter);
        long hours_per_unit = 0;
        long minutes_per_unit = 0;
        if (seconds_per_unit >= 3600) {
            hours_per_unit = seconds_per_unit / 3600;
            seconds_per_unit -= hours_per_unit * 3600;
        }
        if (seconds_per_unit >= 60) {
            minutes_per_unit = seconds_per_unit / 60;
            seconds_per_unit -= minutes_per_unit * 60;
        }
        StringBuilder s = new StringBuilder();
        if (hours_per_unit > 0) {
            s.append(hours_per_unit)
                    .append(" ")
                    .append(resources.getString(hours_per_unit > 1 ? R.string.cue_hours
                            : R.string.cue_hour));
        }
        if (minutes_per_unit > 0) {
            if (hours_per_unit > 0)
                s.append(" ");
            s.append(minutes_per_unit)
                    .append(" ")
                    .append(resources.getString(minutes_per_unit > 1 ? R.string.cue_minutes
                            : R.string.cue_minute));
        }
        if (seconds_per_unit > 0) {
            if (hours_per_unit > 0 || minutes_per_unit > 0)
                s.append(" ");
            s.append(seconds_per_unit)
                    .append(" ")
                    .append(resources.getString(seconds_per_unit > 1 ? R.string.cue_seconds
                            : R.string.cue_second));
        }
        s.append(" " + resources.getString(km ? R.string.cue_perkilometer : R.string.cue_permile));
        return s.toString();
    }

    /**
     * @param target
     * @param seconds_since_epoch
     * @return
     */
    public String formatDateTime(int target, long seconds_since_epoch) {
        // ignore target
        StringBuffer s = new StringBuffer();
        s.append(dateFormat.format(seconds_since_epoch * 1000)); // takes
        // milliseconds
        // as argument
        s.append(" ");
        s.append(timeFormat.format(seconds_since_epoch * 1000));
        return s.toString();
    }

    /**
     * @param target
     * @param meters
     * @return
     */
    public String formatDistance(int target, long meters) {
        switch (target) {
            case CUE:
            case CUE_LONG:
            case CUE_SHORT:
                return cueDistance(meters, false);
            case TXT:
            case TXT_SHORT:
                return cueDistance(meters, true);
            case TXT_LONG:
                return Long.toString(meters) + " m";
        }
        return null;
    }

    private String cueDistance(long meters, boolean txt) {
        double base_val = km_meters; // 1km
        double decimals = 2;
        int res_base = R.string.cue_kilometer;
        int res_base_multi = R.string.cue_kilometers;
        if (km == false) {
            base_val = mi_meters;
            res_base = R.string.cue_mile;
            res_base_multi = R.string.cue_miles;
        }

        int res_meter = R.string.cue_meter;
        int res_meters = R.string.cue_meters;

        if (txt) {
            if (km) {
                res_base = R.string.txt_distance_km;
                res_base_multi = R.string.txt_distance_km;
            } else {
                res_base = R.string.txt_distance_mi;
                res_base_multi = R.string.txt_distance_mi;
            }

            res_meter = R.string.txt_distance_m;
            res_meters = R.string.txt_distance_m;
        }

        StringBuffer s = new StringBuffer();
        if (meters >= base_val) {
            double base = ((double) meters) / base_val;
            double val = round(base, decimals);
            s.append(val).append(" ")
                    .append(resources.getString(base > 1 ? res_base_multi : res_base));
        } else {
            s.append(meters);
            s.append(" ").append(resources.getString(meters > 1 ? res_meters : res_meter));
        }
        return s.toString();
    }

    public static String FormatDistanceMiles(double feet) {
        double base_val = mi_feet; // 1 mile
        double decimals = 2;
        int res_base = R.string.cue_mile;
        int res_base_multi = R.string.cue_miles;

        res_base = R.string.txt_distance_mi;
        res_base_multi = R.string.txt_distance_mi;

        StringBuffer s = new StringBuffer();
        if (feet >= base_val) {
            double base = ((double) feet) / base_val;
            double val = round(base, decimals);
            s.append(FormatDecimal(val));
            s.append(" ");
            //s.append("ft");
            s.append(" ");
            s.append((base > 1 ? "miles" : "mile"));
        } else {
            int toFeet = (int) feet;
            s.append(Integer.toString(toFeet));
            s.append(" ft");
        }

        return s.toString();
    }

    public String formatRemaining(int target, Dimension dimension, double value) {
        switch (dimension) {
            case DISTANCE:
                return formatRemainingDistance(target, value);
            case TIME:
                return formatRemainingTime(target, value);
            case PACE:
            case SPEED:
                break;
            case HR:
                break;
            default:
                break;
        }
        return "";
    }

    public String formatRemainingTime(int target, double value) {
        return formatElapsedTime(target, Math.round(value));
    }

    public String formatRemainingDistance(int target, double value) {
        return formatDistance(target, Math.round(value));
    }

    public String formatName(String first, String last) {
        if (first != null && last != null)
            return first + " " + last;
        else if (first == null && last != null)
            return last;
        else if (first != null && last == null)
            return first;
        return "";
    }

    public String formatTime(int target, long seconds_since_epoch) {
        return timeFormat.format(seconds_since_epoch * 1000);
    }

    public static double round(double base, double decimals) {
        double exp = Math.pow(10, decimals);
        return Math.round(base * exp) / exp;
    }

    public static String FormatDuration(int durationSeconds) {
        int seconds = durationSeconds % 60;
        int minutes = ((durationSeconds - seconds) / 60) % 60;
        int hours = (durationSeconds - (minutes * 60) - seconds) / 3600;
        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public static String FormatDecimal(double val) {
        return String.format("%.2f", val);
    }
}

