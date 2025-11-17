package com.example.mealshare;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SplashScreenActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_splashscreen);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        boolean loggedIn = false;

        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            Intent intentOldUser = new Intent(SplashScreenActivity.this, MainActivity.class);
            Intent intentNewUser = new Intent(SplashScreenActivity.this, LoginActivity.class);
            if(loggedIn)
                startActivity(intentOldUser);
            else
                startActivity(intentNewUser);

            finish();
        }, 2000);
    }
}