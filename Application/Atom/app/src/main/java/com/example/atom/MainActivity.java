package com.example.atom;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private TextView mUser;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mAuth = FirebaseAuth.getInstance();

        // Check if user is already signed in
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
        startActivity(intent);
    }
}
