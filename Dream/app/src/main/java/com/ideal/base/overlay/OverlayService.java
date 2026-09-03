package com.ideal.base.overlay;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;
import android.provider.Settings;

import androidx.annotation.Nullable;

import com.ideal.base.MainActivity;
import com.ideal.base.R;

/** Foreground owner of the entry, panel, and individually isolated shortcut windows. */
public final class OverlayService extends Service {

    public static final String ACTION_START = "com.ideal.base.action.START_OVERLAY";
    public static final String ACTION_STOP = "com.ideal.base.action.STOP_OVERLAY";
    private static final String CHANNEL_ID = "ideal_overlay";
    private static final int NOTIFICATION_ID = 1301;

    private OverlayWindowController windowController;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        Notification notification = buildNotification();
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIFICATION_ID, notification,
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);
        } else {
            startForeground(NOTIFICATION_ID, notification);
        }
        windowController = new OverlayWindowController(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (!Settings.canDrawOverlays(this)) {
            stopSelf();
            return START_NOT_STICKY;
        }
        windowController.show();
        return START_STICKY;
    }

    @Override
    public void onConfigurationChanged(android.content.res.Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        if (windowController != null) {
            windowController.onConfigurationChanged();
        }
    }

    @Override
    public void onDestroy() {
        if (windowController != null) {
            windowController.destroy();
            windowController = null;
        }
        stopForeground(STOP_FOREGROUND_REMOVE);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(CHANNEL_ID, "ideal overlay",
                    NotificationManager.IMPORTANCE_LOW);
            channel.setDescription("Keeps the user-controlled ideal overlay available.");
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
    }

    private Notification buildNotification() {
        Intent launchIntent = new Intent(this, MainActivity.class);
        int pendingFlags = PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE;
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, launchIntent, pendingFlags);
        return new Notification.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle("ideal overlay is active")
                .setContentText("Tap to manage or stop the overlay.")
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }
}
