package com.example.atom;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

public class ConnectionActivity extends AppCompatActivity {

    public static final String EXTRA_CONNECTION_STATUS = ConnectionActivity.class.getCanonicalName() + "CONNECTED";
    private static final String LOG_TAG = ConnectionActivity.class.getSimpleName() + " LOGGING ";


    private Button mConnectButton;
    private Button mDisconnectButton;
    private TextView mConnectionStatus;
    private ProgressBar mLoadingBar;

    private boolean mConnecting;
    private boolean mConnected;
    private LocalBroadcastManager mLocalBroadcastManager;
    SocketReceiver mSocketReceiver = new SocketReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SocketService.ACTION_CONNECTED);
        intentFilter.addAction(SocketService.ACTION_DISCONNECTED);
        intentFilter.addAction(SocketService.ACTION_SEND_MESSAGE);
        intentFilter.addAction(SocketService.ACTION_FAILED);
        intentFilter.addAction(SocketService.ACTION_CONNECTION_INITIATED);
        mLocalBroadcastManager.registerReceiver(mSocketReceiver, intentFilter);


        mConnectButton = findViewById(R.id.connect_connect);
        mDisconnectButton = findViewById(R.id.connect_disconnect);
        mConnectionStatus = findViewById(R.id.connect_status);
        mLoadingBar = findViewById(R.id.connect_loadingBar);

        mConnected = false;
        mConnecting = false;
        updateUIOnConnection();
        mLocalBroadcastManager.sendBroadcast(new Intent(SocketService.ACTION_GET_CONNECTION_STATUS));
    }

    public void navigateBack(View view) {
        mLocalBroadcastManager.unregisterReceiver(mSocketReceiver);
        finish();
    }

    public void connect(View view) {
        Intent intent = new Intent(this, SocketService.class);
        startService(intent);
    }

    public void disconnect(View view) {
        Intent intent = new Intent(this, SocketService.class);
        stopService(intent);
    }

    @Override
    protected void onDestroy() {
        mLocalBroadcastManager.unregisterReceiver(mSocketReceiver);
        super.onDestroy();
    }

    private void updateUIOnConnection() {
        mConnectButton.setEnabled(!mConnected);
        mDisconnectButton.setEnabled(mConnected);
        mConnectionStatus.setText(mConnected ? getString(R.string.status_connected) : getString(R.string.status_disconnected));
        mLoadingBar.setVisibility(mConnecting ? View.VISIBLE : View.GONE);
    }

    public class SocketReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            if (intentAction != null) {
                switch (intentAction) {
                    case SocketService.ACTION_CONNECTION_INITIATED:
                        mConnected = false;
                        mConnecting = true;
                        break;
                    case SocketService.ACTION_SEND_MESSAGE:
                        mConnected = true;
                        mConnecting = false;
                        break;
                    case SocketService.ACTION_CONNECTED:
                        mConnected = true;
                        mConnecting = false;
                        break;
                    case SocketService.ACTION_FAILED:
                        mConnected = false;
                        mConnecting = false;
                        Toast.makeText(context, "Failed to connect!", Toast.LENGTH_SHORT).show();
                        break;
                    case SocketService.ACTION_DISCONNECTED:
                        mConnected = false;
                        mConnecting = false;
                        mLocalBroadcastManager.unregisterReceiver(mSocketReceiver);
                        break;
                    default:
                        break;
                }

                updateUIOnConnection();
            }
            updateUIOnConnection();
        }
    }


}
