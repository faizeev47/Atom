package com.example.atom.Headset;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.atom.BuildConfig;
import com.example.atom.ConnectionActivity;
import com.example.atom.Utilities.NotificationUtils;

import org.jetbrains.annotations.NotNull;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class SocketService extends Service {
    public static boolean SERVICE_RUNNING = false;

    private static final int TASK_CONNECT = 0;
    private static final int TASK_DISCONNECT = 1;

    public static final String CONNECTED = "connected";
    public static final String DISCONNECTED = "disconnected";

    public static final String EXTRA_STATUS = BuildConfig.APPLICATION_ID + "EXTRA_STATE";
    public static final String EXTRA_STATE = BuildConfig.APPLICATION_ID + "EXTRA_STATE";
    public static final String EXTRA_ATTENTION_VALUE = BuildConfig.APPLICATION_ID + "EXTRA_ATTENTION_VALUE";

    public static final String ACTION_GET_CONNECTION_STATUS = BuildConfig.APPLICATION_ID + "GET_CONNECTION_STATUS";
    public static final String ACTION_RETURN_CONNECTION_STATUS = BuildConfig.APPLICATION_ID + "ACTION_RETURN_CONNECTION_STATUS";

    public static final String ACTION_RETURN_ATTENTION_VALUE = BuildConfig.APPLICATION_ID + "ACTION_RETURN_ATTENTION_VALUE";

    public static final String ACTION_CONNECTION_INITIATED = BuildConfig.APPLICATION_ID + "CONNECTION_INITIATED";
    public static final String ACTION_RESPOND_CONNECTED = BuildConfig.APPLICATION_ID + "CONNECTED";
    public static final String ACTION_RESPOND_DISCONNECTED = BuildConfig.APPLICATION_ID + "DISCONNECTED";
    public static final String ACTION_UPDATE_CONNECTION_STATE = BuildConfig.APPLICATION_ID + "ACTION_UPDATE_CONNECTION_STATE";
    public static final String ACTION_FAILED = BuildConfig.APPLICATION_ID + "ACTION_FAILED";

    private static final String LOG_TAG_SOCKET = "Web Socket Event";
    private static final String LOG_TAG = "Logging";

    // Threshold of the gradient
    private static final int GRADIENT_THRESHOLD = 10;

    // Change to match to the IP of the server
    private static final String SERVER_URL = "ws://192.168.100.9:8001";

    private WebSocket mWebSocket;
    private NotificationManager mNotifyManager;
    private Looper serviceLooper;
    private SocketHandler socketHandler;
    private LocalBroadcastManager mLocalBroadcastManager;


    private enum ConnectionState {
        DISCONNECTED_NOT_INFORMED,
        DISCONNECTED_INFORMED,
        CONNECTED_NOT_INFORMED,
        CONNECTED_INFORMED
    }
    private ConnectionState mConnectionState;
    private int mUserAttention;

    RequestReceiver mRequestReceiver = new RequestReceiver();

    // BroadcastReceiver handling connection status requests
    public class RequestReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            if (intentAction != null &&
                    intentAction.equals(ACTION_GET_CONNECTION_STATUS)) {
                Intent replyIntent = new Intent(ACTION_RETURN_CONNECTION_STATUS);
                replyIntent.putExtra(EXTRA_STATUS,
                        mConnectionState == ConnectionState.CONNECTED_INFORMED ||
                        mConnectionState == ConnectionState.CONNECTED_NOT_INFORMED
                        ? CONNECTED
                        : DISCONNECTED);
                mLocalBroadcastManager.sendBroadcast(replyIntent);
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(LOG_TAG, "Service created!");
        SERVICE_RUNNING = true;
        HandlerThread thread = new HandlerThread("ServiceStartArguments",
                Process.THREAD_PRIORITY_BACKGROUND);
        thread.start();

        serviceLooper = thread.getLooper();
        socketHandler = new SocketHandler(serviceLooper);

        mConnectionState = ConnectionState.DISCONNECTED_INFORMED;
        mUserAttention = 0;
        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mLocalBroadcastManager.sendBroadcast(new Intent(ACTION_CONNECTION_INITIATED));
        mLocalBroadcastManager.registerReceiver(mRequestReceiver, new IntentFilter(ACTION_GET_CONNECTION_STATUS));

        mNotifyManager = NotificationUtils.createNotificationChannel(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Message msg = socketHandler.obtainMessage();
        msg.what = TASK_CONNECT;
        socketHandler.sendMessage(msg);
        Log.d(LOG_TAG, "Service issued start command!");
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.d(LOG_TAG, "Service destroyed!");
        switch (mConnectionState) {
            case CONNECTED_INFORMED:
            case CONNECTED_NOT_INFORMED:
                mConnectionState = ConnectionState.DISCONNECTED_INFORMED;
                broadcastDisconnection();
                break;
        }
        Message msg = socketHandler.obtainMessage();
        msg.what = TASK_DISCONNECT;
        socketHandler.sendMessage(msg);
        mLocalBroadcastManager.unregisterReceiver(mRequestReceiver);
        SERVICE_RUNNING = false;
        super.onDestroy();
    }

    private void broadcastDisconnection() {
        Intent replyIntent = new Intent(ACTION_RETURN_CONNECTION_STATUS);
        replyIntent.putExtra(EXTRA_STATUS, DISCONNECTED);
        mLocalBroadcastManager.sendBroadcast(replyIntent);
        mLocalBroadcastManager.sendBroadcast(new Intent(ACTION_RESPOND_DISCONNECTED));
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

            return client.newWebSocket(request, new SocketListener() );
        }

    }

    private class SocketListener extends WebSocketListener {
        @Override
        public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            Log.d(LOG_TAG_SOCKET, "closed!");
            switch (mConnectionState) {
                case CONNECTED_INFORMED:
                case CONNECTED_NOT_INFORMED:
                    mConnectionState = ConnectionState.DISCONNECTED_INFORMED;
                    broadcastDisconnection();
                    break;
            }
        }

        @Override
        public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
            Log.d(LOG_TAG_SOCKET, "closing..");
            switch (mConnectionState) {
                case CONNECTED_INFORMED:
                case CONNECTED_NOT_INFORMED:
                    mConnectionState = ConnectionState.DISCONNECTED_INFORMED;
                    broadcastDisconnection();
                    break;
            }
        }

        @Override
        public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @Nullable Response response) {
            Log.e(LOG_TAG_SOCKET, "failed: " + t.getMessage() + "\n" + t.getCause());
            t.printStackTrace();
            switch (mConnectionState) {
                case CONNECTED_INFORMED:
                case CONNECTED_NOT_INFORMED:
                    broadcastDisconnection();
                    break;
                default:
                    mLocalBroadcastManager.sendBroadcast(new Intent(ACTION_FAILED));
                    break;
            }
            mConnectionState = ConnectionState.DISCONNECTED_INFORMED;
        }

        @Override
        public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
            if (!text.isEmpty()) {
                try {
                    JSONObject headsetResponse = new JSONObject(text);
                    if (headsetResponse.has("connected")) {
                        switch (mConnectionState) {
                            case DISCONNECTED_INFORMED:
                            case DISCONNECTED_NOT_INFORMED:
                                mConnectionState = ConnectionState.CONNECTED_NOT_INFORMED;
                            case CONNECTED_NOT_INFORMED:
                                Notification notification = NotificationUtils.getNotificationBuilder(
                                        SocketService.this,
                                        ConnectionActivity.class,
                                        "Headset connected",
                                        "You've successfully connected with the headset!",
                                        NotificationUtils.CONNECTED_NOTIFICATION).build();
                                startForeground(NotificationUtils.CONNECTED_NOTIFICATION, notification);
                                mLocalBroadcastManager.sendBroadcast(new Intent(ACTION_RESPOND_CONNECTED));
                                mConnectionState = ConnectionState.CONNECTED_INFORMED;
                                break;
                        }
                        mUserAttention = Integer.parseInt(headsetResponse.getString("attention"));
                        Intent readingIntent = new Intent(ACTION_RETURN_ATTENTION_VALUE);
                        readingIntent.putExtra(EXTRA_ATTENTION_VALUE, mUserAttention);
                        mLocalBroadcastManager.sendBroadcast(readingIntent);
                    } else if (headsetResponse.has("disconnected")) {
                        switch (mConnectionState) {
                            case CONNECTED_INFORMED:
                            case CONNECTED_NOT_INFORMED:
                                mConnectionState = ConnectionState.DISCONNECTED_NOT_INFORMED;
                            case DISCONNECTED_NOT_INFORMED:
                                broadcastDisconnection();
                                mConnectionState = ConnectionState.DISCONNECTED_INFORMED;
                                break;
                            default:
                                Intent stateIntent = new Intent(ACTION_UPDATE_CONNECTION_STATE);
                                stateIntent.putExtra(EXTRA_STATE, headsetResponse.getString("status"));
                                mLocalBroadcastManager.sendBroadcast(stateIntent);
                                break;
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                    mLocalBroadcastManager.sendBroadcast(new Intent(ACTION_FAILED));
                }

            }

        }

        @Override
        public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
            Log.d(LOG_TAG_SOCKET, "opened!");
        }
    }
}