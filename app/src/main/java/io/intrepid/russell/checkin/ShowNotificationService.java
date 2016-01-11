package io.intrepid.russell.checkin;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.support.v7.app.NotificationCompat;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 */
public class ShowNotificationService extends IntentService {
    private static final String TAG = ShowNotificationService.class.getSimpleName();

    private static final String ACTION_MESSAGE = "io.intrepid.russell.checkin.action.MESSAGE";

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

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (ACTION_MESSAGE.equals(action)) {
                final String text = intent.getStringExtra(EXTRA_TEXT);
                showNotification(text);
            }
        }
    }

    private void showNotification(String text) {
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

}
