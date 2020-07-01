package com.example.atom;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().post(new Runnable() {
            @Override
            public void run() {
                ImageView splashImage = findViewById(R.id.splash_image);
                splashImage.setAlpha(0f);
                splashImage.animate()
                        .alpha(0.9f)
                        .rotation(720f)
                        .setDuration(4000);
            }
        });
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent intent = new Intent(SplashActivity.this, MainActivity.class);

                SplashActivity.this.startActivity(intent);
                SplashActivity.this.finish();
            }
        }, 5000);
    }
}
