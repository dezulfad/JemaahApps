package com.example.jemaahapps;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;

public class SplashActivity extends Activity {

    private static final String PREFS_NAME = "auth";
    private static final String KEY_IS_AUTHENTICATED = "isAuthenticated";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        new Handler().postDelayed(() -> {
            boolean isLoggedIn = isUserLoggedIn();

            Class<?> next = isLoggedIn ? MainActivity.class : LoginActivity.class;
            startActivity(new Intent(SplashActivity.this, next));
            finish();
        }, 4000); // 4 seconds
    }

    private boolean isUserLoggedIn() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        return prefs.getBoolean(KEY_IS_AUTHENTICATED, false);
    }
}