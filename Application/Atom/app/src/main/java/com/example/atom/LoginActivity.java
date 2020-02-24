package com.example.atom;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

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
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import static com.example.atom.Helpers.isEmailValid;
import static com.example.atom.Helpers.passwordIsLong;

public class LoginActivity extends AppCompatActivity {
    private static final String LOG_TAG = LoginActivity.class.getSimpleName() + "LOG";
    private static final int RC_GOOGLE_SIGN_IN = 1;

    private FirebaseAuth mAuth;
    private GoogleSignInClient mGoogleSignInClient;

    private Button mLoginButton;
    private EditText mEmailEditText;
    private EditText mPasswordEditText;
    private ProgressBar mLoader;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        mAuth = FirebaseAuth.getInstance();

        mEmailEditText = findViewById(R.id.login_email);
        mPasswordEditText = findViewById(R.id.login_password);
        mLoader = findViewById(R.id.login_loading);
        mLoginButton = findViewById(R.id.login_sign_in_button);

        final TextView emailError = findViewById(R.id.login_email_error);
        final TextView passwordError = findViewById(R.id.login_password_error);
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

        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestEmail()
                .build();

        SignInButton googleSignInButton = findViewById(R.id.login_google_sign_in_button);
        googleSignInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                signInWithGoogle();
            }
        });

        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);
    }


    public void signInWithEmail(View view) {
        mLoader.setVisibility(ProgressBar.VISIBLE);
        String email = mEmailEditText.getText().toString();
        String password = mPasswordEditText.getText().toString();
        final TextView loginError = findViewById(R.id.login_error);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                    @Override
                    public void onComplete(@NonNull Task<AuthResult> task) {
                        if (task.isSuccessful()) {
                            FirebaseUser user = mAuth.getCurrentUser();
                            if (!user.isEmailVerified()) {
                                Log.d(LOG_TAG, "signInWithEmail: failure");
                                loginError.setText("Verify your email first!");

                            } else {
                                Log.d(LOG_TAG, "signInWithEmail: success");
                                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                                startActivity(intent);
                                finish();
                            }
                        } else {
                            Log.d(LOG_TAG, "signInWithEmail: failure");
                            loginError.setText(R.string.authentication_failed_error);
                            mLoader.setVisibility(ProgressBar.GONE);
                        }
                    }
                });
    }

    public void signInWithGoogle() {
        Log.d(LOG_TAG, "Fired");
        Intent intent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(intent, RC_GOOGLE_SIGN_IN);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_GOOGLE_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            handleSignInResult(task);
        }
    }

    private void handleSignInResult(Task<GoogleSignInAccount> completedTask) {
        try {
            GoogleSignInAccount account = completedTask.getResult(ApiException.class);
            Log.d(LOG_TAG, "signInWithEmail: success");
            Intent intent = new Intent(LoginActivity.this, MainActivity.class);
            startActivity(intent);
            finish();

        } catch(ApiException e) {
            Log.w(LOG_TAG, "signInResult:failed code=" + e.getStatusCode() + ": " + e.getMessage());
            Toast.makeText(this, R.string.google_sign_in_error, Toast.LENGTH_SHORT).show();
        }
    }

    public void openRegistration(View view) {
        Intent intent = new Intent(this, RegisterActivity.class);
        startActivity(intent);
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

    public void forgotPassword(View view) {
        Intent intent = new Intent(this, PasswordRecoverActivity.class);
        startActivity(intent);
    }
}
