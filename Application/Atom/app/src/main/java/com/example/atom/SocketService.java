package com.example.atom;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class SocketService extends Service {
    private static final String PRIMARY_CHANNEL_ID = "primary_notification_channel";
    private static final int NOTIFICATION_ID = 0;

    private static final int TASK_CONNECT = 0;
    private static final int TASK_DISCONNECT = 1;

    private static final String SERVER_URL = "ws://10.128.42.89:8000";

    public static final String EXTRA_MESSAGE = SocketService.class.getCanonicalName() + "EXTRA_MESSAGE";

    public static final String ACTION_SEND_MESSAGE = BuildConfig.APPLICATION_ID + "SEND_MESSAGE";
    public static final String ACTION_CONNECTED = BuildConfig.APPLICATION_ID + "CONNECTED";
    public static final String ACTION_DISCONNECTED = BuildConfig.APPLICATION_ID + "DISCONNECTED";
    public static final String ACTION_GET_CONNECTION_STATUS = BuildConfig.APPLICATION_ID + "GET_CONNECTION_STATUS";
    public static final String ACTION_FAILED = BuildConfig.APPLICATION_ID + "ACTION_FAILED";
    public static final String ACTION_CONNECTION_INITIATED = BuildConfig.APPLICATION_ID + "CONNECTION_INITIATED";
    public static final String ACTION_USER_READING = BuildConfig.APPLICATION_ID + "SEND_USER_READING";
    public static final String ACTION_USER_NOT_READING = BuildConfig.APPLICATION_ID + "SEND_USER_NOT_READING";


    private static final String LOG_TAG_SOCKET = "Web Socket Event";
    private static final String LOG_TAG = "Logging";

    private NotificationManager mNotifyManager;

    private Looper serviceLooper;
    private SocketHandler socketHandler;

    private WebSocket mWebSocket;
    private boolean mSocketConnected;
    private float mAccX;
    private float mAccY;
    private float mAccZ;

    private LocalBroadcastManager mLocalBroadcastManager;

    RequestReceiver mRequestReceiver = new RequestReceiver();




    public class RequestReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            if (intentAction != null && intentAction.equals(ACTION_GET_CONNECTION_STATUS)) {
                mLocalBroadcastManager.sendBroadcast(new Intent(mSocketConnected ? ACTION_CONNECTED : ACTION_DISCONNECTED));
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        mAccX = 0;
        mAccY = 0;
        mAccZ = 0;

        serviceLooper = thread.getLooper();
        socketHandler = new SocketHandler(serviceLooper);

        mSocketConnected = false;
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mLocalBroadcastManager.sendBroadcast(new Intent(ACTION_CONNECTION_INITIATED));
        mLocalBroadcastManager.registerReceiver(mRequestReceiver, new IntentFilter(ACTION_GET_CONNECTION_STATUS));
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg = socketHandler.obtainMessage();
        msg.what = TASK_CONNECT;
        socketHandler.sendMessage(msg);

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        mLocalBroadcastManager.sendBroadcast(new Intent(ACTION_DISCONNECTED));
        Message msg = socketHandler.obtainMessage();
        msg.what = TASK_DISCONNECT;
        socketHandler.sendMessage(msg);
        mLocalBroadcastManager.unregisterReceiver(mRequestReceiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private final class SocketHandler extends Handler {


        public SocketHandler(Looper looper) {
            super(looper);
        }

        @Override
        public void handleMessage(@NonNull Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case TASK_CONNECT:
                    mWebSocket = getConnectedSocket();
                    break;
                case TASK_DISCONNECT:
                    mWebSocket.close(1000, "Closed connection!");
                    break;
                default:
                    break;
            }
        }

        protected WebSocket getConnectedSocket() {
            OkHttpClient client = new OkHttpClient.Builder()
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(SERVER_URL)
                    .build();

            return client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                    Log.d(LOG_TAG_SOCKET, "closed!");
                    mSocketConnected = false;
                    mLocalBroadcastManager.sendBroadcast(new Intent(ACTION_DISCONNECTED));
                }

                @Override
                public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                    Log.d(LOG_TAG_SOCKET, "closing..");
                    mSocketConnected = false;
                    mLocalBroadcastManager.sendBroadcast(new Intent(ACTION_DISCONNECTED));
                }

                @Override
                public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @org.jetbrains.annotations.Nullable Response response) {
                    Log.e(LOG_TAG_SOCKET, "failed: " + t.getMessage() + "\n" + t.getCause());
                    t.printStackTrace();
                    mSocketConnected = false;
                    mLocalBroadcastManager.sendBroadcast(new Intent(ACTION_FAILED));
                    SocketService.this.stopSelf();
                }

                @Override
                public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                    mSocketConnected = true;
                    if (!text.isEmpty()) {
                        Intent sendMessageIntent = new Intent(ACTION_SEND_MESSAGE);
                        sendMessageIntent.putExtra(EXTRA_MESSAGE, text);
                        mLocalBroadcastManager.sendBroadcast(sendMessageIntent);
                        if (text.contains("met")) {
                            Log.d(LOG_TAG_SOCKET, "metrics");
                        }
                        if (text.contains("mot")) {
                            try {
                                JSONObject motionObject = new JSONObject(text);
                                JSONArray motionArray = motionObject.getJSONArray("mot");
                                float newAccX = Math.abs(Float.parseFloat(motionArray.getString(6)));
                                float newAccY = Math.abs(Float.parseFloat(motionArray.getString(7)));
                                float newAccZ = Math.abs(Float.parseFloat(motionArray.getString(8)));
                                if (mAccX != 0 && mAccY != 0 && mAccZ != 0) {
                                    Intent intent = new Intent();
                                    if (Math.abs(newAccX - mAccX) > 0.08 ||
                                            Math.abs(newAccZ - mAccZ) > 0.08 ||
                                            Math.abs(newAccY - mAccY) > 0.08) {
                                        intent.setAction(ACTION_USER_NOT_READING);
                                    } else {
                                        intent.setAction(ACTION_USER_READING);
                                    }
                                    mLocalBroadcastManager.sendBroadcast(intent);
                                }

                                mAccX = newAccX;
                                mAccZ = newAccZ;
                                mAccY = newAccY;

                            } catch (JSONException e) {
                                e.printStackTrace();
                            }

                        }
                    }

                }

                @Override
                public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                    Log.d(LOG_TAG_SOCKET, "opened!");
                    createNotificationChannel();
                    NotificationCompat.Builder notifyBuilder = getNotificationBuilder();
                    Notification notification = notifyBuilder.build();
                    mSocketConnected = true;
                    startForeground(1, notification);
                    mLocalBroadcastManager.sendBroadcast(new Intent(ACTION_CONNECTED));
                }
            });
        }

    }

    private void createNotificationChannel() {
        mNotifyManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel notificationChannel = new NotificationChannel(PRIMARY_CHANNEL_ID,
                    "Socket notification",
                    NotificationManager.IMPORTANCE_HIGH);
            notificationChannel.enableLights(true);
            notificationChannel.setLightColor(Color.BLUE);
            notificationChannel.enableVibration(true);
            notificationChannel.setDescription("Notification from Socket Service");
            mNotifyManager.createNotificationChannel(notificationChannel);
        }
    }

    private NotificationCompat.Builder getNotificationBuilder() {
        Intent notificationIntent = new Intent(this, ConnectionActivity.class);
        PendingIntent notificationPendingIntent = PendingIntent.getActivity(this,
                NOTIFICATION_ID, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        return new NotificationCompat.Builder(this, PRIMARY_CHANNEL_ID)
                .setContentTitle("Headset connection active")
                .setContentText("You've opened a socket to headset!")
                .setSmallIcon(R.drawable.splash_image)
                .setContentIntent(notificationPendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_LOW)
                .setPriority(NotificationCompat.DEFAULT_ALL);
    }
}