package com.example.atom;

import android.content.Intent;
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
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.example.atom.Headset.IntentFilterFactory;
import com.example.atom.Headset.StandardHeadsetReceiver;
import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.snackbar.Snackbar;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FacebookAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.Arrays;

import static com.example.atom.Utilities.Utils.connectionActive;
import static com.example.atom.Utilities.Utils.hideKeyboard;
import static com.example.atom.Utilities.Utils.isEmailValid;
import static com.example.atom.Utilities.Utils.passwordIsLong;

public class LoginActivity extends AppCompatActivity {
    private static final String LOG_TAG = LoginActivity.class.getSimpleName() + "LOG";
    private static final int RC_GOOGLE_SIGN_IN = 1;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;
    private LoginManager mFacebookLoginManager;
    private CallbackManager mCallbackManager;

    private View mMainView;
    private Button mLoginButton;
    private EditText mEmailEditText;
    private EditText mPasswordEditText;
    private ProgressBar mLoadingBar;

    private LocalBroadcastManager mLocalBroadcastManager;
    private StandardHeadsetReceiver mStandardReceiver;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Make sure no user is signed in
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            enterApplication();
        }

        // Initialize UI components
        mMainView = findViewById(R.id.login_view);
        mEmailEditText = findViewById(R.id.login_email);
        mPasswordEditText = findViewById(R.id.login_password);
        mLoadingBar = findViewById(R.id.login_loading);
        mLoginButton = findViewById(R.id.login_sign_in_button);


        final TextView emailError = findViewById(R.id.login_email_error);
        final TextView passwordError = findViewById(R.id.login_password_error);
        // Add input validation on text changed event to input fields
        TextWatcher loginButtonEnabler = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) { }

            @Override
            public void afterTextChanged(Editable s) {
                mLoginButton.setEnabled(checkForm(mEmailEditText.getText().toString(), emailError,
                        mPasswordEditText.getText().toString(), passwordError));
            }
        };
        mEmailEditText.addTextChangedListener(loginButtonEnabler);
        mPasswordEditText.addTextChangedListener(loginButtonEnabler);

        // Configure required parameters and objects for Oauth sign in
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        mFacebookLoginManager = LoginManager.getInstance();
        mCallbackManager = CallbackManager.Factory.create();
        mFacebookLoginManager.registerCallback(mCallbackManager, new FacebookCallback<LoginResult>() {
            @Override
            public void onSuccess(LoginResult loginResult) {
                Log.d(LOG_TAG, "signInWIthFacebook: success");
                handleFacebookAccessToken(loginResult.getAccessToken());
            }

            @Override
            public void onCancel() {
                Log.d(LOG_TAG, "signInWithFacebook: cancelled");
            }

            @Override
            public void onError(FacebookException error) {
                Log.d(LOG_TAG, "signInWithFacebook: failed");
            }
        });

        Log.d(LOG_TAG, "onStart: parameters confirmed!");

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

    public void signInWithEmail(View view) {
        mLoadingBar.setVisibility(ProgressBar.VISIBLE);
        hideKeyboard(this, view);
        if (!connectionActive(this)) {
            Snackbar.make(mMainView,
                    R.string.no_internet_error,
                    Snackbar.LENGTH_LONG).show();
            mLoadingBar.setVisibility(ProgressBar.INVISIBLE);
            return;
        }
        final TextView loginError = findViewById(R.id.login_error);

        String email = mEmailEditText.getText().toString();
        String password = mPasswordEditText.getText().toString();
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (user == null || !user.isEmailVerified()) {
                                Log.d(LOG_TAG, "signInWithEmail: failure");
                                loginError.setText(R.string.email_verification_error);
                                mLoadingBar.setVisibility(ProgressBar.INVISIBLE);

                            } else {
                                Log.d(LOG_TAG, "signInWithEmail: success");
                                Log.d(LOG_TAG, user.getUid());
                                enterApplication();
                            }
                        } else {
                            Log.d(LOG_TAG, "signInWithEmail: failure");
                            loginError.setText(R.string.authentication_failed_error);
                            mLoadingBar.setVisibility(ProgressBar.INVISIBLE);
                        }
                    }
                });
    }


    public void signInWithGoogle(View view) {
        if (!connectionActive(this)) {
            Snackbar.make(mMainView,
                    R.string.no_internet_error,
                    Snackbar.LENGTH_LONG).show();
            mLoadingBar.setVisibility(ProgressBar.INVISIBLE);
            return;
        }
        Intent intent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(intent, RC_GOOGLE_SIGN_IN    );

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        mCallbackManager.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Log.d(LOG_TAG, "onActivityResult: received from user!");
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    firebaseAuthWithGoogle(account);
                } else {
                    Log.w(LOG_TAG, "signInWithGoogle: failed");
                }
            } catch (ApiException ae)  {
                Log.w(LOG_TAG, "signInWithGoogle: failed code=" + ae.getStatusCode(), ae);
            }
        }
    }

    private void firebaseAuthWithGoogle(GoogleSignInAccount account) {
        Log.d(LOG_TAG, "firebaseAuthWithGoogle: id=" + account.getId());

        Log.d(LOG_TAG, "firebaseAuthWithGoogle: id_token=" + account.getIdToken());
        // Exchange the google account credential for a firebase credential
        AuthCredential credential = GoogleAuthProvider.getCredential(account.getIdToken(), null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            // Sign in success
                            Log.d(LOG_TAG, "signInWithCredential: success");
                            FirebaseUser user = mAuth.getCurrentUser();

                            Log.d(LOG_TAG, "name: " + user.getDisplayName());
                            Log.d(LOG_TAG, "email: " + user.getEmail());

                            enterApplication();
                        } else {
                            Log.w(LOG_TAG, "signInWithCredential: failure", task.getException());
                            Snackbar.make(mMainView,
                                    "Sorry, could not sign in with Google!",
                                    Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
    }

    public void handleFacebookAccessToken(AccessToken accessToken) {
        Log.d(LOG_TAG, "handleFacebookAccessToken: " + accessToken);

        AuthCredential credential = FacebookAuthProvider.getCredential(accessToken.getToken());
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            Log.d(LOG_TAG, "signInWithCredential: success");
                            enterApplication();
                        } else {
                            Log.w(LOG_TAG, "signInWIthCredential: ", task.getException());
                            Snackbar.make(mMainView,
                                    "Sorry, could not sign in with Facebook!",
                                    Snackbar.LENGTH_LONG).show();
                        }
                    }
                });
    }

    public void signInWithFacebook(View view) {
        mFacebookLoginManager.logIn(this, Arrays.asList("email"));
    }


    private boolean checkForm(String email, TextView emailError, String password, TextView passwordError) {
        if (email.isEmpty()) {
            emailError.setText(R.string.email_missing_error);
            return false;
        }
        if (!isEmailValid(email)) {
            emailError.setText(R.string.invalid_email_error);
            return false;
        }
        else {
            emailError.setText("");
        }

        if (password.isEmpty()) {
            passwordError.setText(R.string.password_missing_error);
            return false;
        }
        else if (!passwordIsLong(password)) {
            passwordError.setText(R.string.password_length_error);
            return false;
        }
        else {
            passwordError.setText("");
        }
        return true;
    }

    public void enterApplication() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    public void openRegistration(View view) {
        if (!connectionActive(this)) {
            Snackbar.make(mMainView,
                    R.string.no_internet_error,
                    Snackbar.LENGTH_LONG).show();
            mLoadingBar.setVisibility(ProgressBar.INVISIBLE);
            return;
        }
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
    }

    public void forgotPassword(View view) {
        Intent intent = new Intent(this, PasswordRecoverActivity.class);
        startActivity(intent);
    }
}
