package com.example.atom.Utilities;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.os.Build;
import android.service.notification.StatusBarNotification;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.NotificationCompat;

import com.example.atom.ConnectionActivity;
import com.example.atom.R;

import static android.content.Context.NOTIFICATION_SERVICE;

public class NotificationUtils {
    public static final String PRIMARY_CHANNEL_ID = "primary_notification_channel";
    public static final int CONNECTED_NOTIFICATION = 1;
    public static final int NOT_CONNECTED_NOTIFICATION = 2;
    public static final int NOT_READING_NOTIFICATION = 3;

    public static NotificationManager createNotificationChannel(@NonNull Context context) {
        NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(PRIMARY_CHANNEL_ID,
                    "Headset notification",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.BLUE);
            notificationChannel.enableVibration(true);
            notificationChannel.setDescription("Notification from Socket Service");
            notificationManager.createNotificationChannel(notificationChannel);
        }
        return notificationManager;
    }

    public static NotificationCompat.Builder getNotificationBuilder(@NonNull Context context,
                                                                @NonNull Class intentClass,
                                                                @NonNull String title,
                                                                @NonNull String message,
                                                                @NonNull int notificationId) {
        Intent notificationIntent = new Intent(context, intentClass);
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(context,
                notificationId, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Builder(context, PRIMARY_CHANNEL_ID)
                .setContentTitle(title)
                .setSmallIcon(R.drawable.img_splash)
                .setContentIntent(notificationPendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText(message))
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setPriority(NotificationCompat.DEFAULT_ALL);
    }

    public static void notifyNotConnected(@NonNull Context context, @NonNull  NotificationManager notificationManager) {
        Notification notification = NotificationUtils.getNotificationBuilder(
                context,
                ConnectionActivity.class,
                "Headset is not connected!",
                "Please connect the headset for a monitored reading session!",
                NotificationUtils.NOT_CONNECTED_NOTIFICATION).build();
        notificationManager.notify(NotificationUtils.NOT_CONNECTED_NOTIFICATION, notification);
    }

    public static void notifyNotReading(@NonNull Context context, @NonNull  NotificationManager notificationManager) {
        for (StatusBarNotification notif : notificationManager.getActiveNotifications()) {
            if (notif.getId() == NOT_READING_NOTIFICATION) {
                return;
            }
        }
        Notification notification = NotificationUtils.getNotificationBuilder(
                context,
                ConnectionActivity.class,
                "You're not reading!",
                "Oops, your attention has dropped which means you're not reading the book!",
                NotificationUtils.NOT_READING_NOTIFICATION)
                .setColor(Color.RED).build();
        notificationManager.notify(NotificationUtils.NOT_READING_NOTIFICATION, notification);
    }
}
