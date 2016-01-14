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

import retrofit2.Callback;
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
                    String text = intent.getStringExtra(EXTRA_TEXT);
                    showNotification(text);
                    break;
                case ACTION_GEOFENCE:
                    GeofencingEvent event = GeofencingEvent.fromIntent(intent);
                    processGeofencingEvent(event);
                    break;
                case ACTION_SLACK:
                    postMessageToSlack(intent.getStringExtra(EXTRA_TEXT));
                    notificationManager.cancel(R.id.notification_slack);
                    break;
            }
        }
    }

    private void showNotification(String text) {
        showNotification(text, 0, null);
    }

    private void showNotification(String text, int id, NotificationCompat.Action action) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setTicker(text)
                .setWhen(System.currentTimeMillis())
                .setContentTitle(getText(R.string.app_name))
                .setContentText(text);
        if (action != null) {
            builder.addAction(action);
        }
        Notification notification = builder.build();

        notificationManager.notify(id, notification);
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
        Action action = new Action(R.mipmap.ic_launcher, getString(R.string.slack_action),
                PendingIntent.getService(
                        this,
                        0,
                        createSlackIntent(this, slackMessage),
                        0));
        showNotification(notificationMessage, R.id.notification_slack, action);
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
