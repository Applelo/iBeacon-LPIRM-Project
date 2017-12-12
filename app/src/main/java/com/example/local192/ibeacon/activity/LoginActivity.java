package com.example.local192.ibeacon.activity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;

import com.example.local192.ibeacon.R;

public class LoginActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        startActivity(new Intent(LoginActivity.this, StoryActivity.class));
    }
}
