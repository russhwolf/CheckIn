package io.intrepid.russell.checkin;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.content.LocalBroadcastManager;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.MarkerOptions;

import butterknife.Bind;
import butterknife.ButterKnife;
import butterknife.OnCheckedChanged;
import timber.log.Timber;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    public static final String ACTION_REQUEST_PERMISSION = "request_permission";
    public static final String EXTRA_PERMISSION_NAMES = "permission_names";
    public static final String EXTRA_PERMISSION_REQUEST_CODE = "permission_request_code";

    public static final int REQUEST_LOCATION = 1;
    public static final int REQUEST_LOCATION_UPDATES = 2;
    public static final int REQUEST_GEOFENCING = 3;

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case CheckInService.ACTION_PING_RESPONSE:
                    Timber.d("Ping response received");
                    toggle.setChecked(true);
                    break;
                case ACTION_REQUEST_PERMISSION:
                    String[] names = intent.getStringArrayExtra(EXTRA_PERMISSION_NAMES);
                    int requestCode = intent.getIntExtra(EXTRA_PERMISSION_REQUEST_CODE, -1);
                    ActivityCompat.requestPermissions(MapsActivity.this, names, requestCode);
                    break;
            }
        }
    };

    private GoogleMap map;

    @Bind(R.id.toggle)
    Switch toggle;

    @Bind(R.id.text)
    TextView text;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_maps);
        ButterKnife.bind(this);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(CheckInService.ACTION_PING_RESPONSE);
        filter.addAction(ACTION_REQUEST_PERMISSION);
        LocalBroadcastManager broadcastManager = LocalBroadcastManager.getInstance(this);
        broadcastManager.registerReceiver(receiver, filter);
        Timber.d("Sending ping request to service");
        CheckInService.sendPingBroadcast(this);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
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
        map.addMarker(new MarkerOptions().position(LocationData.THIRD_ST.latLng).title(LocationData.THIRD_ST.getLabel(getResources())));
        map.addMarker(new MarkerOptions().position(LocationData.ROGERS_ST.latLng).title(LocationData.ROGERS_ST.getLabel(getResources())));
        map.moveCamera(CameraUpdateFactory.newLatLng(LocationData.THIRD_ST.latLng));
        map.moveCamera(CameraUpdateFactory.zoomTo(16));
        showMyLocation(true);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode) {
            case REQUEST_LOCATION:
                if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    showMyLocation(true);
                } else {
                    showMyLocation(false);
                }
                break;
            case REQUEST_LOCATION_UPDATES:
            case REQUEST_GEOFENCING:
                if (grantResults.length == 2 && grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED) {
                    LocalBroadcastManager.getInstance(this).sendBroadcast(
                            new Intent(CheckInService.ACTION_PERMISSION_RESULT)
                                    .putExtra(CheckInService.EXTRA_PERMISSION_REQUEST_CODE, requestCode));
                } else {
                    toggle.setChecked(false);
                }
                break;

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

    @OnCheckedChanged(R.id.toggle)
    void toggleService(boolean checked) {
        if (checked) {
            text.setText(R.string.service_toggle_on);
            startService(new Intent(this, CheckInService.class));
        } else {
            text.setText(R.string.service_toggle_off);
            stopService(new Intent(this, CheckInService.class));
        }
    }
}
