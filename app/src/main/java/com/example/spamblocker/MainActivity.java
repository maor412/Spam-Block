package com.example.spamblocker;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.view.Window;
import android.widget.ImageButton;

public class MainActivity extends AppCompatActivity {

    SharedPreferences sharedPreferences;
    ImageButton imageButton;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getSupportActionBar().hide();
        setContentView(R.layout.activity_main);
        initViews();
        setOnClickListeners();
        Intent serviceIntent = new Intent(this, MyForegroundService.class);
        startService(serviceIntent);
    }

    private void setOnClickListeners() {
        imageButton.setOnClickListener(new View.OnClickListener() {

            String imageName = (String) imageButton.getTag();
            @Override
            public void onClick(View v) {

                if (sharedPreferences.getString("button", "").equals("On")) {
                    imageButton.setImageResource(R.drawable.ic_off_button);
                    imageButton.setTag("Off");
                }
                else {
                    imageButton.setImageResource(R.drawable.ic_on_button);
                    imageButton.setTag("On");
                }
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString("button", (String) imageButton.getTag());
                editor.apply();
            }
        });
    }

    private void initViews() {
        sharedPreferences = getSharedPreferences("myPrefs", MODE_PRIVATE);
        imageButton = (ImageButton) findViewById(R.id.image_button);
        if (sharedPreferences.getString("button", "").equals("On"))
            imageButton.setImageResource(R.drawable.ic_on_button);
    }
}