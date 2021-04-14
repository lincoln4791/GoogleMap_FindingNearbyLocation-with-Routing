package com.example.srarchingnearbyplaceswitrouting;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.directions.route.AbstractRouting;
import com.directions.route.Route;
import com.directions.route.RouteException;
import com.directions.route.Routing;
import com.directions.route.RoutingListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
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
import com.google.android.libraries.places.api.Places;
import com.google.android.libraries.places.api.model.Place;
import com.google.android.libraries.places.widget.Autocomplete;
import com.google.android.libraries.places.widget.AutocompleteActivity;
import com.google.android.libraries.places.widget.model.AutocompleteActivityMode;
import com.google.gson.JsonObject;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity implements OnMapReadyCallback, GoogleApiClient.OnConnectionFailedListener,
        RoutingListener {

    private ImageButton btn_search;
    private Button btn_setpolyLine;
    private Spinner spinner;
    private EditText et_searchBar;
    private SupportMapFragment supportMapFragment;
    private FusedLocationProviderClient fusedLocationProviderClient;
    private GoogleMap gMap;
    private double myLat;
    private double myLng;
    private LatLng myLatLng,destinationLatLng;
    private MarkerOptions myMarkerOptions;
    private List<M_Map> listPlaceDetails;
    private List<Polyline> polylines;
    private LocationManager locationManager;
    private LocationRequest locationRequest;
    private Marker userLocationMarker;
    private Address lastAddress;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        //*****************************View Bindings*******************************//
        ImageView iv_info = findViewById(R.id.iv_info_retrofitMap);
        btn_search = findViewById(R.id.btn_search_RatrofitMap);
        et_searchBar = findViewById(R.id.et_searchBar_retrofitMap);
        btn_setpolyLine = findViewById(R.id.btn_setPoliline_retrofitMap);
        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
        supportMapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.retrofitMap);






        //******************************Initializations****************************//
        initSpinner();
        et_searchBar.setFocusable(false);
        listPlaceDetails = new ArrayList<>();
        polylines = new ArrayList<>();
        myMarkerOptions = new MarkerOptions();
        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        supportMapFragment.getMapAsync(this);

        locationRequest = LocationRequest.create()
                .setInterval(1000)
                .setFastestInterval(1000)
                .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);






        //*************************** Click Listeners****************************//
        btn_search.setOnClickListener(v -> {
            //searching for nearby places
            fecthNearbyLocation();
        });

        btn_setpolyLine.setOnClickListener(v -> {
            //setting polyline in selected places
            findRoutes(myLatLng,destinationLatLng);
        });

        iv_info.setOnClickListener(v -> {
            DisplayLocationInformations();
        });

        et_searchBar.setOnClickListener(v -> getPlaceSuggestions());

    }





    private void DisplayLocationInformations() {

        //*************It will display selected location Details when eye button clicked

        TextView tv_infoDetails = findViewById(R.id.tv_infoDetails_retrofitMap);
        tv_infoDetails.setVisibility(View.VISIBLE);

        if(lastAddress != null){
            String address = lastAddress.getAddressLine(0);
            tv_infoDetails.setText(address);

            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    tv_infoDetails.setVisibility(View.GONE);
                }
            },3000);
        }
        else{
            Toast.makeText(this, "Long Press To select an address", Toast.LENGTH_SHORT).show();
        }
    }




    private void getPlaceSuggestions() {
        //***************** it will display places suggestions when we search for locations
        Places.initialize(MainActivity.this,"Your API KEY");
        List<Place.Field> placeLIst = Arrays.asList(Place.Field.ADDRESS,Place.Field.LAT_LNG, Place.Field.NAME);
        Intent intent = new Autocomplete.IntentBuilder(AutocompleteActivityMode.OVERLAY,placeLIst).build(MainActivity.this);
        startActivityForResult(intent,100);
    }




    private void initSpinner() {

        //******************* Initializing Spinner for selecting places

        spinner = findViewById(R.id.spinner_retrofitMap);
        ArrayAdapter spinnerAdapter = new ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, getResources().getStringArray(R.array.types));
        spinner.setAdapter(spinnerAdapter);


    }

    private void fecthNearbyLocation() {

        //******************* It will fetch all nearby expected locations from google map api

        int index = spinner.getSelectedItemPosition();
        String text = getResources().getStringArray(R.array.types)[index];
        String myLatStr = String.valueOf(myLat);
        String myLngStr = String.valueOf(myLng);
        String myLocationStr = myLatStr + "," + myLngStr;
        Retrofit retrofit = new Retrofit
                .Builder()
                .baseUrl(EndPoints.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        CallBackMap callBackMap = retrofit.create(CallBackMap.class);
        Call<JsonObject> call = callBackMap.callNearbyPlaces(text, myLocationStr,"YOUR_API_KEY");
        Log.d("tag", "location : " + myLocationStr);
        call.enqueue(new Callback<JsonObject>() {
            @Override
            public void onResponse(Call<JsonObject> call, Response<JsonObject> response) {
                if (response.code() == 200) {
                    gMap.clear();
                    listPlaceDetails.clear();
                    gMap.addMarker(myMarkerOptions);
                    JsonObject jsonObject = response.body();
                    try {
                        JSONObject object = new JSONObject(String.valueOf(jsonObject));
                        JSONArray resultArray = object.getJSONArray("results");
                        for (int i = 0; i < resultArray.length(); i++) {
                            JSONObject object1 = resultArray.getJSONObject(i);

                            JSONObject geometry = object1.getJSONObject("geometry");
                            JSONObject location = geometry.getJSONObject("location");
                            String lat1 = location.getString("lat");
                            String lng1 = location.getString("lng");
                            LatLng latLng1 = new LatLng(Double.parseDouble(lat1), Double.parseDouble(lng1));
                            String placeName = object1.getString("name");

                            M_Map m_map = new M_Map(lat1, lng1, placeName);
                            listPlaceDetails.add(m_map);

                            MarkerOptions markerOptions1 = new MarkerOptions();
                            markerOptions1.title(placeName);
                            markerOptions1.position(latLng1);
                            gMap.addMarker(markerOptions1);
                            //Toast.makeText(MapForRetrofit.this, "done", Toast.LENGTH_SHORT).show();

                        }
                        Log.d("tag", "passed : ");
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.d("tag", "error : " + e.getMessage());
                    }


                } else {
                    Toast.makeText(MainActivity.this, "failed" + response.message(), Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<JsonObject> call, Throwable t) {
                Toast.makeText(MainActivity.this, "failed2 : " + t.getMessage(), Toast.LENGTH_SHORT).show();

            }
        });

    }








    LocationCallback locationCallback = new LocationCallback() {
        @Override
        public void onLocationResult(@NonNull LocationResult locationResult) {
            super.onLocationResult(locationResult);
            if(gMap != null){
                setUserMarker(locationResult.getLastLocation());
            }
            else{
                Toast.makeText(MainActivity.this, "Location is Null", Toast.LENGTH_SHORT).show();
            }
        }
    };









    private void setUserMarker(Location location) {

        //********************* It will place marker in users current location

        LatLng latLng = new LatLng(location.getLatitude(),location.getLongitude());
        myLat = location.getLatitude();
        myLng = location.getLongitude();
        myLatLng = latLng;
        if(userLocationMarker==null){
            MarkerOptions options = new MarkerOptions();
            options.icon(BitmapDescriptorFactory.fromResource(R.drawable.car_yellow));
            options.anchor((float) 0.5,(float) 0.5);
            options.rotation(location.getBearing());
            options.position(latLng);
            userLocationMarker = gMap.addMarker(options);
            gMap.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng,16));
        }
        else{
            userLocationMarker.setRotation(location.getBearing());
            userLocationMarker.setPosition(latLng);
            //gMap.moveCamera(CameraUpdateFactory.newLatLng(latLng));
        }
    }


    private void startLocationUpdates() {

        //***************** Sending current location updates constantly

        if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationProviderClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());
        }
        else{
            askForAccessFineLocation();
            Toast.makeText(this, "Permission Needed", Toast.LENGTH_SHORT).show();
        }

    }







    private void askForAccessFineLocation() {

        ActivityCompat.requestPermissions(MainActivity.this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION},100);

    }






    private void stopLocationUpdate(){

        //****************Stop Sending current Location updates

        fusedLocationProviderClient.removeLocationUpdates(locationCallback);
    }







    private void setDestinationMarker(LatLng latLng){

        //*********************** It will put a marker when user long press in a location

        gMap.clear();
        myMarkerOptions.position(myLatLng);
        myMarkerOptions.icon(BitmapDescriptorFactory.fromResource(R.drawable.car_yellow));
        gMap.addMarker(myMarkerOptions);
        destinationLatLng = latLng;
        MarkerOptions options = new MarkerOptions();
        Geocoder geocoder = new Geocoder(MainActivity.this, Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocation(latLng.latitude,latLng.longitude,1);
            if(addressList.size()>0){
                options.title(addressList.get(0).getAddressLine(0));
                options.position(latLng);
                gMap.addMarker(options);
            }
            else{
                Toast.makeText(this, "Address Empty", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }




    @Override
    public void onRoutingSuccess(ArrayList<Route> route, int shortestRouteIndex) {

        //***********************Drawing polyline between two places

        CameraUpdate center = CameraUpdateFactory.newLatLng(myLatLng);
        CameraUpdate zoom = CameraUpdateFactory.zoomTo(16);
        if(polylines!=null) {
            polylines.clear();
        }
        PolylineOptions polyOptions = new PolylineOptions();
        LatLng polylineStartLatLng=null;
        LatLng polylineEndLatLng=null;


        polylines = new ArrayList<>();
        //add route(s) to the map using polyline
        for (int i = 0; i <route.size(); i++) {

            if(i==shortestRouteIndex)
            {
                polyOptions.color(getResources().getColor(R.color.black));
                polyOptions.width(15);
                polyOptions.addAll(route.get(shortestRouteIndex).getPoints());
                Polyline polyline = gMap.addPolyline(polyOptions);
                polylineStartLatLng=polyline.getPoints().get(0);
                int k=polyline.getPoints().size();
                polylineEndLatLng=polyline.getPoints().get(k-1);
                polylines.add(polyline);

            }
            else {

            }

        }

        //Add Marker on route starting position
        MarkerOptions startMarker = new MarkerOptions();
        startMarker.position(polylineStartLatLng);
        startMarker.title("My Location");
        //gMap.addMarker(startMarker);

        //Add Marker on route ending position
        MarkerOptions endMarker = new MarkerOptions();
        endMarker.position(polylineEndLatLng);
        endMarker.title("Destination");
        //gMap.addMarker(endMarker);
    }








    public void findRoutes(LatLng Start, LatLng End)

            //****************** Fetching the shortest Route between two places

    {
        if(Start==null || End==null) {
            Toast.makeText(MainActivity.this,"Unable to get location",Toast.LENGTH_LONG).show();
        }
        else
        {
            Routing routing = new Routing.Builder()
                    .travelMode(AbstractRouting.TravelMode.DRIVING)
                    .withListener(this)
                    .alternativeRoutes(true)
                    .waypoints(Start, End)
                    .key("AIzaSyDOfp0Dzn5ydzgYN2-s96q44cJvbz7OHuY")  //also define your api key here.
                    .build();
            routing.execute();
        }
    }




    private void saveLocationsDetails(LatLng latLng){

        //*********************collecting and saving the destination Location for routing

        Geocoder geocoder = new Geocoder(MainActivity.this,Locale.getDefault());
        try {
            List<Address> addressList = geocoder.getFromLocation(latLng.latitude,latLng.longitude,1);
            if(addressList.size() > 0){
                lastAddress = addressList.get(0);
            }
            else{
                Toast.makeText(this, "No address Found", Toast.LENGTH_SHORT).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

    }




    @Override
    public void onMapReady(GoogleMap googleMap) {
        gMap = googleMap;
        //gMap.setMyLocationEnabled(true);
        gMap.setTrafficEnabled(true);
        gMap.setMapType(googleMap.MAP_TYPE_NORMAL);

        gMap.setOnMapLongClickListener(latLng -> {
            saveLocationsDetails(latLng);
            setDestinationMarker(latLng);
        });

        gMap.setOnMapClickListener(latLng -> {

        });
        //getPresentLocation();
        startLocationUpdates();
    }




    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if(requestCode==100){
            if(grantResults.length>0 && grantResults.equals(RESULT_OK)){
                startLocationUpdates();
            }
        }
    }




    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == 100 && resultCode == RESULT_OK){
            Place place = Autocomplete.getPlaceFromIntent(data);
            et_searchBar.setText(place.getAddress());
        }

        else if(requestCode == 100 && resultCode == AutocompleteActivity.RESULT_ERROR){
            Status status = Autocomplete.getStatusFromIntent(data);
            Toast.makeText(this, "Error : "+data, Toast.LENGTH_SHORT).show();
        }
    }




    @Override
    public void onRoutingCancelled() {

    }







    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }







    @Override
    protected void onStart() {
        super.onStart();
        if(ActivityCompat.checkSelfPermission(MainActivity.this,Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED){
            startLocationUpdates();

        }
        else{
            askForAccessFineLocation();
        }

    }







    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopLocationUpdate();
    }



    @Override
    public void onRoutingFailure(RouteException e) {

    }



    @Override
    public void onRoutingStart() {

    }







}