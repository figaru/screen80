package com.devxop.screen80;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;

public class LauncherActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        AppStartup();
    }

    private void AppStartup(){

        Log.d("Screen Startup", "######################################");
        String device_id = StorageManager.get(getApplicationContext(), "device_id");
        String authCode = StorageManager.get(getApplicationContext(), StorageManager.AUTH_CODE);

        //device_id = "";

        Log.d("STARTUP", authCode);

        if(device_id.isEmpty()){
            Log.d("device_id", "No device id -> generating one.");
            String guid = java.util.UUID.randomUUID().toString();
            StorageManager.set(getApplicationContext(), "device_id", guid);

            Intent intent = new Intent(LauncherActivity.this,
                    LoginActivity.class);
            startActivity(intent);
            finish();
        }else if(authCode.isEmpty()){
            Intent intent = new Intent(LauncherActivity.this,
                    LoginActivity.class);
            startActivity(intent);
            finish();
        }else{
            Log.d("Device_id", device_id);


            // Launching the Sync Activity
            Intent intent = new Intent(LauncherActivity.this,
                    MainActivity.class);
            startActivity(intent);
            finish();
        }
    }
}
