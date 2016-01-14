package io.intrepid.russell.checkin;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Action;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingEvent;

import java.io.IOException;

import retrofit2.Response;
import timber.log.Timber;

public class ShowNotificationService extends IntentService {
    private static final String TAG = ShowNotificationService.class.getSimpleName();

    private static final String ACTION_MESSAGE = "notification_message";
    private static final String ACTION_GEOFENCE = "notification_geofence";
    private static final String ACTION_SLACK = "notification_slack";

    private static final String EXTRA_TEXT = "text";

    private NotificationManager notificationManager;

    public ShowNotificationService() {
        super(TAG);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
    }

    public static Intent createMessageIntent(Context context, String text) {
        return new Intent(context, ShowNotificationService.class)
                .setAction(ACTION_MESSAGE)
                .putExtra(EXTRA_TEXT, text);
    }

    public static Intent createGeofenceIntent(Context context) {
        return new Intent(context, ShowNotificationService.class)
                .setAction(ACTION_GEOFENCE);
    }

    public static Intent createSlackIntent(Context context, String text) {
        return new Intent(context, ShowNotificationService.class)
                .setAction(ACTION_SLACK)
                .putExtra(EXTRA_TEXT, text);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            switch (intent.getAction()) {
                case ACTION_MESSAGE:
                    showNotification(intent.getStringExtra(EXTRA_TEXT));
                    break;

                case ACTION_GEOFENCE:
                    GeofencingEvent event = GeofencingEvent.fromIntent(intent);
                    processGeofencingEvent(event);
                    break;

                case ACTION_SLACK:
                    postMessageToSlack(intent.getStringExtra(EXTRA_TEXT));
                    break;
            }
        }
    }

    private void showNotification(String text) {
        showNotification(text, null);
    }

    private void showNotification(String text, Action action) {
        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MapsActivity.class), 0);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.ic_location)
                .setTicker(text)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getText(R.string.app_name))
                .setContentText(text)
                .setContentIntent(contentIntent);

        if (action != null) {
            builder.addAction(action);
        }

        Notification notification = builder.build();
        notification.flags |= NotificationCompat.FLAG_NO_CLEAR;

        notificationManager.notify(R.id.notification, notification);
    }

    private void processGeofencingEvent(GeofencingEvent event) {
        if (event.getTriggeringGeofences().size() == 0) {
            return;
        }

        Geofence fence = event.getTriggeringGeofences().get(0);
        LocationData data = LocationData.forId(fence.getRequestId());
        if (data == null) {
            return;
        }

        String placeName = data.getLabel(getResources());
        String notificationMessage;
        String slackMessage;
        switch (event.getGeofenceTransition()) {
            case Geofence.GEOFENCE_TRANSITION_EXIT:
                notificationMessage = getString(R.string.notification_exited, placeName);
                slackMessage = getString(R.string.slack_exited, placeName);
                break;

            case Geofence.GEOFENCE_TRANSITION_DWELL:
                notificationMessage = getString(R.string.notification_dwelling, placeName);
                slackMessage = getString(R.string.slack_dwelling, placeName);
                break;

            default:
                return;
        }

        Action action = new Action(R.drawable.ic_message, getString(R.string.action_slack),
                PendingIntent.getService(
                        this,
                        0,
                        createSlackIntent(this, slackMessage),
                        0));

        showNotification(notificationMessage, action);
    }

    private static void postMessageToSlack(String message) {
        // Note that this is called from onHandleIntent(), which is already off the main UI thread,
        // so we can use execute() instead of enqueue here.
        try {
            Response<Void> response = CheckInApplication.getApi().postCheckIn(new SlackApi.TextRequest(message)).execute();
            Timber.d("Slack post returned %d %s", response.code(), response.message());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

}
