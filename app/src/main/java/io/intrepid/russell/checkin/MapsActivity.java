package io.intrepid.russell.checkin;

import android.Manifest;
import android.app.PendingIntent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.MarkerOptions;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import timber.log.Timber;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {
    private static final int REQUEST_LOCATION = 1;
    private static final int REQUEST_LOCATION_UPDATES = 2;

    private static final int LOITERING_DELAY = 5000;

    private GoogleMap map;

    private GoogleApiClient googleApiClient;

    @Bind(R.id.button)
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ButterKnife.bind(this);

//        startService(new Intent(this, CheckInService.class));

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        googleApiClient.connect();
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
//        logLocation(getLocation());
//        requestLocationUpdates();
        requestGeofencing();
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_UPDATES);
            return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(
                googleApiClient,
                new LocationRequest()
                        .setInterval(1000)
                        .setFastestInterval(1000)
                        .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY),
                this);
    }

    private void requestGeofencing() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION_UPDATES);
            return;
        }
        LocationServices.GeofencingApi.addGeofences(
                googleApiClient,
                getGeofencingRequest(),
                PendingIntent.getService(
                        this,
                        0,
                        ShowNotificationService.createGeofenceIntent(this),
                        0)
        ).setResultCallback(new ResultCallback<Status>() {
            @Override
            public void onResult(@NonNull Status status) {
                Timber.d("addGeofences(): success=" + status.isSuccess());
            }
        });
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
        logLocation(location);
    }

    /**
     * Manipulates the map once available.
     * This callback is triggered when the map is ready to be used.
     * This is where we can add markers or lines, add listeners or move the camera.
     *
     * If Google Play services is not installed on the device, the user will be prompted to install
     * it inside the SupportMapFragment. This method will only be triggered once the user has
     * installed Google Play services and returned to the app.
     */
    @Override
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        // Add a marker in Sydney and move the camera
        map.addMarker(new MarkerOptions().position(LocationData.THIRD_ST.latLng).title(LocationData.THIRD_ST.getLabel(getResources())));
        map.addMarker(new MarkerOptions().position(LocationData.ROGERS_ST.latLng).title(LocationData.ROGERS_ST.getLabel(getResources())));
        map.moveCamera(CameraUpdateFactory.newLatLng(LocationData.THIRD_ST.latLng));
        map.moveCamera(CameraUpdateFactory.zoomTo(16));
    }

    private Location getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
            return null;
        }
        return LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
    }

    private void logLocation(Location location) {
        if (location != null) {
            Timber.v("Location (%1.6f, %1.6f), precision %f", location.getLongitude(), location.getLatitude(), location.getAccuracy());
        } else {
            Timber.v("Null location");
        }
    }

    private void showMyLocation(boolean enabled) {
        if (enabled
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
            return;
        }
        map.setMyLocationEnabled(enabled);
    }

    private GeofencingRequest getGeofencingRequest() {
        return new GeofencingRequest.Builder()
                .setInitialTrigger(GeofencingRequest.INITIAL_TRIGGER_DWELL | GeofencingRequest.INITIAL_TRIGGER_ENTER)
                .addGeofence(new Geofence.Builder()
                        .setRequestId(LocationData.THIRD_ST.id)
                        .setCircularRegion(LocationData.THIRD_ST.lat, LocationData.THIRD_ST.lng, LocationData.GEOFENCE_RADIUS)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT | Geofence.GEOFENCE_TRANSITION_DWELL)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setLoiteringDelay(LOITERING_DELAY)
                        .build())
                .addGeofence(new Geofence.Builder()
                        .setRequestId(LocationData.ROGERS_ST.id)
                        .setCircularRegion(LocationData.ROGERS_ST.lat, LocationData.ROGERS_ST.lng, LocationData.GEOFENCE_RADIUS)
                        .setTransitionTypes(Geofence.GEOFENCE_TRANSITION_ENTER | Geofence.GEOFENCE_TRANSITION_EXIT | Geofence.GEOFENCE_TRANSITION_DWELL)
                        .setExpirationDuration(Geofence.NEVER_EXPIRE)
                        .setLoiteringDelay(LOITERING_DELAY)
                        .build())
                .build();
    }

    @OnClick(R.id.button)
    public void clickButton() {
        Location location = getLocation();
        logLocation(location);
        if (map != null) {
            showMyLocation(!map.isMyLocationEnabled());
        }
    }
}
