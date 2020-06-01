package com.example.subscribertester;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;

import android.app.Notification;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.widget.CompoundButton;
import android.widget.Switch;


import org.jetbrains.annotations.NotNull;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketAddress;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;

public class MainActivity extends AppCompatActivity {

    Switch mConnectionSwitch;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final String hostname = "ws://192.168.100.9:8001";
        Log.d("IP", hostname);

        mConnectionSwitch = findViewById(R.id.switch1);
        mConnectionSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    new SocketConnection().execute(hostname);
                }
            }
        });
    }

    public static class SocketConnection extends AsyncTask<String, Void, Void> {
        private static final String LOG_TAG_SOCKET = "SOCKET";
        public boolean isRunning;

        @Override
        protected Void doInBackground(String... strings) {
            isRunning = true;
            getConnectedSocket(strings[0]);
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            isRunning = false;
        }



        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            isRunning = false;
        }

        protected WebSocket getConnectedSocket(String serverURL) {
            OkHttpClient client = new OkHttpClient.Builder()
                    .readTimeout(0, TimeUnit.MILLISECONDS)
                    .build();

            Request request = new Request.Builder()
                    .url(serverURL)
                    .build();

            return client.newWebSocket(request, new WebSocketListener() {
                @Override
                public void onClosed(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                    Log.d(LOG_TAG_SOCKET, "closed!");
                }

                @Override
                public void onClosing(@NotNull WebSocket webSocket, int code, @NotNull String reason) {
                    Log.d(LOG_TAG_SOCKET, "closing..");
                }

                @Override
                public void onFailure(@NotNull WebSocket webSocket, @NotNull Throwable t, @org.jetbrains.annotations.Nullable Response response) {
                    Log.e(LOG_TAG_SOCKET, "failed: " + t.getMessage() + "\n" + t.getCause());
                }

                @Override
                public void onMessage(@NotNull WebSocket webSocket, @NotNull String text) {
                    Log.d(LOG_TAG_SOCKET, text);
                }

                @Override
                public void onOpen(@NotNull WebSocket webSocket, @NotNull Response response) {
                    Log.d(LOG_TAG_SOCKET, "opened!");
                }
            });
        }
    }
}
