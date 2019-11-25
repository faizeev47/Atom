package com.example.fypversion2;

import android.content.Intent;
import android.os.Bundle;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import android.view.View;

import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.navigation.NavigationView;

import androidx.drawerlayout.widget.DrawerLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.view.Menu;
import android.widget.Button;
import android.widget.ImageView;

public class Dashboard extends AppCompatActivity implements View.OnClickListener {

    private AppBarConfiguration mAppBarConfiguration;
    Button headsetSettings;
    ImageView reading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        headsetSettings=findViewById(R.id.headsetSettings);
        headsetSettings.setOnClickListener(this);
        reading= findViewById(R.id.readingIcon);
        reading.setOnClickListener(this);

        DrawerLayout drawer = findViewById(R.id.drawer_layout);
        NavigationView navigationView = findViewById(R.id.nav_view);
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        mAppBarConfiguration = new AppBarConfiguration.Builder(
                R.id.nav_home)
                .setDrawerLayout(drawer)
                .build();


    }


    @Override
    public void onClick(View view) {
        if(view.getId()==headsetSettings.getId()){
            Intent h= new Intent(this,headset.class);
            startActivity(h);
        }
        if(view.getId()==reading.getId()){
            Intent r= new Intent(this,bookshelf.class);
            startActivity(r);
        }
    }
}
