package com.example.atom;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.atom.Headset.IntentFilterFactory;
import com.example.atom.Headset.StandardHeadsetReceiver;
import com.example.atom.Utilities.Utils;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import static com.example.atom.Headset.ServiceAction.CONNECTION;
import static com.example.atom.Headset.ServiceAction.CONNECTION_FAILED;
import static com.example.atom.Headset.ServiceAction.CONNECTION_INITIATION;
import static com.example.atom.Headset.ServiceAction.DISCONNECTION;
import static com.example.atom.Headset.ServiceAction.STATE_UPDATE;
import static com.example.atom.Utilities.Utils.connectionActive;

public class AccountActivity extends AppCompatActivity {
    private static final String LOG_TAG = LoginActivity.class.getSimpleName() + "LOG";

    private FirebaseAuth mAuth;

    private String mEmail;

    private View mMainView;
    private ProgressBar mLoadingBar;

    private LocalBroadcastManager mLocalBroadcastManager;
    private StandardHeadsetReceiver mStandardReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        // Require the user to be logged in
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            // Otherwise redirect to login
            Utils.redirectToLogin(AccountActivity.this, this);
        } else {
            TextView nameTextView = findViewById(R.id.account_name);
            TextView emailTextView = findViewById(R.id.account_email);

            nameTextView.setText(currentUser.getDisplayName());
            mEmail = currentUser.getEmail();
            emailTextView.setText(mEmail);
        }

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mStandardReceiver = new StandardHeadsetReceiver(LOG_TAG, mMainView);
        mLocalBroadcastManager.registerReceiver(mStandardReceiver,
                IntentFilterFactory.createStandardFilter());

        // Initialize UI elements
        mMainView = findViewById(R.id.account_view);
        mLoadingBar = findViewById(R.id.account_loading);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocalBroadcastManager.unregisterReceiver(mStandardReceiver);
    }

    public void navigateBack(View view) {
        finish();
    }

    public void changePassword(View view) {
        mLoadingBar.setVisibility(ProgressBar.VISIBLE);

        if (!connectionActive(this)) {
            Snackbar.make(mMainView,
                    R.string.no_internet_error,
                    Snackbar.LENGTH_LONG).show();
            mLoadingBar.setVisibility(ProgressBar.GONE);
            return;
        }
        mAuth.sendPasswordResetEmail(mEmail)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(LOG_TAG, "sendPasswordResetEmail: success");
                            Toast.makeText(AccountActivity.this, "We've sent a email containing the password reset instructions to you. You can reset the password from there. Thank you!", Toast.LENGTH_LONG).show();
                        } else {
                            Log.d(LOG_TAG, "sendPasswordResetEmail: failure");
                            Toast.makeText(AccountActivity.this, "Password change failed! Please try again later.", Toast.LENGTH_LONG).show();
                        }
                        mLoadingBar.setVisibility(ProgressBar.GONE);
                    }
                });
    }

    public void signOut(View view) {
        mAuth.signOut();
        resetApp(this);
    }

    private void resetApp(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
