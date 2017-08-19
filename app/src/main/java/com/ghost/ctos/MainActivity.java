package com.ghost.ctos;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.content.Context;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "ctos_preference";
    public static String PATH = Environment.DIRECTORY_DOCUMENTS;

    private Context context;
    private boolean perm_gps_act;
    private boolean perm_cam_act;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Store the context for later use in objects
        context = this;
        checkActivityPermissions();

        Button gps_act = (Button) findViewById(R.id.button_gps);
        if(gps_act!=null) gps_act.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(perm_gps_act) {
                    Intent intent = new Intent(context, GPSActivity.class);
                    startActivity(intent);
                }else{
                    String msg = getResources().getString(R.string.error_permission);
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    checkActivityPermissions();
                }
            }
        });

        Button cam_act = (Button) findViewById(R.id.button_vr);
        if(cam_act!=null) cam_act.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if(perm_cam_act) {
                    Intent intent = new Intent(context, VRActivity.class);
                    startActivity(intent);
                }else{
                    String msg = getResources().getString(R.string.error_permission);
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                    checkActivityPermissions();
                }
            }
        });
    }

    // Check activity's permissions to avoid errors
    private void checkActivityPermissions() {
        boolean fine = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        boolean sd_w = ContextCompat.checkSelfPermission(this,
                Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        boolean sd_r = ContextCompat.checkSelfPermission(this,
                Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED;
        if (!fine || !sd_r || !sd_w) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 200);
        }
        perm_gps_act = fine && sd_r && sd_w;
        perm_cam_act = true;
    }

    // Triggered by the requestPermissions function, when the user has answered
    @Override
    public void onRequestPermissionsResult(int permsRequestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (permsRequestCode) {
            case 200:
                boolean fine = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean sd_r = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                boolean sd_w = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                perm_gps_act = fine && sd_r && sd_w;
                perm_cam_act = true;
                break;
        }
    }
}
