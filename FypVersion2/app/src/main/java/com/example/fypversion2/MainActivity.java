package com.example.fypversion2;

import androidx.appcompat.app.AppCompatActivity;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

public class MainActivity extends AppCompatActivity {
    EditText Email, Password;
    Button LogInButton, RegisterButton,nextButton;
    FirebaseAuth mAuth;
    FirebaseAuth.AuthStateListener mAuthListner;
    FirebaseUser mUser;
    String email, password;
    ProgressDialog dialog;
    DatabaseReference mdatabase;
    public static final String userEmail="";

    public static final String TAG="LOGIN";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        LogInButton = (Button) findViewById(R.id.Login);

        RegisterButton = (Button) findViewById(R.id.buttonRegister);

        nextButton = (Button) findViewById(R.id.next);

        Email = (EditText) findViewById(R.id.editEmail);
        Password = (EditText) findViewById(R.id.editPassword);
        dialog = new ProgressDialog(this);
        mAuth = FirebaseAuth.getInstance();
        mUser = FirebaseAuth.getInstance().getCurrentUser();
        mAuthListner = new FirebaseAuth.AuthStateListener() {
            @Override
            public void onAuthStateChanged( FirebaseAuth firebaseAuth) {
                if (mUser != null) {
                    Intent intent = new Intent(MainActivity.this, Dashboard.class);
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                    startActivity(intent);
                }
                else
                {
                    Log.d(TAG,"AuthStateChanged:Logout");
                }

            }
        };
        LogInButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                userSign();
            }
        });

        nextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, Dashboard.class);
                startActivity(intent);


            }
        });
        RegisterButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(MainActivity.this, register.class);
                startActivity(intent);

            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        mAuth.removeAuthStateListener(mAuthListner);

    }

    @Override
    protected void onStop() {
        super.onStop();
        if (mAuthListner != null) {
            mAuth.removeAuthStateListener(mAuthListner);
        }

    }

    @Override
    public void onBackPressed() {
        MainActivity.super.finish();
    }



    private void userSign() {
        email = Email.getText().toString().trim();
        password = Password.getText().toString().trim();
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(MainActivity.this, "Enter the correct Email", Toast.LENGTH_SHORT).show();
            return;
        } else if (TextUtils.isEmpty(password)) {
            Toast.makeText(MainActivity.this, "Enter the correct password", Toast.LENGTH_SHORT).show();
            return;
        }
        dialog.setMessage("Loging in please wait...");
        dialog.setIndeterminate(true);
        dialog.show();
        mAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
            @Override
            public void onComplete(Task<AuthResult> task) {
                if (!task.isSuccessful()) {
                    dialog.dismiss();
                    Toast.makeText(MainActivity.this, "Login not successfull", Toast.LENGTH_SHORT).show();
                } else {
                    dialog.dismiss();
                    Email.getText().clear();

                    Password.getText().clear();
                    Intent intent = new Intent(MainActivity.this, Dashboard.class);
                    intent.putExtra(userEmail,email);

                    startActivity(intent);
                    //checkIfEmailVerified();

                }
            }
        });

    }
    private void checkIfEmailVerified(){
        FirebaseUser users=FirebaseAuth.getInstance().getCurrentUser();
        mdatabase = FirebaseDatabase.getInstance().getReference().child("Users");
        boolean emailVerified=users.isEmailVerified();
        if(!emailVerified){
            Toast.makeText(this,"Verify the Email Id",Toast.LENGTH_SHORT).show();
            mAuth.signOut();
            finish();
        }
        else {
            Email.getText().clear();

            Password.getText().clear();
            Intent intent = new Intent(MainActivity.this, Dashboard.class);
            intent.putExtra(userEmail,email);

            startActivity(intent);

        }
    }
}
