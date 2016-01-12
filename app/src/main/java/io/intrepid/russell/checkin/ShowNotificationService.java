package io.intrepid.russell.checkin;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import com.google.android.gms.location.GeofencingEvent;

import timber.log.Timber;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class ShowNotificationService extends IntentService {
    private static final String TAG = ShowNotificationService.class.getSimpleName();

    private static final String ACTION_MESSAGE = "io.intrepid.russell.checkin.action.MESSAGE";
    private static final String ACTION_GEOFENCE = "io.intrepid.russell.checkin.action.GEOFENCE";

    private static final String EXTRA_TEXT = "io.intrepid.russell.checkin.extra.TEXT";

    public ShowNotificationService() {
        super(TAG);
    }

    public static void showNotification(Context context, String text) {
        context.startService(createNotificationIntent(context, text));
    }

    public static void showGeofenceNotification(Context context, Intent intent) {

    }

    public static Intent createNotificationIntent(Context context, String text) {
        return new Intent(context, ShowNotificationService.class)
                .setAction(ACTION_MESSAGE)
                .putExtra(EXTRA_TEXT, text);
    }

    public static Intent createGeofenceIntent(Context context) {
        return new Intent(context, ShowNotificationService.class)
                .setAction(ACTION_GEOFENCE);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            switch (intent.getAction()) {
                case ACTION_MESSAGE:
                    String text = intent.getStringExtra(EXTRA_TEXT);
                    showNotification(text);
                    break;
                case ACTION_GEOFENCE:
                    GeofencingEvent event = GeofencingEvent.fromIntent(intent);
                    processGeofencingEvent(event);
                    break;
            }
        }
    }

    private void showNotification(String text) {
        Timber.d("Showing notification: %s", text);
        NotificationManager notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);

        // Set the info for the views that show in the notification panel.
        Notification notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.app_name))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .build();

        // Send the notification.
        notificationManager.notify(0, notification);
    }

    private void processGeofencingEvent(GeofencingEvent event) {
        Timber.i("Processing geofencing event");
        Timber.d("  Triggering fences (length=%d)", event.getTriggeringGeofences().size());
        for (int i = 0; i < event.getTriggeringGeofences().size(); i++) {
            Timber.d("    id=%s, fence=%s", event.getTriggeringGeofences().get(i).getRequestId(), event.getTriggeringGeofences().get(i));
        }
        Timber.d("  Transition=%d", event.getGeofenceTransition());
        Timber.d("  Error code=%d", event.getErrorCode());
        Timber.d("  Location=(%f,%f)", event.getTriggeringLocation().getLongitude(), event.getTriggeringLocation().getLatitude());
    }

}
