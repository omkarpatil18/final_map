package com.bignerdranch.android.googlemaps;


import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

// managed marker cluster
//

public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener {

    GoogleMap mMap;
    ClusterManager<ClusterMarkerLocation> mClusterManager;
    ArrayList<LatLng> MarkerPoints;
    GoogleApiClient mGoogleApiClient;
    Location mLastLocation;
    Marker mCurrLocationMarker;
    LocationRequest mLocationRequest;
    AutoCompleteTextView searchField;
    Polyline polyline;
    PolylineOptions lineOptions;
    Boolean isVisible = false, isBusRouteShown = false, isDownloaded = false;
    Toolbar myToolbar;
    Menu myMenu;
    CoordinatorLayout coordinatorLayout;
    ArrayList<String> placesArray;
    ProgressDialog progress, pDialog;
    Context context;
    float searchBarPosX, searchBarPosY;

    private static final LatLng MAIN_GATE = new LatLng(13.005976, 80.242486);
    private static final LatLng JAM_BUS_STOP = new LatLng(12.986634, 80.238757);
    private static final LatLng GAJENDRA_CIRCLE_BUS_STOP = new LatLng(12.991780, 80.233772);
    private static final LatLng HSB_BUS_STOP = new LatLng(12.990925, 80.231896);
    private static final LatLng BT_BUS_STOP_1 = new LatLng(12.990287, 80.227627);
    private static final LatLng VELACHERY_GATE = new LatLng(12.987857, 80.223127);
    private static final LatLng BT_BUS_STOP_2 = new LatLng(12.989977, 80.227707);
    private static final LatLng CRC_BUS_STOP = new LatLng(12.988204, 80.230125);
    private static final LatLng TGH_BUS_STOP = new LatLng(12.986574, 80.233254);
    private static final LatLng NARMADA_BUS_STOP = new LatLng(12.986473, 80.235324);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);

        myToolbar = (Toolbar) findViewById(R.id.my_toolbar);
        setSupportActionBar(myToolbar);
        context = this;
        pDialog = new ProgressDialog(this);
        progress = new ProgressDialog(this);
        coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coord_layout);

        placesArray = new ArrayList<String>();

        getSuggestions();

        searchField = (AutoCompleteTextView) findViewById(R.id.auto_comp_tv_search);
        searchBarPosX = searchField.getX();
        searchBarPosY = searchField.getY();
        searchField.setVisibility(View.GONE);

        searchField.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            public void onItemClick(AdapterView<?> parent, View view, int position, long rowId) {
                final String selection = (String) parent.getItemAtPosition(position);
                searchField.setText(selection);
                animateSearchOut();
                setBusDeselectedIcon();
                mMap.clear();
                pDialog.setMessage("Searching...");
                pDialog.setCancelable(false);
                pDialog.show();
                Thread thread = new Thread() {
                    @Override
                    public void run() {
                        doMySearch(selection);
                    }
                };
                thread.start();
            }
        });

        searchField.setOnEditorActionListener(new TextView.OnEditorActionListener() {
                                                  @Override
                                                  public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                                                      if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                                                          final Editable selection = searchField.getText();
                                                          animateSearchOut();
                                                          setBusDeselectedIcon();
                                                          mMap.clear();
                                                          pDialog.setMessage("Searching...");
                                                          pDialog.setCancelable(false);
                                                          pDialog.show();
                                                          Thread thread = new Thread() {
                                                              @Override
                                                              public void run() {
                                                                  doMySearch(selection.toString());
                                                              }
                                                          };
                                                          thread.start();
                                                          return true;
                                                      }

                                                      return false;
                                                  }
                                              }

        );

        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)

        {
            checkLocationPermission();
        }
        // Initializing
        MarkerPoints = new ArrayList<>();

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    private void setBusDeselectedIcon() {
        if (polyline != null) polyline.setVisible(false);
        myMenu.findItem(R.id.bus_route).setIcon(R.drawable.ic_bus_deselected);
        isBusRouteShown = false;
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.options_menu, menu);
        myMenu = menu;
        // Associate searchable configuration with the SearchView

        return true;
    }

    public void doMySearch(String query) {

        final RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")//https://students.iitm.ac.in/studentsapp/map/get_location.php?
                .authority("students.iitm.ac.in")
                .appendPath("studentsapp")
                .appendPath("map")
                .appendPath("get_location.php")
                .appendQueryParameter("locname", query);
        String url = builder.build().toString();

        // Request a string response from the provided URL.
        StringRequest jsonObjReq = new StringRequest(Request.Method.GET,
                url, new Response.Listener<String>() {

            @Override
            public void onResponse(String response) {


                try {

                    JSONArray jsonArray = new JSONArray(response);
                    JSONObject jsonObject;
                    int i;
                    String locationName, locationDescription, latitude = "12.991780", longitude = "80.233772";
                    LatLng latLong;
                    for (i = 0; i < jsonArray.length(); i++) {
                        jsonObject = jsonArray.getJSONObject(i);
                        locationName = jsonObject.getString("locname");
                        locationDescription = jsonObject.getString("locdesc");
                        latitude = jsonObject.getString("lat");
                        longitude = jsonObject.getString("long");

                        latLong = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
                        mMap.addMarker(new MarkerOptions()
                                .title(locationName)
                                .snippet(locationDescription)
                                .position(latLong));
                    }
                    if (pDialog.isShowing()) pDialog.dismiss();
                    LatLng latLngGC;
                    if (jsonArray.length() == 1) {
                        latLngGC = new LatLng(Double.parseDouble(latitude), Double.parseDouble(longitude));
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngGC, 18));
                    } else {
                        latLngGC = new LatLng(12.991780, 80.233772);
                        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLngGC, 14));
                        Snackbar snackbar = Snackbar
                                .make(coordinatorLayout, "Showing all related results...", Snackbar.LENGTH_LONG);
                        snackbar.show();
                    }
                } catch (JSONException e) {

                    if (pDialog.isShowing()) pDialog.dismiss();
                    Snackbar snackbar = Snackbar
                            .make(coordinatorLayout, "No result found!", Snackbar.LENGTH_LONG);
                    snackbar.show();
                    e.printStackTrace();

                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                if (pDialog.isShowing()) pDialog.dismiss();
                VolleyLog.d("VolleyResponseError", error);
                Snackbar snackbar = Snackbar
                        .make(coordinatorLayout, "Couldn't connect to the server.", Snackbar.LENGTH_LONG);
                snackbar.show();

            }
        });
        queue.add(jsonObjReq);
    }

    private void getSuggestions() {

        final RequestQueue queue = Volley.newRequestQueue(getApplicationContext());

        Uri.Builder builder = new Uri.Builder();
        builder.scheme("https")//https://students.iitm.ac.in/studentsapp/map/get_names.php
                .authority("students.iitm.ac.in")
                .appendPath("studentsapp")
                .appendPath("map")
                .appendPath("get_names.php");
        final String url = builder.build().toString();

        StringRequest jsonObjReq = new StringRequest(Request.Method.GET,
                url, new Response.Listener<String>() {


            @Override
            public void onResponse(String response) {


                try {
                    JSONArray jsonArray = new JSONArray(response);
                    JSONObject jsonObject;
                    int i;
                    String locationName;
                    for (i = 0; i < jsonArray.length(); i++) {
                        jsonObject = jsonArray.getJSONObject(i);
                        locationName = jsonObject.getString("locname");
                        placesArray.add(locationName);
                    }
                    isDownloaded = true;
                    ArrayAdapter<String> adapter = new ArrayAdapter<String>(context,
                            android.R.layout.simple_dropdown_item_1line, placesArray);
                    searchField.setAdapter(adapter);
                } catch (JSONException e) {
                    Snackbar snackbar = Snackbar
                            .make(coordinatorLayout, "Error getting data, try again later...", Snackbar.LENGTH_LONG);
                    snackbar.show();
                    e.printStackTrace();

                }

            }
        }, new Response.ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), "Couldn't update data.", Toast.LENGTH_SHORT).show();

            }
        });
        queue.add(jsonObjReq);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera. In this case,
     * we just add a marker near Sydney, Australia.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.search_go_btn: {

                if (isVisible) {
                    animateSearchOut();
                } else {
                    if (!isDownloaded) getSuggestions();
                    animateSearchIn();
                }

                return true;
            }
            case R.id.action_settings:
                // User chose the "Settings" item, show the app settings UI...
                return true;


            case R.id.bus_route:
                // User chose the "Favorite" action, mark the current item
                // as a favorite...
                mMap.clear();
                if (!isBusRouteShown) {

                    if (polyline != null) {
                        mMap.addPolyline(lineOptions);
                        item.setIcon(R.drawable.ic_bus_selected);
                        isBusRouteShown = true;
                        setUpClusterer();
                    } else {
                        progress.setMessage("Getting bus route...");
                        progress.setCancelable(false); // disable dismiss by tapping outside of the dialog
                        progress.show();
                        MarkerPoints.add(MAIN_GATE);
                        MarkerPoints.add(JAM_BUS_STOP);

                        LatLng origin = MarkerPoints.get(0);
                        LatLng dest = MarkerPoints.get(1);
                        isBusRouteShown = true;

                        mMap.moveCamera(CameraUpdateFactory.newLatLng(NARMADA_BUS_STOP));
                        mMap.animateCamera(CameraUpdateFactory.zoomTo(11));

                        setUpClusterer();

                        // Getting URL to the Google Directions API
                        String urlString = getUrl(origin, dest);
                        FetchUrl FetchUrl = new FetchUrl();

                        // Start downloading json data from Google Directions API
                        FetchUrl.execute(urlString);
                        //move map camera
                    }
                } else {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                        item.setIcon(getResources().getDrawable(R.drawable.ic_bus_deselected, this.getTheme()));
                    }

                    polyline.setVisible(false);
                    item.setIcon(R.drawable.ic_bus_deselected);
                    isBusRouteShown = false;
                    mMap.setOnCameraIdleListener(null);
                    mMap.setOnMarkerClickListener(null);


                }

                return true;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
    }

    private void animateSearchOut() {
        if (!isVisible) return;
        searchField.animate()
                .translationX(searchBarPosX)
                .translationY(searchBarPosY)
                .setDuration(100)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        searchField.setVisibility(View.GONE);
                    }
                });
        isVisible = false;
        MenuItem item = myMenu.findItem(R.id.search_go_btn);
        item.setIcon(R.drawable.ic_search_deselected);
        searchField.setText("");

        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(searchField.getWindowToken(), 0);
    }

    private void animateSearchIn() {
        if (isVisible) return;
        searchField.animate()
                .translationYBy(myToolbar.getHeight())
                .setDuration(100)
                .setListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationStart(Animator animation) {
                        super.onAnimationStart(animation);
                        searchField.setVisibility(View.VISIBLE);
                    }
                });
        isVisible = true;
        MenuItem item = myMenu.findItem(R.id.search_go_btn);
        item.setIcon(R.drawable.ic_search_selected);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {

        LatLng testGC = new LatLng(12.991780, 80.233772);

        googleMap.setMyLocationEnabled(false);
        googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(testGC, 17));

        mMap = googleMap;
        //Initialize Google Play Services
        if (android.os.Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)
                    == PackageManager.PERMISSION_GRANTED) {
                buildGoogleApiClient();
                mMap.setMyLocationEnabled(false);
            }
        } else {
            buildGoogleApiClient();
            mMap.setMyLocationEnabled(true);
        }

    }

    private String getUrl(LatLng origin, LatLng dest) {

        // Origin of route
        String str_origin = "origin=" + origin.latitude + "," + origin.longitude;

        // Destination of route
        String str_dest = "destination=" + dest.latitude + "," + dest.longitude;

        String waypoints = "waypoints=12.991780,80.233772|12.990925,80.231896|12.990287,80.227627|12.987857,80.223127|12.989977,80.227707|12.988204,80.230125|12.986574,80.233254|12.986473,80.235324";

        // Sensor enabled
        String sensor = "sensor=false";

        // Building the parameters to the web service
        String parameters = str_origin + "&" + str_dest + "&" + waypoints + "&" + sensor;

        // Output format
        String output = "json";

        // Building the url to the web service
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;


        return url;
    }

    /**
     * A method to download json data from url
     */
    private String downloadUrl(String strUrl) throws IOException {
        String data = "";
        InputStream iStream = null;
        HttpURLConnection urlConnection = null;
        try {
            URL url = new URL(strUrl);

            // Creating an http connection to communicate with url
            urlConnection = (HttpURLConnection) url.openConnection();

            // Connecting to url
            urlConnection.connect();

            // Reading data from url
            iStream = urlConnection.getInputStream();

            BufferedReader br = new BufferedReader(new InputStreamReader(iStream));

            StringBuffer sb = new StringBuffer();

            String line = "";
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }

            data = sb.toString();
            br.close();

        } catch (Exception e) {
            if (progress.isShowing()) {
                progress.dismiss();
                isBusRouteShown = false;
                Snackbar snackbar = Snackbar
                        .make(coordinatorLayout, "Couldn't connect to internet.", Snackbar.LENGTH_LONG);
                snackbar.show();
            }
        } finally {
            iStream.close();
            urlConnection.disconnect();
        }
        return data;
    }

    // Fetches data from url passed
    private class FetchUrl extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... url) {

            // For storing data from web service
            String data = "";

            try {
                // Fetching the data from web service
                data = downloadUrl(url[0]);
            } catch (Exception e) {
                if (progress.isShowing()) {
                    progress.dismiss();
                    isBusRouteShown = false;
                    Snackbar snackbar = Snackbar
                            .make(coordinatorLayout, "Couldn't connect to internet.", Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
            }
            return data;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);

            ParserTask parserTask = new ParserTask();

            // Invokes the thread for parsing the JSON data
            parserTask.execute(result);

        }
    }

    /**
     * A class to parse the Google Places in JSON format
     */
    private class ParserTask extends AsyncTask<String, Integer, List<List<HashMap<String, String>>>> {

        // Parsing the data in non-ui thread
        @Override
        protected List<List<HashMap<String, String>>> doInBackground(String... jsonData) {

            JSONObject jObject;
            List<List<HashMap<String, String>>> routes = null;

            try {
                jObject = new JSONObject(jsonData[0]);
                DataParser parser = new DataParser();

                // Starts parsing data
                routes = parser.parse(jObject);

            } catch (Exception e) {
                if (progress.isShowing()) {
                    progress.dismiss();
                    isBusRouteShown = false;
                    Snackbar snackbar = Snackbar
                            .make(coordinatorLayout, "Error parsing data, try again later...", Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
                e.printStackTrace();
            }
            return routes;
        }

        // Executes in UI thread, after the parsing process
        @Override
        protected void onPostExecute(List<List<HashMap<String, String>>> result) {
            ArrayList<LatLng> points;
            lineOptions = null;

            // Traversing through all the routes
            if (result != null) {
                for (int i = 0; i < result.size(); i++) {
                    points = new ArrayList<>();
                    lineOptions = new PolylineOptions();
                    // Fetching i-th route
                    List<HashMap<String, String>> path = result.get(i);

                    // Fetching all the points in i-th route
                    for (int j = 0; j < path.size(); j++) {
                        HashMap<String, String> point = path.get(j);

                        double lat = Double.parseDouble(point.get("lat"));
                        double lng = Double.parseDouble(point.get("lng"));
                        LatLng position = new LatLng(lat, lng);

                        points.add(position);
                    }

                    // Adding all the points in the route to LineOptions
                    lineOptions.addAll(points);
                    lineOptions.width(6);
                    lineOptions.color(Color.GRAY);

                }
            }

            // Drawing polyline in the Google Map for the i-th route
            if (lineOptions != null) {
                polyline = mMap.addPolyline(lineOptions);
                isBusRouteShown = true;
                MenuItem item = myMenu.findItem(R.id.bus_route);
                item.setIcon(R.drawable.ic_bus_selected);
                if (progress.isShowing()) progress.dismiss();
            } else {
                if (progress.isShowing()) {
                    isBusRouteShown = false;
                    progress.dismiss();
                    Snackbar snackbar = Snackbar
                            .make(coordinatorLayout, "Error getting route, try again later...", Snackbar.LENGTH_LONG);
                    snackbar.show();
                }
            }
        }

    }

    protected synchronized void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleApiClient.connect();
    }

    @Override
    public void onConnected(Bundle bundle) {

        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000);
        mLocationRequest.setFastestInterval(1000);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_BALANCED_POWER_ACCURACY);
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleApiClient, mLocationRequest, this);
        }

    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onLocationChanged(Location location) {

        mLastLocation = location;
        if (mCurrLocationMarker != null) {
            mCurrLocationMarker.remove();
        }

        //Place current location marker
        LatLng latLng = new LatLng(location.getLatitude(), location.getLongitude());
        MarkerOptions markerOptions = new MarkerOptions();
        markerOptions.position(latLng);
        markerOptions.title("Current Position");
        markerOptions.icon(BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_MAGENTA));
        mCurrLocationMarker = mMap.addMarker(markerOptions);

        //move map camera
        mMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        mMap.animateCamera(CameraUpdateFactory.zoomTo(15));

        //stop location updates
        if (mGoogleApiClient != null) {
            LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleApiClient, this);
        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    public static final int MY_PERMISSIONS_REQUEST_LOCATION = 99;

    public boolean checkLocationPermission() {
        if (ContextCompat.checkSelfPermission(this,
                android.Manifest.permission.ACCESS_FINE_LOCATION)
                != PackageManager.PERMISSION_GRANTED) {

            // Asking user if explanation is needed
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                    android.Manifest.permission.ACCESS_FINE_LOCATION)) {

                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.

                //Prompt the user once explanation has been shown
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);


            } else {
                // No explanation needed, we can request the permission.
                ActivityCompat.requestPermissions(this,
                        new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION},
                        MY_PERMISSIONS_REQUEST_LOCATION);
            }
            return false;
        } else {
            return true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case MY_PERMISSIONS_REQUEST_LOCATION: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // permission was granted. Do the
                    // contacts-related task you need to do.
                    if (ContextCompat.checkSelfPermission(this,
                            android.Manifest.permission.ACCESS_FINE_LOCATION)
                            == PackageManager.PERMISSION_GRANTED) {

                        if (mGoogleApiClient == null) {
                            buildGoogleApiClient();
                        }
                        mMap.setMyLocationEnabled(true);
                    }

                } else {

                    // Permission denied, Disable the functionality that depends on this permission.

                    Toast.makeText(this, "permission denied", Toast.LENGTH_LONG).show();
                }
                return;
            }

            // other 'case' lines to check for other permissions this app might request.
            // You can add here other case statements according to your requirement.
        }
    }

    private void setUpClusterer() {

            // Initialize the manager with the context and the map.
            mClusterManager = new ClusterManager<>(this, mMap);


            // Point the map's listeners at the listeners implemented by the cluster
            // manager.
            mMap.setOnCameraIdleListener(mClusterManager);
            mMap.setOnMarkerClickListener(mClusterManager);

            // Add cluster items (markers) to the cluster manager.
            addItems();
            mClusterManager.setRenderer(new OwnIconRendered(this,mMap, mClusterManager));

    }


    private void addItems() {

            ClusterMarkerLocation item1 = new ClusterMarkerLocation(MAIN_GATE);
            mClusterManager.addItem(item1);
            ClusterMarkerLocation item2 = new ClusterMarkerLocation(JAM_BUS_STOP);
            mClusterManager.addItem(item2);
            ClusterMarkerLocation item3 = new ClusterMarkerLocation(GAJENDRA_CIRCLE_BUS_STOP);
            mClusterManager.addItem(item3);
            ClusterMarkerLocation item4 = new ClusterMarkerLocation(HSB_BUS_STOP);
            mClusterManager.addItem(item4);
            ClusterMarkerLocation item5 = new ClusterMarkerLocation(BT_BUS_STOP_2);
            mClusterManager.addItem(item5);
            ClusterMarkerLocation item6 = new ClusterMarkerLocation(VELACHERY_GATE);
            mClusterManager.addItem(item6);
            ClusterMarkerLocation item7 = new ClusterMarkerLocation(CRC_BUS_STOP);
            mClusterManager.addItem(item7);
            ClusterMarkerLocation item8 = new ClusterMarkerLocation(TGH_BUS_STOP);
            mClusterManager.addItem(item8);
            ClusterMarkerLocation item9 = new ClusterMarkerLocation(NARMADA_BUS_STOP);
            mClusterManager.addItem(item9);

    }
    class OwnIconRendered extends DefaultClusterRenderer<ClusterMarkerLocation> {

        public OwnIconRendered(Context context, GoogleMap map,
                               ClusterManager<ClusterMarkerLocation> clusterManager) {
            super(context, map, clusterManager);
        }

        @Override
        protected void onBeforeClusterItemRendered(ClusterMarkerLocation item, MarkerOptions markerOptions) {


            if(item.getPosition()==MAIN_GATE){
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_radio_button_checked_black_24dp))
                        .alpha(0.5f)
                        .title("MAIN GATE");
            }
            if(item.getPosition()==JAM_BUS_STOP){
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_radio_button_checked_black_24dp))
                        .alpha(0.5f)
                        .title("JAM BUS STOP");
            }
            if(item.getPosition()==GAJENDRA_CIRCLE_BUS_STOP){
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_radio_button_checked_black_24dp))
                        .alpha(0.4f)
                        .title("GAJENDRA CIRCLE BUS STOP");
            }
            if(item.getPosition()==HSB_BUS_STOP){
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_radio_button_checked_black_24dp))
                        .alpha(0.4f)
                        .title("HSB BUS STOP");
            }
            if(item.getPosition()==VELACHERY_GATE){
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_radio_button_checked_black_24dp))
                        .alpha(0.5f)
                        .title("VELACHERY GATE");
            }
            if(item.getPosition()==CRC_BUS_STOP){
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_radio_button_checked_black_24dp))
                        .alpha(0.4f)
                        .title("CRC BUS STOP");
            }
            if(item.getPosition()==TGH_BUS_STOP){
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_radio_button_checked_black_24dp))
                        .alpha(0.4f)
                        .title("TGH BUS STOP");
            }
            if(item.getPosition()==NARMADA_BUS_STOP){
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_radio_button_checked_black_24dp))
                        .alpha(0.4f)
                        .title("NARMADA BUS STOP");
            }
            if(item.getPosition()==BT_BUS_STOP_2){
                markerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_radio_button_checked_black_24dp))
                        .alpha(0.4f)
                        .title("BT BUS STOP");
            }



            super.onBeforeClusterItemRendered(item, markerOptions);
        }
    }

}
