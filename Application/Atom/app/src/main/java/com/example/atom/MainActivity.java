package com.example.atom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.atom.Headset.HeadsetState;
import com.example.atom.Headset.IntentFilterFactory;
import com.example.atom.Headset.ServiceAction;
import com.example.atom.Headset.SocketService;
import com.example.atom.Headset.StandardHeadsetReceiver;
import com.example.atom.Utilities.Utils;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {
    private static final String LOG_TAG = MainActivity.class.getSimpleName() + " Logging";

    private View mMainView;
    private TextView mHeadsetStatus;

    private HeadsetState mHeadsetState;
    private LocalBroadcastManager mLocalBroadcastManager;
    private SocketReceiver mSocketReceiver = new SocketReceiver();
    private StandardHeadsetReceiver mStandardReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Require user to be logged in
        FirebaseAuth mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        TextView welcomeUser = findViewById(R.id.main_user);
        if (currentUser == null) {
            // Otherwise redirect to login
            Utils.redirectToLogin(MainActivity.this, this);
        } else {
            String helloString = "Hi there, " + currentUser.getDisplayName().split(" ")[0] + "!";
            welcomeUser.setText(helloString);
        }

        mHeadsetStatus = findViewById(R.id.main_headset_status);
        mMainView = findViewById(R.id.main_view);

        mHeadsetState = HeadsetState.DISCONNECTED;

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mStandardReceiver = new StandardHeadsetReceiver(LOG_TAG, mMainView);

        mLocalBroadcastManager.registerReceiver(mSocketReceiver,
                IntentFilterFactory.createFilter(ServiceAction.STATUS_UPDATE));
        mLocalBroadcastManager.sendBroadcast(new Intent(SocketService.ACTION_GET_CONNECTION_STATUS));

        mLocalBroadcastManager.registerReceiver(mStandardReceiver,
                IntentFilterFactory.createStandardFilter());

        updateUIOnConnection();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mLocalBroadcastManager.sendBroadcast(new Intent(SocketService.ACTION_GET_CONNECTION_STATUS));
    }

    @Override
    protected void onDestroy() {
        mLocalBroadcastManager.unregisterReceiver(mSocketReceiver);
        mLocalBroadcastManager.unregisterReceiver(mStandardReceiver);
        super.onDestroy();
    }

    public void openAccount(View view) {
        Intent intent = new Intent(this, AccountActivity.class);
        startActivity(intent);
    }

    public void openBooks(View view) {
        Intent intent = new Intent(this, BookActivity.class);
        startActivity(intent);
    }


    public void openConnection(View view) {
        Intent intent = new Intent(this, ConnectionActivity.class);
        startActivity(intent);
    }

    public void openProgress(View view) {
        Intent intent = new Intent(this, ProgressActivity.class);
        startActivity(intent);
    }

    private void updateUIOnConnection() {
        if (mHeadsetState == HeadsetState.CONNECTED) {
            mHeadsetStatus.setText(getString(R.string.status_connected));
            mHeadsetStatus.setTextColor(getColor(R.color.success));
        } else {
            mHeadsetStatus.setText(getString(R.string.status_disconnected));
            mHeadsetStatus.setTextColor(getColor(R.color.warning));
        }
    }

    private class SocketReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            if (intentAction != null && intentAction.equals(SocketService.ACTION_RETURN_CONNECTION_STATUS)) {
                String status = intent.getStringExtra(SocketService.EXTRA_STATUS);
                if (status.equals("connected")) {
                    mHeadsetState = HeadsetState.CONNECTED;
                } else {
                    mHeadsetState = HeadsetState.DISCONNECTED;
                }
                updateUIOnConnection();
            }
        }
    }
}
