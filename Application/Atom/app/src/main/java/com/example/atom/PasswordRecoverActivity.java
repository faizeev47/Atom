package com.example.atom;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.atom.Headset.IntentFilterFactory;
import com.example.atom.Headset.StandardHeadsetReceiver;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.FirebaseAuth;

import static com.example.atom.Utilities.Utils.isEmailValid;

public class PasswordRecoverActivity extends AppCompatActivity {
    private static final String LOG_TAG = LoginActivity.class.getSimpleName() + "LOG";

    private FirebaseAuth mAuth;

    private View mMainView;
    private Button mRecoverButton;
    private EditText mEmailEditText;
    private ProgressBar mRecoverLoader;

    private LocalBroadcastManager mLocalBroadcastManager;
    private StandardHeadsetReceiver mStandardReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_password_recover);

        mAuth = FirebaseAuth.getInstance();

        mMainView = findViewById(R.id.recover_view);
        mRecoverButton = findViewById(R.id.recover_recover_button);
        mEmailEditText = findViewById(R.id.recover_email);
        mRecoverLoader = findViewById(R.id.recover_loading);
        final TextView emailError = findViewById(R.id.recover_email_error);

        mEmailEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
               mRecoverButton.setEnabled(checkForm(mEmailEditText.getText().toString(), emailError));
            }
        });

        mLocalBroadcastManager = LocalBroadcastManager.getInstance(this);
        mStandardReceiver = new StandardHeadsetReceiver(LOG_TAG, mMainView);
        mLocalBroadcastManager.registerReceiver(mStandardReceiver,
                IntentFilterFactory.createStandardFilter());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mLocalBroadcastManager.unregisterReceiver(mStandardReceiver);
    }

    public void navigateBack(View view) {
        finish();
    }

    private boolean checkForm(String email, TextView emailError) {
        if (email.isEmpty()) {
            emailError.setText(getString(R.string.email_missing_error));
            return false;
        }
        if (!isEmailValid(email)) {
            emailError.setText(getString(R.string.invalid_email_error));
            return false;
        }
        else {
            emailError.setText("");        }
        return true;
    }

    public void recoverPassword(View view) {
        mRecoverLoader.setVisibility(ProgressBar.VISIBLE);
        final TextView recoverError = findViewById(R.id.recover_error);
        String email = mEmailEditText.getText().toString();

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            Log.d(LOG_TAG, "sendPasswordResetEmail: success");
                            Snackbar.make(mMainView,
                                    "A password reset email has been sent to your provided email!",
                                    Snackbar.LENGTH_LONG).show();
                            finish();
                        }
                        else {
                            Log.d(LOG_TAG, "sendPasswordResetEMail: failure");
                            recoverError.setText(R.string.unknown_email_error);
                            mRecoverLoader.setVisibility(ProgressBar.GONE);
                        }
                    }
                });
    }
}
