package com.example.android.wesport;

import android.Manifest.permission;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.places.AutocompleteFilter;
import com.google.android.gms.location.places.Place;
import com.google.android.gms.location.places.ui.PlaceAutocompleteFragment;
import com.google.android.gms.location.places.ui.PlaceSelectionListener;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.GoogleMap.OnMapLongClickListener;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

@SuppressWarnings("ALL")
public class MapsActivity extends AppCompatActivity implements OnMapReadyCallback, OnMapLongClickListener, PlaceSelectionListener {

    private static final String LOG_TAG = "GooglePlaces ";
    private final String TAG = "MapActivity";
    FragmentManager fm;
    //Variables to store games and locations from marker click
    String games = "";
    private GoogleMap map;
    private SupportMapFragment mainFragment;
    private MarkerOptions userMarker;
    private String address = "";
    private String selectedGame;
    private String addressResult;
    private View mLayout;


    /*Autocomplete Widget*/
    private TextView mPlaceDetailsText;
    private TextView mPlaceAttribution;
    private double lat, lon;
    private ProgressBar mProgressBar;



    public MapsActivity() {
        // Required empty public constructor
    }

    public static MapsActivity newInstance() {
        MapsActivity fragment = new MapsActivity();
        return fragment;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Restoring the markers on configuration changes
        setContentView(R.layout.activity_maps);
        setUpMapIfNeeded();
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mLayout = findViewById(android.R.id.content);

        // Retrieve the PlaceAutocompleteFragment.
        PlaceAutocompleteFragment autocompleteFragment = (PlaceAutocompleteFragment)
                getFragmentManager().findFragmentById(R.id.autocomplete_fragment);
        autocompleteFragment.setHint(getString(R.string.autocomplete_hint));

        // Register a listener to receive callbacks when a place has been selected or an error has
        // occurred and set Filter to retreive only places with precise address
        autocompleteFragment.setOnPlaceSelectedListener(this);
        AutocompleteFilter typeFilter = new AutocompleteFilter.Builder()
                .setTypeFilter(AutocompleteFilter.TYPE_FILTER_ADDRESS)
                .build();
        autocompleteFragment.setFilter(typeFilter);

        if(!EventBus.getDefault().hasSubscriberForEvent(GetAddressTask.class)) {
            EventBus.getDefault().register(this);
        }

        // Initialize references to views
        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        // Initialize progress bar
        mProgressBar.setVisibility(View.GONE);

    }

    public void setUserMarker(LatLng latLng) {
        if (userMarker == null) {
            userMarker = new MarkerOptions().position(latLng).title(getString(R.string.usermarker_title));
            userMarker.icon(BitmapDescriptorFactory.fromResource(R.drawable.user_marker));
            map.addMarker(userMarker);
        }
        map.animateCamera(CameraUpdateFactory.newLatLngZoom(latLng, 12.9f));

    }

    private void setUpMapIfNeeded() {
        // Do a null check to confirm that we have not already instantiated the
        // map.
        if (map == null) {
            // Try to obtain the map from the SupportMapFragment.
            // Obtain the SupportMapFragment and get notified when the map is ready to be used.
            mainFragment = (SupportMapFragment) getSupportFragmentManager()
                    .findFragmentById(R.id.map);
            mainFragment.getMapAsync(this);
            // Check if we were successful in obtaining the map.
            if (map != null) {
                setUpMap();
            }
        }
    }

    private void setUpMap() {
        map.getUiSettings().setZoomControlsEnabled(false);
        if (ActivityCompat.checkSelfPermission(this, permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            return;
        }
        map.setMyLocationEnabled(true);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap sMap) {
        map = sMap;
        map.setOnMapLongClickListener(this);
        mainFragment.setRetainInstance(true);
        //Instiantiate background task to download places list and address list for respective locations
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        Double mLat = Double.parseDouble(preferences.getString("latitude", ""));
        Double mLon = Double.parseDouble(preferences.getString("longtitude", ""));
        new DownloadTask(this, map).execute();
        SharedPreferences chGame = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        selectedGame = chGame.getString("chosenGame", "Other");
        setUserMarker(new LatLng(mLat, mLon));
    }

    @Subscribe(threadMode = ThreadMode.BACKGROUND)    // This method will be called when a GetAddressTask is posted
    public void onEvent(String address){
        // your implementation
        // De-serialize the JSON string into an array of address objects
        if (address.equals("")) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm yyyyMMdd", Locale.getDefault());
            address = sdf.format(new Date());
        }
        Snackbar.make(mLayout, selectedGame + " " + getString(R.string.save_game) + " " + address + "  " + getString(R.string.save_game_text),
                Snackbar.LENGTH_LONG).show();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        prefs.edit().putString("games", address).apply();
    }

    /* Get address for new places when user long click's on the map and show the address*/
    @Override
    public void onMapLongClick(LatLng latLng) {
        Double marLat = latLng.latitude;
        Double marLon = latLng.longitude;
        mProgressBar.setVisibility(View.VISIBLE);
        new GetAddressTask(this,marLat,marLon).execute();
        mProgressBar.setVisibility(View.GONE);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.maps_menu, menu);
        return true;
    }

    //respond to menu item selection
    public boolean onOptionsItemSelected(MenuItem item) {

        super.onOptionsItemSelected(item);
        switch (item.getItemId()) {
            case android.R.id.home:
                this.finish();
                break;
            case R.id.map_menu:
                Intent intent = new Intent(this, CatalogActivity.class);
                startActivity(intent);
                break;
        }
        return true;
    }

    /**
     * Callback invoked when a place has been selected from the PlaceAutocompleteFragment.
     */
    @Override
    public void onPlaceSelected(Place place) {
        // Either address from marker or address from autocomplete should be the location.
        String address = (String) place.getName();
        Snackbar.make(mLayout, selectedGame + " " + getString(R.string.save_game) + " " + address + "  " + getString(R.string.save_game_text),
                Snackbar.LENGTH_LONG).show();
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getBaseContext());
        prefs.edit().putString("games", address).apply();
    }

    /**
     * Callback invoked when PlaceAutocompleteFragment encounters an error.
     */
    @Override
    public void onError(Status status) {
        Snackbar.make(mLayout, getString(R.string.place_error) + status.getStatusMessage(),
                Snackbar.LENGTH_LONG).show();
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }
}