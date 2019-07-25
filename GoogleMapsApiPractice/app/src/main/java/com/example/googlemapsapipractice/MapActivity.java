package com.example.googlemapsapipractice;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.googlemapsapipractice.API.RetrofitApi;
import com.example.googlemapsapipractice.API.RetrofitClient;
import com.example.googlemapsapipractice.Model.GoogleMapsDirectionReqsponse;
import com.example.googlemapsapipractice.Model.MyLocation;
import com.example.googlemapsapipractice.Model.Route;
import com.github.nkzawa.emitter.Emitter;
import com.github.nkzawa.socketio.client.IO;
import com.github.nkzawa.socketio.client.Socket;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class MapActivity extends FragmentActivity implements OnMapReadyCallback {

    @BindView(R.id.et_search)
    EditText et_search;

    @BindView(R.id.iv_search)
    ImageView iv_search;

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COARSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST = 1234;
    private static final float DEFAULT_MAP_ZOOM = 15f;
    private static final String TAG = "MapActivity";
    private boolean mLocationGranted = false;
    private LocationManager mLocationManager ;

    private GoogleMap mGoogleMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private LocationRequest mLocationRequest;
    private LocationCallback mLocationCallback;

    private boolean isRequestingLocationUpdates;
    private boolean isUpdatingLocation;
    private List<MyLocation> mPreviousLocations=new ArrayList<>();
    private MarkerOptions markerOptions;
    private LatLng currentLatLng;

    private static int locationNumber=2;

    private MutableLiveData<List<Route>> mRouteList=new MutableLiveData<>();

    private static String socketUrl="http://chat.socket.io";

    private Socket mSocket;{

        try {
            mSocket= IO.socket(socketUrl);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_map);
        ButterKnife.bind(this);
        //check for the location permission and get the current location
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);

        CheckLocationPermission();

        //init the location callback
        initLocationCallback();
        //init the location request
        initLocationRequest();
        //retrieve the previous locations if found
//        drawLastLocationsFromBundle(savedInstanceState);
    }


    //check for locations permissions
    private void CheckLocationPermission() {

        Log.v(TAG, "Checking For Permissions");

        String[] permissions = {FINE_LOCATION, COARSE_LOCATION};
        if (ContextCompat.checkSelfPermission(this, FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
                && ContextCompat.checkSelfPermission(this, COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {

            Log.v(TAG, "Permissions Found");
            mLocationGranted = true;
            initMap();

        } else {
            Log.v(TAG, "Permissions Not Found and asking fot them");
            mLocationGranted = false;
            ActivityCompat.requestPermissions(this, permissions, LOCATION_PERMISSION_REQUEST);

        }

        //check if gps is opened or not  ==> if not display gps needed dialog
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (!mLocationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps();
        }

    }

    //display gps needed dialog
    private void buildAlertMessageNoGps() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(getString(R.string.pleaseOpenGPS))
                .setCancelable(false)
                .setPositiveButton(getString(R.string.open), new DialogInterface.OnClickListener() {
                    public void onClick(@SuppressWarnings("unused") final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
                    }
                })
                .setNegativeButton(getString(R.string.close), new DialogInterface.OnClickListener() {
                    public void onClick(final DialogInterface dialog, @SuppressWarnings("unused") final int id) {
                        dialog.cancel();
                        finish();
                    }
                });
        final AlertDialog alert = builder.create();
        alert.show();
    }

    //check if permission is granted or not after requesting it
    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);


        mLocationGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST:
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationGranted = false;
                            return;
                        }
                    }
                    Log.v(TAG, "Permissions Granted");
                    mLocationGranted = true;
                    //initiate the map
                    initMap();

                } else {
                    Log.v(TAG, "Permissions not Granted");
                    mLocationGranted = false;
                }
                break;
        }
    }

    //init google map from the xml fragment and waits for map to be ready
    private void initMap() {

        Log.v(TAG, "Initializing Map");
        SupportMapFragment supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        supportMapFragment.getMapAsync(this);
    }

    //init google map and other features that need map to be initialized first
    @Override
    public void onMapReady(GoogleMap googleMap) {
        Log.v(TAG, "Map Loaded");
        mGoogleMap = googleMap;
        Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();

        getMyLocation();
        //init search by name feature
        initSearch();
        //init map click listener and draw marker on selected place
        initMapClickListener();
    }

    //get the current location from using Google FusedLocationProvider
    private void getMyLocation() {
        Log.v(TAG, "getting My Device Location");


        if (mLocationGranted == true) {

            @SuppressLint("MissingPermission") Task locationTask = mFusedLocationProviderClient.getLastLocation();
            locationTask.addOnCompleteListener(new OnCompleteListener() {
                @SuppressLint("MissingPermission")
                @Override
                public void onComplete(@NonNull Task task) {

                    if(task.isSuccessful()){

                        Location currentLocation = (Location) task.getResult();

                        LatLng latLng=new LatLng(currentLocation.getLatitude(),currentLocation.getLongitude());

                        currentLatLng=latLng;

                        Log.v(TAG, "Device Location Location Found ,lat: "+latLng.latitude+" lng: "+latLng.longitude);

                        moveCamera(latLng,DEFAULT_MAP_ZOOM,"My Location");

                        //save Location in Previous locations List
                        mPreviousLocations.add(new MyLocation(latLng.latitude,latLng.longitude,"First Location"));

                        //set blue point in the selected Location
                        mGoogleMap.setMyLocationEnabled(true);
                        //hide find my location button
                        mGoogleMap.getUiSettings().setMyLocationButtonEnabled(false);
                    }
                    else {
                        Log.v(TAG, "Failed to get Device Location");
                        Log.v(TAG, "Exception : "+String.valueOf(task.getException()));
                    }
                }
            });

        }
    }

    //move camera to specific point on map
    private void moveCamera(LatLng latLng ,float zoom,String title){
        Log.v(TAG, "Map Zoomed to your location");
        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng,zoom));

        addMarker(latLng,title);
    }

    //add marker to specific point on map
    private void addMarker(LatLng latLng,String title){
        MarkerOptions markerOptions =new MarkerOptions()
                .position(new LatLng(latLng.latitude,latLng.longitude))
                .title(title);

        mGoogleMap.addMarker(markerOptions);
    }

    //search for specific location on map and zoom to it if found
    private void searchLocation(String targetLocation){
        //close Keyboard first

        Log.v(TAG, "Searching For Location : "+targetLocation);

        Geocoder geocoder=new Geocoder(this);
         List<Address> addressList = null;
        try {
            addressList=geocoder.getFromLocationName(targetLocation,1);
        } catch (IOException e) {
            e.printStackTrace();
            Log.v(TAG,"GeoCoder Exception : "+e.getMessage());
            Toast.makeText(this, "problem occurred while getting Location ", Toast.LENGTH_SHORT).show();
            return;
        }

        Address searchAddress=null;
        for(int i=0;i<addressList.size();i++){
            searchAddress=addressList.get(0);

            Log.v(TAG, "Location has been found successfully : "+searchAddress.toString());
        }

        if(searchAddress!=null){

            moveCamera(new LatLng(searchAddress.getLatitude(),searchAddress.getLongitude())
                    ,DEFAULT_MAP_ZOOM
                    ,searchAddress.getAddressLine(0));

            Toast.makeText(this, "Your location has been found ", Toast.LENGTH_SHORT).show();
        }
        else {
            Toast.makeText(this, "can't find this place ", Toast.LENGTH_SHORT).show();

        }
    }

    //init search feature and enable search in edit text different events
    private void initSearch(){

        et_search.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if(actionId== EditorInfo.IME_ACTION_SEARCH||actionId==EditorInfo.IME_ACTION_DONE
                        ||event.getAction()==KeyEvent.ACTION_DOWN||event.getAction()==KeyEvent.KEYCODE_ENTER){

                    searchLocation(et_search.getText().toString());
                    return true;
                }
                else {
                    return false;
                }

            }
        });


    }


    @SuppressLint("MissingPermission")
    private void startLocationUpdate(){
        Log.v(TAG,"starting listing to locations changes");
        mFusedLocationProviderClient.requestLocationUpdates(mLocationRequest, mLocationCallback,null);
        Toast.makeText(this, "starting Tracking your location", Toast.LENGTH_SHORT).show();
    }

    private void stopLocationUpdate(){
        Log.v(TAG,"stopped listing to locations changes");
        mFusedLocationProviderClient.removeLocationUpdates(mLocationCallback);
        Toast.makeText(this, "stopped Tracking your location", Toast.LENGTH_SHORT).show();

    }

    private void initLocationCallback(){


        Log.v(TAG,"Location Update : initializing Location Callback ");
        mLocationCallback=new LocationCallback(){
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if(locationResult==null){
                    Log.v(TAG,"Location Update : No New Locations found");
                    return;
                }

                List<Location> locations=locationResult.getLocations();

                for(int i=0;i<locations.size();i++){

                    MyLocation newLocation=new MyLocation(locations.get(i).getLatitude()
                            ,locations.get(i).getLongitude(),"Location "+locationNumber);

                    locationNumber++;

                    addMarker(new LatLng(newLocation.getLat(),newLocation.getLng()),newLocation.getTitle());
                    Log.v(TAG,"Location Update : "+newLocation.getTitle()+" with lat: "+newLocation.getLat()+
                            " lng: "+newLocation.getLng());

                    //save Location in Previous locations List
                    mPreviousLocations.add(newLocation);

                }

            }
        };

    }

    private void initLocationRequest(){
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(1000*10); // 10 seconds
        mLocationRequest.setFastestInterval(1000*10); //10 seconds
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
    }

    private void hideKeyboard(){
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }

    @Override
    protected void onPause() {
        super.onPause();
        stopLocationUpdate();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(isRequestingLocationUpdates==true){
            startLocationUpdate();
        }
    }


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //save previous locations and if user is updating or not
        outState.putBoolean("isUpdating", isRequestingLocationUpdates);
        outState.putSerializable("locations", (Serializable) mPreviousLocations);
        Log.v(TAG,"Saving Locations before Configuration Change");
    }

    //get the previous locations from bundle and add marker to these locations
    private void drawLastLocationsFromBundle(Bundle inState){

        Log.v(TAG,"Retrieving Locations after Configuration Change");
        if(inState==null){
            Log.v(TAG,"Retrieving Locations after Configuration Change : No Data Found");
            return;
        }

        isRequestingLocationUpdates = inState.getBoolean("isUpdating");
        mPreviousLocations= (List<MyLocation>) inState.getSerializable("locations");

        for(MyLocation location:mPreviousLocations){

            addMarker(new LatLng(location.getLat(),location.getLng()),location.getTitle());
        }


    }

    @OnClick({R.id.iv_myLocation,R.id.iv_updateLocation,R.id.iv_search}) void myLocation(View view){
        switch (view.getId()){
            case R.id.iv_myLocation:getMyLocation(); break;
            case R.id.iv_updateLocation:handleLocationUpdateClick(); break;
            case R.id.iv_search: handleSearchLocationClick(); break;

        }
    }

    //handle search location button click==>enable search if search text not empty
    private void handleSearchLocationClick(){
        if(et_search.getText().toString().length()>0){
            searchLocation(et_search.getText().toString());
        }
    }

    //handle update location button click==>open location update and vice versa
    private void handleLocationUpdateClick(){

        if(isUpdatingLocation==true){
            stopLocationUpdate();
            isRequestingLocationUpdates=false;
        }
        else {
            startLocationUpdate();
            isRequestingLocationUpdates=true;
        }
    }

    //init the map click listener
    private void initMapClickListener(){
        mGoogleMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                mapClicked(latLng);
            }
        });
    }

    //handle map clicks
    private void mapClicked(LatLng latLng) {
//        CameraPosition cameraPosition = new CameraPosition.Builder()
//                .target(latLng)
//                .zoom(DEFAULT_MAP_ZOOM)
//                .build();
//        mGoogleMap.animateCamera(CameraUpdateFactory.newCameraPosition(cameraPosition));
//        mGoogleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, DEFAULT_MAP_ZOOM));

        getMyLocation();

        //
        getDirection(latLng);


        markerOptions = new MarkerOptions()
                .position(latLng)
                .title(getLocationAddress(latLng.latitude,latLng.longitude));
        mGoogleMap.clear();
        mGoogleMap.addMarker(markerOptions);

        Log.v(TAG,"Marker has been set to new location with : "+latLng.latitude+" lng: "+latLng.longitude);
    }

    //get the address name location of specific point
    private String getLocationAddress(double lat,double lng){
        Geocoder geocoder =new Geocoder(this);
        List<Address> locations;
        Address targetLocation;
        String address = "";
        try {
            locations = geocoder.getFromLocation(lat, lng, 1);
            targetLocation=locations.get(0);
            String city=targetLocation.getAddressLine(0);
            String state=targetLocation.getAdminArea();
            String country=targetLocation.getCountryName();
            String name=targetLocation.getFeatureName();
            address+=name+", "+city+", "+state+", "+country;

        } catch (IOException e) {

            Log.v(TAG,"GeoCoder Exception : "+e.getMessage());
            Toast.makeText(this, "problem occurred while getting Location ", Toast.LENGTH_SHORT).show();
            return "Not Found";
        }

        return address;
    }

    //draw direction from origin point to another point
    private void getDirection(LatLng destinationLatLng){

        getDirectionPolyline(currentLatLng,destinationLatLng).observe(this, new Observer<List<Route>>() {
            @Override
            public void onChanged(List<Route> routes) {
                if(routes!=null){

                    List<LatLng> points=Route.getLatLngList(routes);
                    drawPolyLine(points);
                }
            }
        });
    }

    //get direction points used to draw polyline "route"
    private LiveData<List<Route>> getDirectionPolyline(LatLng originLatLng , LatLng destinationLatLng){

        Log.v(TAG,"requesting google direction routes from google server: ");
        String url=getDirectionUrl(originLatLng,destinationLatLng,"driving");

        Retrofit retrofit= RetrofitClient.getInstance();
        RetrofitApi retrofitApi = retrofit.create(RetrofitApi.class);

        retrofitApi.getMapRoute(url).enqueue(new Callback<GoogleMapsDirectionReqsponse>() {
            @Override
            public void onResponse(Call<GoogleMapsDirectionReqsponse> call, Response<GoogleMapsDirectionReqsponse> response) {

                if(response.code()!=200&&response.body()!=null){
                    Log.v(TAG,"response success: "+response.body().toString());
                    mRouteList.setValue(response.body().getRoutes());
                }
                else {
                    Log.v(TAG,"response failure: "+response.body().getErrorMessage());
                    Toast.makeText(MapActivity.this, response.body().getErrorMessage(), Toast.LENGTH_LONG).show();
                }
            }

            @Override
            public void onFailure(Call<GoogleMapsDirectionReqsponse> call, Throwable t) {
                Log.v(TAG,"response Exception: "+t.getMessage());
            }
        });

        return mRouteList;
    }

    //get the direction url between two points
    private String getDirectionUrl(LatLng originLatLng ,LatLng destinationLatLng,String directionMode){

        Log.v(TAG,"Location : getting Location Url");
        String origin="origin="+originLatLng.latitude+","+originLatLng.latitude;
        String destination="destination="+destinationLatLng.latitude+","+destinationLatLng.latitude;
        String mode="mode="+directionMode;
        String parameters=origin+"&"+destination+"&"+mode;
        String output="json";
        String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters + "&key=" + getString(R.string.GoogleApiKey);

        Log.v(TAG,"Location : Location Url :"+url);
        return url;
    }

    //draw polyline "route on the map"
    public void drawPolyLine(List<LatLng> points) {
        if (points == null) {
            Log.e("Draw Line", "got null as parameters");
            return;
        }

        Polyline line = mGoogleMap.addPolyline(new PolylineOptions().width(3).color(Color.RED));
        line.setPoints(points);
    }

    private void initSocketIO(){

        mSocket.connect();
        mSocket.on("Location Update",OnLocationUpdate);

        Log.v(TAG,"Socket.IO: has been connected" );
    }

    //send Location Updates to server
    private void sendLocationToServer(double lat,double lng){

        JSONObject latLngObject=new JSONObject();
        try {
            latLngObject.put("lat",lat);
            latLngObject.put("lng",lng);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        mSocket.emit("Location Update",latLngObject.toString());
    }

    //Listen to Location Change
    Emitter.Listener OnLocationUpdate =new Emitter.Listener() {
        @Override
        public void call(Object... args) {

            if(args==null){
                return;
            }
            Log.v(TAG,"new Location update from server");

            //parse the json response
            JSONObject data= (JSONObject) args[0];
            String lat;
            String lng;
            try {
                lat = data.getString("lat");
                lng = data.getString("lng");
            } catch (JSONException e) {
                return;
            }

            //update ui with the new response
        }
    };


    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSocket.disconnect();
        mSocket.off("Location Update");

        Log.v(TAG,"Socket.IO: has disconnected" );
    }
}
