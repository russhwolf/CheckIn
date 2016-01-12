package io.intrepid.russell.checkin;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import retrofit2.Callback;
import retrofit2.Response;
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
        showNotification(text, 0);
    }

    private void showNotification(String text, int id) {
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
        notificationManager.notify(id, notification);
    }

    private void processGeofencingEvent(GeofencingEvent event) {
        if (event.getTriggeringGeofences().size() > 0) { // TODO Is this size ever not 1? Why?
            Geofence fence = event.getTriggeringGeofences().get(0);
            switch (event.getGeofenceTransition()) {
                case Geofence.GEOFENCE_TRANSITION_ENTER:
                    showNotification("Entered " + fence.getRequestId(), R.id.notification_enter);
                    break;
                case Geofence.GEOFENCE_TRANSITION_EXIT:
                    showNotification("Exited " + fence.getRequestId(), R.id.notification_exit);
                    break;
                case Geofence.GEOFENCE_TRANSITION_DWELL:
                    showNotification("Dwelling at " + fence.getRequestId(), R.id.notification_dwell);
                    break;
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
