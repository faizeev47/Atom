package com.example.atom;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.atom.Headset.HeadsetState;
import com.example.atom.Headset.IntentFilterFactory;
import com.example.atom.Headset.SocketService;
import com.example.atom.Headset.StandardHeadsetReceiver;

import static com.example.atom.Headset.ServiceAction.*;
import static com.example.atom.Utilities.Utils.toFirstCaps;

public class ConnectionActivity extends AppCompatActivity {
    public static final String EXTRA_CONNECTION_STATUS = ConnectionActivity.class.getCanonicalName() + "CONNECTED";
    private static final String LOG_TAG = ConnectionActivity.class.getSimpleName() + " LOGGING ";

    private View mMainView;
    private Button mConnectButton;
    private Button mDisconnectButton;
    private TextView mConnectionStatus;
    private TextView mConnectionState;
    private ProgressBar mLoadingBar;

    private HeadsetState mHeadsetState;

    private LocalBroadcastManager mLocalBroadcastManager;

    private StandardHeadsetReceiver mStandardReceiver;
    private SocketReceiver mSocketReceiver = new SocketReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection);

        // Initialize UI components
        mMainView = findViewById(R.id.connect_view);
        mConnectButton = findViewById(R.id.connect_connect);
        mDisconnectButton = findViewById(R.id.connect_disconnect);
        mConnectionStatus = findViewById(R.id.connect_status);
        mConnectionState = findViewById(R.id.connect_state);
        mLoadingBar = findViewById(R.id.connect_loadingBar);

        mHeadsetState = HeadsetState.DISCONNECTED;

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mStandardReceiver = new StandardHeadsetReceiver(LOG_TAG, mMainView);

        mLocalBroadcastManager.registerReceiver(mStandardReceiver,
                IntentFilterFactory.createStandardFilter());
        mLocalBroadcastManager.registerReceiver(mSocketReceiver,
                IntentFilterFactory.createFilter(
                        STATUS_UPDATE,
                        STATE_UPDATE,
                        CONNECTION_INITIATION,
                        CONNECTION_FAILED,
                        CONNECTION,
                        DISCONNECTION));
        mLocalBroadcastManager.sendBroadcast(new Intent(SocketService.ACTION_GET_CONNECTION_STATUS));

        updateUI();
    }

    @Override
    protected void onDestroy() {
        mLocalBroadcastManager.unregisterReceiver(mSocketReceiver);
        mLocalBroadcastManager.unregisterReceiver(mStandardReceiver);
        if (mHeadsetState != HeadsetState.CONNECTED) {
            disconnect();
        }
        super.onDestroy();
    }

    public void navigateBack(View view) {
        mLocalBroadcastManager.unregisterReceiver(mSocketReceiver);
        finish();
    }

    public void connect(View view) {
        if (!SocketService.SERVICE_RUNNING) {
            Intent intent = new Intent(this, SocketService.class);
            startService(intent);
            Log.d(LOG_TAG, "NOT RUNNING");
        }
        else {
            Log.d(LOG_TAG, "RUNNING");
        }
    }

    public void disconnect() {
        if (SocketService.SERVICE_RUNNING) {
            Intent intent = new Intent(this, SocketService.class);
            stopService(intent);
        }
    }

    public void disconnect(View view) {
        disconnect();
    }

    private void updateUI() {
        switch (mHeadsetState) {
            case DISCONNECTED:
                mConnectButton.setEnabled(true);
                mDisconnectButton.setEnabled(false);
                mConnectionState.setText("");
                mConnectionStatus.setText(getString(R.string.status_disconnected));
                mConnectionStatus.setTextColor(getColor(R.color.warning));
                break;
            case CONNECTING:
                mConnectButton.setEnabled(false);
                mDisconnectButton.setEnabled(false);
                mConnectionState.setText(getString(R.string.state_attempting_connection));
                mLoadingBar.setVisibility(View.VISIBLE);
                break;
            case FAILED:
                mConnectButton.setEnabled(true);
                mDisconnectButton.setEnabled(false);
                mConnectionState.setText(getString(R.string.state_failed_to_connect));
                mLoadingBar.setVisibility(View.INVISIBLE);
                break;
            case CONNECTED:
                mConnectButton.setEnabled(false);
                mDisconnectButton.setEnabled(true);
                mConnectionState.setText("");
                mConnectionStatus.setText(getString(R.string.status_connected));
                mConnectionStatus.setTextColor(getColor(R.color.success));
                mLoadingBar.setVisibility(View.INVISIBLE);
                break;
            default:
                break;
        }
    }

    private void updateUI(String state) {
        updateUI();
        if (mHeadsetState == HeadsetState.CONNECTING) {
            mConnectionState.setText(toFirstCaps(state) + "…");
        }
    }

    public class SocketReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            String state = null;
            if (intentAction != null) {
                switch (intentAction) {
                    case SocketService.ACTION_CONNECTION_INITIATED:
                        mHeadsetState = HeadsetState.CONNECTING;
                        state = "Attempting to connect";
                        break;
                    case SocketService.ACTION_UPDATE_CONNECTION_STATE:
                        mHeadsetState = HeadsetState.CONNECTING;
                        state = intent.getStringExtra(SocketService.EXTRA_STATE);
                        break;
                    case SocketService.ACTION_RESPOND_CONNECTED:
                        mHeadsetState = HeadsetState.CONNECTED;
                        break;
                    case SocketService.ACTION_FAILED:
                        mHeadsetState = HeadsetState.FAILED;
                        break;
                    case SocketService.ACTION_RESPOND_DISCONNECTED:
                        mHeadsetState = HeadsetState.DISCONNECTED;
                        break;
                    case SocketService.ACTION_RETURN_CONNECTION_STATUS:
                        String status = intent.getStringExtra(SocketService.EXTRA_STATUS);
                        if (status != null && status.equals("connected")) {
                            mHeadsetState = HeadsetState.CONNECTED;
                        } else {
                            mHeadsetState = HeadsetState.DISCONNECTED;
                        }
                        break;
                }
            }
            if (state != null) {
                updateUI(state);
            } else {
                updateUI();
            }
        }
    }


}
