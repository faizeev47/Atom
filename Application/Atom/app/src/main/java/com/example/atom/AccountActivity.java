package com.example.atom;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import static com.example.atom.Utils.connectionActive;

public class AccountActivity extends AppCompatActivity {
    private static final String LOG_TAG = LoginActivity.class.getSimpleName() + "LOG";
    private static final int COMMON_ACCOUNT = 0;
    private static final int GOOGLE_ACCOUNT = 1;

    private int mAccountType;
    private FirebaseAuth mFirebaseAuth;
    private FirebaseUser mFirebaseAccount;
    private GoogleSignInClient mGoogleSignInClient;
    private GoogleSignInAccount mGoogleAccount;

    private String mName;
    private String mEmail;

    private ProgressBar mLoadingBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_account);

        mFirebaseAuth = FirebaseAuth.getInstance();
        mFirebaseAccount = mFirebaseAuth.getCurrentUser();
        if (mFirebaseAccount != null) {
            mAccountType = COMMON_ACCOUNT;
            mName = mFirebaseAccount.getDisplayName();
            mEmail = mFirebaseAccount.getEmail();
        }
        else {
            mGoogleAccount = GoogleSignIn.getLastSignedInAccount(this);
            if (mGoogleAccount != null) {
                mAccountType = GOOGLE_ACCOUNT;
                mName = mGoogleAccount.getDisplayName();
                mEmail = mGoogleAccount.getEmail();

                findViewById(R.id.account_change_password).setVisibility(Button.GONE);
            }
        }

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);


        mLoadingBar = findViewById(R.id.account_loading);

        TextView nameTextView = findViewById(R.id.account_name);
        TextView emailTextView = findViewById(R.id.account_email);

        nameTextView.setText(mName);
        emailTextView.setText(mEmail);

    }

    public void navigateBack(View view) {
        finish();
    }

    public void changePassword(View view) {
        mLoadingBar.setVisibility(ProgressBar.VISIBLE);

        if (!connectionActive(this)) {
            Toast.makeText(this, getString(R.string.no_internet_error), Toast.LENGTH_SHORT).show();
            mLoadingBar.setVisibility(ProgressBar.GONE);
            return;
        }
        mFirebaseAuth.sendPasswordResetEmail(mFirebaseAccount.getEmail())
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
        if (mAccountType == COMMON_ACCOUNT) {
            mFirebaseAuth.signOut();
        } else {
            mGoogleSignInClient.signOut()
                    .addOnCompleteListener(this, new OnCompleteListener<Void>() {
                        @Override
                        public void onComplete(@NonNull Task<Void> task) {
                            resetApp(AccountActivity.this);
                        }
                    });
        }
        resetApp(this);
    }

    private void resetApp(Context context) {
        Intent intent = new Intent(context, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
    }
}
