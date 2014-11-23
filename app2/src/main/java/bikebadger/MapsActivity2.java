package bikebadger;

import android.annotation.TargetApi;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.NavUtils;
import android.text.InputType;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.UiSettings;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;

import java.util.StringTokenizer;

import gpx.Waypoint;
import us.theramsays.bikebadger.app2.R;
import util.Constants;
import util.LatLngUtils;
import util.MyToast;

public class MapsActivity2 extends FragmentActivity  {

    //private GoogleMap mMap; // Might be null if Google Play services APK is not available.
    private RideManager mRideManager;
    private Menu mMenu;
    MenuItem [] types2items = { null, null, null, null, null };
    Polyline mPolyline;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("Bike Bud", "MapsActivity2::onCreate()");
        setContentView(R.layout.activity_maps_activity2);
        Integer resultCode = GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (resultCode == ConnectionResult.SUCCESS) {
            //Do what you want
        } else if (
                resultCode == ConnectionResult.SERVICE_MISSING ||
                    resultCode == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED ||
                    resultCode == ConnectionResult.SERVICE_DISABLED) {

            Dialog dialog = GooglePlayServicesUtil.getErrorDialog(resultCode,this, 1);
            if (dialog != null) {
                //This dialog will help the user update to the latest GooglePlayServices
                dialog.show();
            }
        }

        // get the RideManager Singleton
        mRideManager = RideManager.get(this);

        setupActionBar();
        setUpMapIfNeeded();

        initListeners();

