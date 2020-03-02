package com.example.atom;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import static com.example.atom.ConnectionActivity.EXTRA_CONNECTION_STATUS;

public class MainActivity extends AppCompatActivity {

    private static final String LOG_TAG = MainActivity.class.getSimpleName() + " Logging";
    private static final String LOG_TAG_SOCKET = "Web Socket Event";
    private FirebaseAuth mAuth;

    private TextView mUser;
    private TextView mHeadsetStatus;

    private boolean mConnected;
    private LocalBroadcastManager mLocalBroadcastManager;
    SocketReceiver mSocketReceiver = new SocketReceiver();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mHeadsetStatus = findViewById(R.id.main_headset_status);

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(SocketService.ACTION_CONNECTED);
        intentFilter.addAction(SocketService.ACTION_DISCONNECTED);
        intentFilter.addAction(SocketService.ACTION_SEND_MESSAGE);
        mLocalBroadcastManager.registerReceiver(mSocketReceiver, intentFilter);
        updateUIOnConnection();

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        mUser = findViewById(R.id.main_user);
        if (currentUser != null) {
            String helloString = "Hi there, " + currentUser.getDisplayName().split(" ")[0] + "!";
            mUser.setText(helloString);
            return;
        }
        GoogleSignInAccount googleAccount = GoogleSignIn.getLastSignedInAccount(this);
        if (googleAccount != null) {
            String helloString = "Hi there, " + googleAccount.getDisplayName().split(" ")[0] + "!";
            mUser.setText(helloString);
            return;
        }
        Intent redirectLoginIntent = new Intent(MainActivity.this, LoginActivity.class);
        startActivity(redirectLoginIntent);
        finish();
    }

    public void openAccount(View view) {
        Intent intent = new Intent(this, AccountActivity.class);
        startActivity(intent);
    }

    public void openBooks(View view) {
        Intent intent = new Intent(this, BookActivity.class);
        if (mConnected) {
            intent.putExtra(EXTRA_CONNECTION_STATUS, "CONNECTED");
        }
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
        mHeadsetStatus.setText(mConnected ? "Connected" : "Disconnected");
        mHeadsetStatus.setTextColor(mConnected ? getColor(R.color.success) : getColor(R.color.warning));
    }

    @Override
    protected void onDestroy() {
        mLocalBroadcastManager.unregisterReceiver(mSocketReceiver);
        super.onDestroy();
    }



    public class SocketReceiver extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {
            String intentAction = intent.getAction();
            if (intentAction != null) {
                switch (intentAction) {
                    case SocketService.ACTION_SEND_MESSAGE:
                    case SocketService.ACTION_CONNECTED:
                        mConnected = true;
                        break;
                    case SocketService.ACTION_DISCONNECTED:
                        mConnected = false;
                        mLocalBroadcastManager.unregisterReceiver(mSocketReceiver);
                        break;
                    default:
                        break;
                }

                updateUIOnConnection();
            }
        }
    }
}
