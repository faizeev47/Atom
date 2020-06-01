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
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;

import static com.example.atom.Utilities.Utils.connectionActive;
import static com.example.atom.Utilities.Utils.containsSpecial;
import static com.example.atom.Utilities.Utils.isEmailValid;
import static com.example.atom.Utilities.Utils.passwordIsLong;

public class RegisterActivity extends AppCompatActivity {
    private static final String LOG_TAG = LoginActivity.class.getSimpleName() + "LOG";

    private FirebaseAuth mAuth;

    private View mMainView;
    private Button mRegisterButton;
    private EditText mFullnameEditText;
    private EditText mEmailEditText;
    private EditText mPasswordEditText;
    private EditText mConfirmationEditText;
    private ProgressBar mLoadingBar;

    boolean mAdmissibleRegistration;

    private LocalBroadcastManager mLocalBroadcastManager;
    private StandardHeadsetReceiver mStandardReceiver;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        mAuth = FirebaseAuth.getInstance();

        // Initialize UI components
        mMainView = findViewById(R.id.register_view);
        mRegisterButton = findViewById(R.id.register_register_button);
        mLoadingBar = findViewById(R.id.register_loading);
        mFullnameEditText = findViewById(R.id.register_fullname);
        mEmailEditText = findViewById(R.id.register_email);
        mPasswordEditText = findViewById(R.id.register_password);
        mConfirmationEditText = findViewById(R.id.register_confirmation);

        final TextView fullnameError = findViewById(R.id.register_fullname_error);
        final TextView emailError = findViewById(R.id.register_email_error);
        final TextView passwordError = findViewById(R.id.register_password_error);

        TextWatcher registerButtonEnabler = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                boolean field = checkForm(mFullnameEditText.getText().toString(), fullnameError,
                        mEmailEditText.getText().toString(), emailError,
                        mPasswordEditText.getText().toString(), mConfirmationEditText.getText().toString(),
                        passwordError);
                Log.d(LOG_TAG, field ? "common: yes" : "common: no");
                mRegisterButton.setEnabled(field);
            }
        };
        mFullnameEditText.addTextChangedListener(registerButtonEnabler);
        mEmailEditText.addTextChangedListener(registerButtonEnabler);
        mPasswordEditText.addTextChangedListener(registerButtonEnabler);
        mConfirmationEditText.addTextChangedListener(registerButtonEnabler);


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

    public void createNewUser(View view) {
        mLoadingBar.setVisibility(ProgressBar.VISIBLE);

        if (!connectionActive(this)) {
            Snackbar.make(mMainView,
                    R.string.no_internet_error,
                    Snackbar.LENGTH_LONG).show();
            mLoadingBar.setVisibility(ProgressBar.GONE);
            return;
        }

        String email = mEmailEditText.getText().toString();
        String password = mPasswordEditText.getText().toString();

        final TextView registerError = findViewById(R.id.register_error);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(LOG_TAG, "createUserWithEmail: success");
                            FirebaseUser user =  mAuth.getCurrentUser();
                            UserProfileChangeRequest profileUpdates = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(mFullnameEditText.getText().toString())
                                    .build();
                            user.updateProfile(profileUpdates)
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.d(LOG_TAG, "updateProfile: success");
                                            }
                                            else {
                                                Log.d(LOG_TAG, "updateProfile: failure");
                                            }
                                        }
                                    });
                            user.sendEmailVerification()
                                    .addOnCompleteListener(new OnCompleteListener<Void>() {
                                        @Override
                                        public void onComplete(@NonNull Task<Void> task) {
                                            if (task.isSuccessful()) {
                                                Log.d(LOG_TAG, "sendEmailVerification: success");
                                            }
                                            else {
                                                Log.d(LOG_TAG, "sendEmailVerification: failure");
                                            }
                                        }
                                    });
                            Snackbar.make(mMainView,
                                    "Your account has been created! We've sent you a link to verify your email",
                                    Snackbar.LENGTH_LONG)
                                        .show();RegisterActivity.this.finish();
                        } else {
                            Log.d(LOG_TAG, "createUserWithEmail: failure");
                            registerError.setText(R.string.email_used_error);
                            mLoadingBar.setVisibility(ProgressBar.GONE);
                        }
                    }
                });
    }

    public void navigateBack(View view) {
        finish();
    }

    private boolean checkForm(String fullname, TextView fullnameError,
                               String email, TextView emailError,
                              String password,
                              String confirmation, TextView passwordError) {
        if (fullname.isEmpty()) {
            fullnameError.setText(R.string.fullname_missing_error);
            return false;
        }
        if (containsSpecial(fullname)) {
            fullnameError.setText(R.string.invalid_name_error);
            return false;
        } else {
            fullnameError.setText("");
            Log.d(LOG_TAG, mAdmissibleRegistration ? "fullname: yes" : "fullname: no");
        }

        if (email.isEmpty()) {
            emailError.setText(getString(R.string.email_missing_error));
            return false;
        }
        if (!isEmailValid(email)) {
            emailError.setText(getString(R.string.invalid_email_error));
            return false;
        }
        else {
            emailError.setText("");
            Log.d(LOG_TAG, mAdmissibleRegistration ? "email: yes" : "email: no");
        }

        if (password.isEmpty()) {
            passwordError.setText(getString(R.string.password_missing_error));
            return false;
        }
        if (!passwordIsLong(password)) {
            passwordError.setText(getString(R.string.password_length_error));
            return false;
        }
        if (confirmation.isEmpty()) {
            passwordError.setText(R.string.confirm_missing_error);
            return false;
        }
        else {
            passwordError.setText("");
            Log.d(LOG_TAG, mAdmissibleRegistration ? "password: yes" : "password: no");
        }

        if (!confirmation.equals(password)) {
            passwordError.setText(R.string.passwords_mismatch_error);
            return false;
        }
        else {
            passwordError.setText("");
            Log.d(LOG_TAG, mAdmissibleRegistration ? "confirm: yes" : "confirm: no");
        }
        return true;
    }
}
