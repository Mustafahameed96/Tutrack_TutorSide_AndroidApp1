package com.example.seekm.uitrial;

import android.*;
import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.system.ErrnoException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.example.seekm.uitrial.Models.PlaceInfo;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.places.AutocompletePrediction;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.PlaceBuffer;
import com.google.android.gms.location.places.Places;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class MapsActivity extends FragmentActivity implements OnMapReadyCallback,


        GoogleApiClient.OnConnectionFailedListener, GoogleMap.OnInfoWindowClickListener, GoogleMap.OnMarkerDragListener {

    SharedPreferences Profile_preferences ;
    FirebaseAuth mAuth;
    DatabaseReference databaseReference;

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
       // Toast.makeText(this, "Map is Ready", Toast.LENGTH_SHORT).show();
        Log.d(TAG, "onMapReady: map is ready");
        mMap = googleMap;

        if (mLocationPermissionsGranted) {
            getDeviceLocation();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                return;
            }
//            mMap.setMyLocationEnabled(true);
            mMap.getUiSettings().setMyLocationButtonEnabled(false);
            mMap.getUiSettings().setMapToolbarEnabled(false);
        }
    }

    private static final String TAG = "MapActivity";

    private static final String FINE_LOCATION = Manifest.permission.ACCESS_FINE_LOCATION;
    private static final String COURSE_LOCATION = Manifest.permission.ACCESS_COARSE_LOCATION;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1234;
    private static final float DEFAULT_ZOOM = 16.5f;
    private static final LatLngBounds LAT_LNG_BOUNDS = new LatLngBounds(
            new LatLng(-40, -168), new LatLng(71, 136));
    //widgets
    private AutoCompleteTextView mSearchText;
    private ImageButton mNext, mGps;

    //vars
    private double latitude, longitude;
    private Boolean mLocationPermissionsGranted = false;
    private GoogleMap mMap;
    private FusedLocationProviderClient mFusedLocationProviderClient;
    private PlaceAutocompleteAdapter mPlaceAutocompleteAdapter;
    private GoogleApiClient mGoogleApiClient;
    private PlaceInfo mPlace;


    FirebaseDatabase myDB;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        mSearchText = (AutoCompleteTextView) findViewById(R.id.input_search);
        mGps = (ImageButton) findViewById(R.id.ic_gps);
        mNext = (ImageButton)findViewById(R.id.next);
        getLocationPermission();

        Profile_preferences = getApplicationContext().getSharedPreferences("Profile_Preferecens",0);
        mAuth = FirebaseAuth.getInstance();

    }

    private void init() {

        Log.d(TAG, "init: initializing");
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addApi(Places.GEO_DATA_API)
                .addApi(Places.PLACE_DETECTION_API)
                .enableAutoManage(this, this)
                .build();
        mSearchText.setOnItemClickListener(mAutocompleteClickListener);

        mPlaceAutocompleteAdapter = new PlaceAutocompleteAdapter(this, mGoogleApiClient,
                LAT_LNG_BOUNDS, null);

        mSearchText.setAdapter(mPlaceAutocompleteAdapter);
        mSearchText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionId, KeyEvent keyEvent) {
                if (actionId == EditorInfo.IME_ACTION_SEARCH
                        || actionId == EditorInfo.IME_ACTION_DONE
                        || keyEvent.getAction() == KeyEvent.ACTION_DOWN
                        || keyEvent.getAction() == KeyEvent.KEYCODE_ENTER) {

                    //execute our method for searching
                    geoLocate();
                }

                return false;
            }
        });

        mGps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Log.d(TAG, "onClick:  Clicked gps icon");
                if (mLocationPermissionsGranted){
                    getDeviceLocation();
                }
                else {
                    try {
                        Log.d(TAG, "getLocationPermission: getting location permissions");
                        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION};

                        if (ContextCompat.checkSelfPermission(MapsActivity.this,
                                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                            if (ContextCompat.checkSelfPermission(MapsActivity.this,
                                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                                mLocationPermissionsGranted = true;
                                initMap();
                            } else {
                                ActivityCompat.requestPermissions(MapsActivity.this,
                                        permissions,
                                        LOCATION_PERMISSION_REQUEST_CODE);
                            }
                        } else {
                            ActivityCompat.requestPermissions(MapsActivity.this,
                                    permissions,
                                    LOCATION_PERMISSION_REQUEST_CODE);
                        }

                    }catch (Error e) {
                        Log.d(TAG, "onClick: Error on Click" + e.getMessage());
                    }
                }
            }
        });

        mNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (longitude!=0.0d && latitude!=0.0d) {
                    Log.d(TAG, "global lat and long: " + latitude);






                   // Intent intent = new Intent(MapsActivity.this, Result.class);
                    String longitudeStr, latitudeStr;
                    latitudeStr = String.valueOf(latitude);
                    longitudeStr = String.valueOf(longitude);

                    SharedPreferences.Editor editor = Profile_preferences.edit();
                    editor.putString("Latitude",latitudeStr);
                    editor.putString("Longititude",longitudeStr);

                    editor.apply();

                   String First_Name = Profile_preferences.getString("First_Name",null);
                   String Last_Name  =       Profile_preferences.getString("Last_Name",null);
                   String Email =          Profile_preferences.getString("Email",null);
                   String password =           Profile_preferences.getString("Password",null);
                   String DateOfBirth =             Profile_preferences.getString("Date_Of_Birth",null);
                   String Gender =          Profile_preferences.getString("Gender",null);
                   String profile_Image_Url =         Profile_preferences.getString("Profile_Image_Url",null);
                   String Education_Board =        Profile_preferences.getString("Education_Board",null);
                   String  Class_Grade   =      Profile_preferences.getString("Class_Grade",null);
                   String School_private =         Profile_preferences.getString("School_private",null);
                   String  Field_OfStudy=           Profile_preferences.getString("Field_OfStudy",null);
                   String Latest_Qualification =         Profile_preferences.getString("Latest_Qualification",null);
                   String Longitude =  Profile_preferences.getString("Longititude",null);
                   String Latitude  =        Profile_preferences.getString("Latitude",null);




                   databaseReference = FirebaseDatabase.getInstance().getReference("users");

                   String UserId= databaseReference.push().getKey();
                    UserClass New_Profile= new UserClass(First_Name,Last_Name,Email,password,DateOfBirth,Gender,profile_Image_Url,Education_Board,Class_Grade,School_private,Field_OfStudy,Latest_Qualification,Longitude,Latitude);
                            databaseReference
                            .child(FirebaseAuth.getInstance().getCurrentUser().getUid())
                            .setValue(New_Profile)
                            .addOnCompleteListener(new OnCompleteListener<Void>() {
                                @Override
                                public void onComplete(@NonNull Task<Void> task) {

                                    if (task.isSuccessful()){
                                        Toast.makeText(MapsActivity.this,"You've been registered successfully.",Toast.LENGTH_SHORT);
                                        startActivity(new Intent(MapsActivity.this,ProfileActivity.class));
                                        finishAfterTransition();

                                    }

                                    else {

                                        Toast.makeText(getApplicationContext(),task.getException().getMessage(),Toast.LENGTH_SHORT);
                                    }


                                }
                            });






                 //   startActivity(new Intent(ProfileBuilder.this,MapsActivity.class));
//                    intent.putExtra("Longitude", latitudeStr);
//                    intent.putExtra("Latitude", longitudeStr);
//                    startActivity(intent);
                }
            }
        });
    }

    private void geoLocate() {
        Log.d(TAG, "geoLocate: geolocating");

        String searchString = mSearchText.getText().toString();

        Geocoder geocoder = new Geocoder(MapsActivity.this);
        List<Address> list = new ArrayList<>();
        try {
            list = geocoder.getFromLocationName(searchString, 1);
        } catch (IOException e) {
            Log.e(TAG, "geoLocate: IOException: " + e.getMessage());
        }

        if (list.size() > 0) {
            Address address = list.get(0);

            Log.d(TAG, "geoLocate: found a location: " + address.toString());
            //Toast.makeText(this, address.toString(), Toast.LENGTH_SHORT).show();
            latitude = address.getLatitude();
            longitude = address.getLongitude();
            moveCameraToSearchLocation(new LatLng(latitude, longitude), DEFAULT_ZOOM);

        }
    }

    private void getDeviceLocation() {
        Log.d(TAG, "getDeviceLocation: getting the devices current location");
        mFusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(this);
        try {
            if (mLocationPermissionsGranted) {
                final Task location = mFusedLocationProviderClient.getLastLocation();
                location.addOnCompleteListener(new OnCompleteListener() {
                    @Override
                    public void onComplete(@NonNull Task task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "onComplete: found location!");
                            Location currentLocation = (Location) task.getResult();
                            latitude = currentLocation.getLatitude();
                            longitude = currentLocation.getLongitude();
                            moveCameraToMyLocation(new LatLng(latitude, longitude),
                                    DEFAULT_ZOOM);

                        } else {
                            Log.d(TAG, "onComplete: current location is null");
                            Toast.makeText(MapsActivity.this, "unable to get current location", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }

        } catch (SecurityException e) {
            Log.e(TAG, "getDeviceLocation: SecurityException: " + e.getMessage());
        }
    }

    private void moveCameraToMyLocation(LatLng latLng, float zoom) {
        mMap.clear();
        mSearchText.setText("");
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        Marker marker= mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Is this you? Hard press me to locate precisely")
                .draggable(true)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pinpoint)));
        marker.showInfoWindow();
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMarkerDragListener(this);

    }

    private void moveCameraToSearchLocation(LatLng latLng, float zoom) {
        mMap.clear();
        Log.d(TAG, "moveCamera: moving the camera to: lat: " + latLng.latitude + ", lng: " + latLng.longitude);
        mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, zoom));
        Marker marker= mMap.addMarker(new MarkerOptions()
                .position(latLng)
                .title("Is this you? Hard press me to locate precisely")
                .draggable(true)
                .icon(BitmapDescriptorFactory.fromResource(R.drawable.ic_pinpoint)));
        marker.showInfoWindow();
        hideSoftKeyboard();
        mMap.setOnInfoWindowClickListener(this);
        mMap.setOnMarkerDragListener(this);

    }

    private void initMap() {
        Log.d(TAG, "initMap: initializing map");
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);

        mapFragment.getMapAsync(MapsActivity.this);


    }

    private void getLocationPermission() {
        Log.d(TAG, "getLocationPermission: getting location permissions");
        String[] permissions = {Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION};

        if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (ContextCompat.checkSelfPermission(this.getApplicationContext(),
                    COURSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                mLocationPermissionsGranted = true;
                initMap();
            } else {
                ActivityCompat.requestPermissions(this,
                        permissions,
                        LOCATION_PERMISSION_REQUEST_CODE);
            }
        } else {
            ActivityCompat.requestPermissions(this,
                    permissions,
                    LOCATION_PERMISSION_REQUEST_CODE);
        }
        initMap();
        init();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        Log.d(TAG, "onRequestPermissionsResult: called.");
        mLocationPermissionsGranted = false;

        switch (requestCode) {
            case LOCATION_PERMISSION_REQUEST_CODE: {
                if (grantResults.length > 0) {
                    for (int i = 0; i < grantResults.length; i++) {
                        if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                            mLocationPermissionsGranted = false;
                            Log.d(TAG, "onRequestPermissionsResult: permission failed");
                            return;
                        }
                    }
                    Log.d(TAG, "onRequestPermissionsResult: permission granted");
                    mLocationPermissionsGranted = true;
                    //initialize our map
                    initMap();
                }
            }
        }
    }

    @Override
    public void onInfoWindowClick(Marker marker) {
      //  Toast.makeText(this, marker.getTitle(), Toast.LENGTH_LONG).show();
    }

    @Override
    public void onMarkerDragStart(Marker marker) {
        LatLng position0 = marker.getPosition();

        Log.d(getClass().getSimpleName(), String.format("Drag from %f:%f",
                position0.latitude,
                position0.longitude));
    }

    @Override
    public void onMarkerDrag(Marker marker) {
        LatLng position0 = marker.getPosition();
        Log.d(getClass().getSimpleName(),
                String.format("Dragging to %f:%f", position0.latitude,
                        position0.longitude));
    }

    @Override
    public void onMarkerDragEnd(Marker marker) {
        LatLng position = marker.getPosition();
        latitude = position.latitude;
        longitude = position.latitude;
        Log.d(getClass().getSimpleName(), String.format("Dragged to %f:%f",
                position.latitude,
                position.longitude));
    }


    private void hideSoftKeyboard() {
        this.getWindow().setSoftInputMode(WindowManager.LayoutParams.
                SOFT_INPUT_STATE_ALWAYS_HIDDEN);
    }



    /*
            ----------------------------------------- google places API Autocomplete suggestions -------------------------------------------------
     */

    private AdapterView.OnItemClickListener mAutocompleteClickListener = new AdapterView.OnItemClickListener() {
        @Override
        public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
            hideSoftKeyboard();
            final AutocompletePrediction item = mPlaceAutocompleteAdapter.getItem(i);
            final String placeId = item.getPlaceId();
            PendingResult<PlaceBuffer> placeResult = Places.GeoDataApi
                    .getPlaceById(mGoogleApiClient, placeId);
            placeResult.setResultCallback(mUpdatePlaceDetailsCallback);
        }
    };

    private ResultCallback<PlaceBuffer> mUpdatePlaceDetailsCallback = new ResultCallback<PlaceBuffer>() {
        @Override
        public void onResult(@NonNull PlaceBuffer places) {
            if (!places.getStatus().isSuccess()) {
                Log.d(TAG, "onResult: Place query did not complete succeesfully" + places.getStatus().toString());
                places.release();
                return;
            }
            final Place place = places.get(0);
            try {
                mPlace = new PlaceInfo();
                mPlace.setName(place.getName().toString());
                mPlace.setAddress(place.getAddress().toString());
                mPlace.setAttributions(place.getAttributions().toString());
                mPlace.setId(place.getId());
                mPlace.setLatlng(place.getLatLng());
                mPlace.setRatings(place.getRating());
                mPlace.setPhoneNumber(place.getPhoneNumber().toString());
                mPlace.setWebsiteUri(place.getWebsiteUri());
                Log.d(TAG, "onResult: place : " + mPlace.toString());

            } catch (NullPointerException e) {
                Log.d(TAG, "onResult: NullPointerException" + e.getMessage());
            }

            latitude = place.getViewport().getCenter().latitude;
            longitude = place.getViewport().getCenter().longitude;


            moveCameraToSearchLocation(new LatLng(place.getViewport().getCenter().latitude,
                    place.getViewport().getCenter().longitude), DEFAULT_ZOOM);
            places.release();
        }
    };
}