        // keep the screen on
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    private void initListeners() {

        mRideManager.mMap.setOnMarkerClickListener(new GoogleMap.OnMarkerClickListener() {

            @Override
            public boolean onMarkerClick(final Marker marker) {
                Log.e("TESTING", "on Marker click: " + marker.getTitle());

                if (!marker.isInfoWindowShown())
                    marker.showInfoWindow();
                else
                    marker.hideInfoWindow();

                return true;
            }
        });

        mRideManager.mMap.setOnInfoWindowClickListener(new GoogleMap.OnInfoWindowClickListener() {
            @Override
            public void onInfoWindowClick(final Marker marker) {

                //Intent intent = new Intent(MapsActivity2.this, CreditsActivity.class);
                //startActivity(intent);
                String sMarkerName = marker.getTitle();
                String sName = "";
                String sArg = "";
                StringTokenizer tokens = new StringTokenizer(sMarkerName, " ");
                int tcount = tokens.countTokens();
                if (tcount > 0) {
                    sName = tokens.nextToken();
                    if (tcount > 1) {
                        sArg = tokens.nextToken();
                    }
                }

                AlertDialog.Builder alert = new AlertDialog.Builder(MapsActivity2.this);
                final EditText einput = new EditText(MapsActivity2.this);

                if(sName.equals("START") || sName.equals("LAP")) {
                    alert.setTitle("Target Average Speed");
                    alert.setMessage("Edit the target average speed.");
                    einput.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL | InputType.TYPE_NUMBER_FLAG_SIGNED);
                    einput.setText(sArg);

                    final String sWpName = sName;
                    alert.setView(einput);
                    alert.setPositiveButton("Ok", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            String sValue = einput.getText().toString();
                            marker.setTitle(sWpName + " " + sValue);
                            String id = marker.getId();
                            String sKey = id.substring(1);
                            int iID;
                            try {
                                iID = Integer.parseInt(sKey);
                            } catch (NumberFormatException nfe) {
                                iID = 0;
                                Log.d("RunTracker", "Error parsing mSpeedIntervalSeconds");
                            }

                            mRideManager.mWaypoints.get(iID).setName(sWpName + " " + sValue);
                        }
                    });

                    alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Canceled.
                        }
                    });
                    alert.show();
                    marker.hideInfoWindow();
                    // marker.showInfoWindow();
                } // if START
            }
        });

        //mRideManager.mMap.setOnMar
        mRideManager.mMap.setOnMapLongClickListener(new GoogleMap.OnMapLongClickListener() {

            @Override
            public void onMapLongClick(LatLng latLng) {

                final float calculatedDist[] = { 5, 15, 22, 40, 65, 162, 240, 625, 1080, 1860, 1900, 2000, 2000, 2000, 2000, 2000, 2000, 2000, 2000, 2000, 2000, 2000 };
                Intent waypointIntent = new Intent(mRideManager.mAppContext, WaypointActivity.class);
                Location loc = new Location(LocationManager.GPS_PROVIDER);

                loc.setLatitude(latLng.latitude);
                loc.setLongitude(latLng.longitude);

                //mRideManager.mMap

                if(mRideManager != null && mRideManager.mWaypoints != null) {
                    for (int idx = 0; idx < (mRideManager.mWaypoints.size()); idx++) {
                        //dist = mWaypoints.get(idx).calculateDistanceTo(curLocation);
                        Waypoint wp = mRideManager.mWaypoints.get(idx);
                        double lat2 = (wp.getLatitude() * 1000000) / 1000000;
                        double lng2 = (wp.getLongitude() * 1000000) / 1000000;
                        double results[] = {0, 0, 0};
                        LatLngUtils.computeDistanceAndBearing(latLng.latitude, latLng.longitude, lat2, lng2, results);
                        double dist = results[0] * 3.2808; // feet
                        CameraPosition cp= mRideManager.mMap.getCameraPosition();
                        float zoom = cp.zoom;
                        float maxzoom = mRideManager.mMap.getMaxZoomLevel();
                        float calcdist = calculatedDist[((int)maxzoom - (int)zoom )];
                        Log.d(Constants.APP.TAG, "zoom=" +zoom+ ", dist= " + dist + ", calcdist=" + calcdist);
                        if(dist < calcdist)  {
                            MyToast.Show(mRideManager.mAppContext,"Can't yet edit existing waypoints", Color.BLACK);
                            //TODO Add the ability to edit waypoints
                            // waypointIntent.putExtra("waypoint", wp);
                        }
                    }
                }
                waypointIntent.putExtra("location", loc );
                startActivityForResult(waypointIntent, Constants.APP.ACTION_WAYPOINT_REQUEST_CODE);
            }
        });

        // when the location changes move camera if speed is > 1...
        mRideManager.mMap.setOnMyLocationChangeListener(new GoogleMap.OnMyLocationChangeListener() {
            @Override
            public void onMyLocationChange(Location location) {
                double speed = 0;
                if(location.hasSpeed()) {
                    speed = location.getSpeed();
                }
                // if we are moving then zoom to the current location always.
                if(speed > 1) {
                    mRideManager.mMap.moveCamera(CameraUpdateFactory.newLatLng(new LatLng(
                            location.getLatitude(), location.getLongitude())));
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu)
    {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_maps, menu);
        mMenu = menu;
        int maptype = mRideManager.mMap.getMapType();

        types2items[0] = null;
        types2items[GoogleMap.MAP_TYPE_NORMAL] = mMenu.findItem(R.id.map_type_normal);
        types2items[GoogleMap.MAP_TYPE_SATELLITE] = mMenu.findItem(R.id.map_type_sat);
        types2items[GoogleMap.MAP_TYPE_TERRAIN] = mMenu.findItem(R.id.map_type_terrain);
        types2items[GoogleMap.MAP_TYPE_HYBRID] = mMenu.findItem(R.id.map_type_hybrid);

        types2items[maptype].setChecked(true);

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        switch (id) {
            case (android.R.id.home):
                 // This ID represents the Home or Up button. In the case of this
                // activity, the Up button is shown. Use NavUtils to allow users
                // to navigate up one level in the application structure. For
                // more details, see the Navigation pattern on Android Design:
                //
                // http://developer.android.com/design/patterns/navigation.html#up-vs-back
                //
                // TODO: If Settings has multiple levels, Up should navigate up
                // that hierarchy.
                // TODO: This causes bad things to happen
                //NavUtils.navigateUpFromSameTask(this);
                Intent intent = NavUtils.getParentActivityIntent(this);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP|Intent.FLAG_ACTIVITY_SINGLE_TOP);
                NavUtils.navigateUpTo(this, intent);
                break;
            case R.id.settings:
            Intent setting_intent = new Intent(this, SettingsActivity.class);
            startActivity(setting_intent);
            break;
            case R.id.menu_item_gpx_file:
                //mRideManager.LoadGpxFile
                break;
           case R.id.map_type_normal:
                mRideManager.mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
                mRideManager.mPrefsEditor.putInt(RideManager.PREF_MAP_TYPE, GoogleMap.MAP_TYPE_NORMAL);
                mRideManager.mPrefsEditor.commit();
                item.setChecked(true);
                break;
            case R.id.map_type_sat:
                mRideManager.mMap.setMapType(GoogleMap.MAP_TYPE_SATELLITE);
                mRideManager.mPrefsEditor.putInt(RideManager.PREF_MAP_TYPE, GoogleMap.MAP_TYPE_SATELLITE);
                mRideManager.mPrefsEditor.commit();
                item.setChecked(true);
                break;
            case R.id.map_type_terrain:
                mRideManager.mMap.setMapType(GoogleMap.MAP_TYPE_TERRAIN);
                mRideManager.mPrefsEditor.putInt(RideManager.PREF_MAP_TYPE, GoogleMap.MAP_TYPE_TERRAIN);
                mRideManager.mPrefsEditor.commit();
                item.setChecked(true);
                break;
            case R.id.map_type_hybrid:
                mRideManager.mMap.setMapType(GoogleMap.MAP_TYPE_HYBRID);
                mRideManager.mPrefsEditor.putInt(RideManager.PREF_MAP_TYPE, GoogleMap.MAP_TYPE_HYBRID);
                mRideManager.mPrefsEditor.commit();
                item.setChecked(true);
                break;
            case R.id.about:
                Intent creditsIntent = new Intent(this, CreditsActivity.class);
                startActivity(creditsIntent);
        }

        return super.onOptionsItemSelected(item);
    }


    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    private void setupActionBar() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            // Show the Up button in the action bar.
            try {
                getActionBar().setDisplayHomeAsUpEnabled(true);
            } catch (Exception e)
            {
                Log.d("Bike Badger", "setDisplayHomeAsUpEnabled threw an exception");
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        setUpMapIfNeeded();
        Log.d("Bike Bud", "MapsActivity2::onResume()");
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d("Bike Bud", "MapsActivity2::onStop()");
        mRideManager.mMap = null;
    }

    /**
     * Sets up the map if it is possible to do so (i.e., the Google Play services APK is correctly
     * installed) and the map has not already been instantiated.. This will ensure that we only ever
     * call  once when  not null.
     * <p>
     * If it isn't installed {@link SupportMapFragment} (and
     * {@link com.google.android.gms.maps.MapView MapView}) will show a prompt for the user to
     * install/update the Google Play services APK on their device.
     * <p>
     * A user can return to this FragmentActivity after following the prompt and correctly
     * installing/updating/enabling the Google Play services. Since the FragmentActivity may not
     * have been completely destroyed during this process (it is likely that it would only be
     * stopped or paused), {@link #onCreate(Bundle)} may not be called again so we should call this
     * method in {@link #onResume()} to guarantee that it will be called.
     */
    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the map.
        if (mRideManager.mMap == null) {
            // Try to obtain the map from the SupportMapFragment.
  //          SupportMapFragment mapFrag = (SupportMapFragment) getSupportFragmentManager()
    //                .findFragmentById(R.id.map);
            android.support.v4.app.FragmentManager myFragmentManager = getSupportFragmentManager();
            SupportMapFragment mySupportMapFragment = (SupportMapFragment)myFragmentManager.findFragmentById(R.id.map);

            mRideManager.mMap = mySupportMapFragment.getMap();
            // Check if we were successful in obtaining the map.
            if (mRideManager.mMap != null) {
                mRideManager.mMap.setMyLocationEnabled(true);
                UiSettings uis = mRideManager.mMap.getUiSettings();
                uis.setCompassEnabled(true);
               // mRideManager.mMap.set
                mRideManager.SetPreferenceMapType();

                zoomToCoverAllWaypoints();

                if(mRideManager.mPolylineOptions == null && mRideManager.mTrkpts != null && !mRideManager.mTrkpts.isEmpty()) {
                    mRideManager.mPolylineOptions = new PolylineOptions();
                    mRideManager.mPolylineOptions.addAll(mRideManager.mTrkpts);
                    mRideManager.mPolylineOptions.width(6);
                    mRideManager.mPolylineOptions.color(Color.BLUE);
                  }

                //              if(mRideManager.mPolylineOptions == null)
     //           {
     //               File gpxFile = new File(mRideManager.mCurrentGPXPath);
                    //ArrayList<LatLng> lls = (ArrayList<LatLng>) GPXTrackParser.getPoints(gpxFile);
     //               GPXTrackParser gr = new GPXTrackParser(mRideManager);
     //               gr.execute(gpxFile);
     //           }

                //ArrayList<LatLng> lls = gr.get();
                //ArrayList<LatLng> lls = new ArrayList<LatLng>();
                //lls.add(new LatLng(51.5, -0.1));
                //lls.add(new LatLng(40.7, -74.0));
               // PolylineOptions po = new PolylineOptions();
               // po.width(6);
                //po.color(Color.BLUE);
                //po.addAll(lls);
               // po.add(new LatLng(51.5, -0.1));
               // mPolyline = mRideManager.mMap.addPolyline(po);

            }
        }
    }


    private void zoomToCoverAllWaypoints()
    {
        if(mRideManager == null || mRideManager.mMap == null)
            return;  // bail

         mRideManager.mMap.setOnCameraChangeListener(new GoogleMap.OnCameraChangeListener() {
            @Override
            public void onCameraChange(CameraPosition arg0) {
                LatLngBounds.Builder builder = new LatLngBounds.Builder();
                // Move camera.
                if (mRideManager.mWaypoints != null && mRideManager.mWaypoints.size() > 0) {
                    for (int idx = 0; idx < (mRideManager.mWaypoints.size()); idx++) {
                        Waypoint wp = mRideManager.mWaypoints.get(idx);
                        if(wp != null) {
                            LatLng ll = new LatLng(wp.getLatitude(), wp.getLongitude());
                            builder.include(ll);
                            Marker marker = AddMarkerToMap(idx, ll);

                            String id = marker.getId();
                            // TODO BAD BAD BAD. Create a HashMap or something!
                            mRideManager.mWaypoints.get(idx).key = id;
                            Log.d("MapActivity2", "id = " + id);
                        }
                    }

                      LatLngBounds bounds = builder.build();
                      mRideManager.mMap.moveCamera(CameraUpdateFactory.newLatLngBounds(bounds, 14));
                    // Remove listener to prevent position reset on camera move.
                    mRideManager.mMap.setOnCameraChangeListener(null);
                      // no waypoints or empty
                } else if(mRideManager.GetLastKnowLocation() != null) {
                    Location location = mRideManager.mCurrentLocation;
                    mRideManager.mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(
                            new LatLng(location.getLatitude(), location.getLongitude()), 13));

                    CameraPosition cameraPosition = new CameraPosition.Builder()
                            .target(new LatLng(location.getLatitude(), location.getLongitude()))      // Sets the center of the map to location user
                            .zoom(12)                   // Sets the zoom
                                    //.bearing(90)                // Sets the orientation of the camera to east
                                    //.tilt(40)                   // Sets the tilt of the camera to 30 degrees
                            .build();                   // Creates a CameraPosition from the builder
                    mRideManager.mMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
                }

                if(mRideManager.mPolylineOptions != null)
                {
                    mRideManager.mMap.addPolyline(mRideManager.mPolylineOptions);
                }
                // Remove listener to prevent position reset on camera move.
                mRideManager.mMap.setOnCameraChangeListener(null);
            }
        });
    }

    public Marker AddMarkerToMap(int idx, LatLng ll) {
        MarkerOptions mo = null;

        if (mRideManager.mWaypoints.get(idx).getName().startsWith("START") ||
                mRideManager.mWaypoints.get(idx).getName().startsWith("LAP"))
        {
            //Polyline polyline = new Polyline();

            mo = new MarkerOptions()
                    .position(ll)
                    .title(mRideManager.mWaypoints.get(idx).getName())
                    .snippet("Click to edit...")
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.start_blue)
                    );
        } else if (mRideManager.mWaypoints.get(idx).getName().startsWith("SPEED")) {
            mo = new MarkerOptions()
                    .position(ll)
                    .title(mRideManager.mWaypoints.get(idx).getName())
                    .snippet(mRideManager.mWaypoints.get(idx).getDesc())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.speed_blue)
                    );
        } else if (mRideManager.mWaypoints.get(idx).getName().startsWith("STRAVA")) {
            mo = new MarkerOptions()
                    .position(ll)
                    .title(mRideManager.mWaypoints.get(idx).getName())
                    .snippet(mRideManager.mWaypoints.get(idx).getDesc())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.blue_strava)
                    );
        } else if (mRideManager.mWaypoints.get(idx).getName().startsWith("TASKER")) {
            mo = new MarkerOptions()
                    .position(ll)
                    .title(mRideManager.mWaypoints.get(idx).getName())
                    .snippet(mRideManager.mWaypoints.get(idx).getDesc())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.tasker_blue)
                    );
        } else if (mRideManager.mWaypoints.get(idx).getName().startsWith("PLAY") ||
                mRideManager.mWaypoints.get(idx).getName().startsWith("PLAYLIST")) {
            mo = new MarkerOptions()
                    .position(ll)
                    .title(mRideManager.mWaypoints.get(idx).getName())
                    .snippet(mRideManager.mWaypoints.get(idx).getDesc())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.music_blue)
                    );

        } else if (mRideManager.mWaypoints.get(idx).getName().startsWith("PROMPT")) {
            mo = new MarkerOptions()
                    .position(ll)
                    .title(mRideManager.mWaypoints.get(idx).getName())
                    .snippet(mRideManager.mWaypoints.get(idx).getDesc())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.prompt_blue)
                    );
        } else if (mRideManager.mWaypoints.get(idx).getName().startsWith("MUTE") ||
                mRideManager.mWaypoints.get(idx).getName().startsWith("VOLUME") ) {
            mo = new MarkerOptions()
                    .position(ll)
                    .title(mRideManager.mWaypoints.get(idx).getName())
                    .snippet(mRideManager.mWaypoints.get(idx).getDesc())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.quiet_blue)
                    );
        } else {
            mo = new MarkerOptions()
                    .position(ll)
                    .title(mRideManager.mWaypoints.get(idx).getName())
                    .icon(BitmapDescriptorFactory.fromResource(R.drawable.blank_blue)
                    );
        }

        Marker marker = mRideManager.mMap.addMarker(mo);

        return marker;
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {

        switch (requestCode) {
            case Constants.APP.ACTION_WAYPOINT_REQUEST_CODE:
                if (resultCode == this.RESULT_OK) {
                    if (data != null) {
                        final Location location = data.getParcelableExtra("location");
                        String command = data.getStringExtra("command");
                        final String arg = data.getStringExtra("argument");

                        mRideManager.AddNewWaypoint(command, arg, location);
                        // enable the save menu
                        mRideManager.SetDirtyGPX(true);
                        int idx = mRideManager.mWaypoints.size() - 1;
                        final LatLng lloc = new LatLng(location.getLatitude(), location.getLongitude());

                        AddMarkerToMap(idx, lloc);
                    }
                }
                break;

        }
    }

}
