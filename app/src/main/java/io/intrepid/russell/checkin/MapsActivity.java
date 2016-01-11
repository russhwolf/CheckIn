package io.intrepid.russell.checkin;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.widget.Button;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnClick;
import retrofit2.Callback;
import retrofit2.Response;
import timber.log.Timber;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback, GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private static final int REQUEST_LOCATION = 1;

    private GoogleMap map;

    private GoogleApiClient googleApiClient;

    private Marker currentLocation;

    @Bind(R.id.button)
    Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ButterKnife.bind(this);

        startService(new Intent(this, CheckInService.class));

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
        logLocation();
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

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
    public void onMapReady(GoogleMap googleMap) {
        map = googleMap;

        // Add a marker in Sydney and move the camera
        LatLng thirdSt = new LatLng(42.367023, -71.080052);
        map.addMarker(new MarkerOptions().position(thirdSt).title("Third St."));
        LatLng rogersSt = new LatLng(42.366399, -71.077689);
        map.addMarker(new MarkerOptions().position(rogersSt).title("Rogers St."));
        map.moveCamera(CameraUpdateFactory.newLatLng(thirdSt));
        map.moveCamera(CameraUpdateFactory.zoomTo(16));
    }

    @Nullable
    private Location getLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_LOCATION);
            return null;
        }
        return LocationServices.FusedLocationApi.getLastLocation(googleApiClient);
    }

    private void logLocation() {
        Location location = getLocation();
        if (location != null) {
            Timber.i("Location (%1.6f, %1.6f)", location.getLongitude(), location.getLatitude());
        } else {
            Timber.i("Null location");
        }
    }

    @OnClick(R.id.button)
    public void clickButton() {
        logLocation();
        Location location = getLocation();
        if (location != null) {
            double lng = location.getLongitude();
            double lat = location.getLatitude();
            LatLng latLng = new LatLng(lat, lng);
            if (map != null) {
                if (currentLocation != null) {
                    currentLocation.remove();
                }
                currentLocation = map.addMarker(new MarkerOptions().position(latLng).title("Current location"));
            }
        }
    }

    private static void postMessageToSlack(String message) {
        CheckInApplication.getApi().postCheckIn(new SlackApi.TextRequest(message)).enqueue(new Callback<Void>() {
            @Override
            public void onResponse(Response<Void> response) {
                Timber.d("Slack post returned %d %s", response.code(), response.message());
            }

            @Override
            public void onFailure(Throwable t) {

            }
        });
    }
}
