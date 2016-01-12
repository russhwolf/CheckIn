package io.intrepid.russell.checkin;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

import timber.log.Timber;

public class CheckInService extends Service implements GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener, LocationListener {

    private final BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (ACTION_PING_REQUEST.equals(intent.getAction())) {
                Timber.d("Ping request received");
                LocalBroadcastManager.getInstance(CheckInService.this).sendBroadcast(new Intent(ACTION_PING_RESPONSE));
            }
        }
    };

    public static final String ACTION_PING_REQUEST = "checkin_service_ping_request";
    public static final String ACTION_PING_RESPONSE = "checkin_service_ping_response";

    private static final int LOITERING_DELAY = 5000;

    private GoogleApiClient googleApiClient;

    private final int NOTIFICATION_ID = R.id.notification_service;
    private NotificationManager notificationManager;

    @Override
    public void onCreate() {
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, new IntentFilter(ACTION_PING_REQUEST));

        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        showNotification();

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        googleApiClient.connect();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Timber.i("Received start id " + startId + ": " + intent);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);

        notificationManager.cancelAll();

        if (googleApiClient.isConnected()) {
            googleApiClient.disconnect();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return new Binder();
    }

    @Override
    public void onConnected(Bundle bundle) {
//        requestLocationUpdates();
        requestGeofencing();
    }

    private void requestLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
//                    Manifest.permission.ACCESS_COARSE_LOCATION}, Utils.REQUEST_LOCATION_UPDATES);
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
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
//                    Manifest.permission.ACCESS_COARSE_LOCATION}, Utils.REQUEST_LOCATION_UPDATES);
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

    private static void logLocation(Location location) {
        if (location != null) {
            Timber.v("Location (%1.6f, %1.6f), precision %f", location.getLongitude(), location.getLatitude(), location.getAccuracy());
        } else {
            Timber.v("Null location");
        }
    }

    /**
     * Show a notification while this service is running.
     */
    private void showNotification() {
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getString(R.string.service_running);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MapsActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.app_name))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .addAction(R.mipmap.ic_launcher, "Action", contentIntent)
                .build();
        notification.flags |= NotificationCompat.FLAG_NO_CLEAR;

        // Send the notification.
        notificationManager.notify(NOTIFICATION_ID, notification);
    }
}