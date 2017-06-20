package com.ghost.ctos;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Date;
import java.text.SimpleDateFormat;


public class MainActivity extends Activity implements LocationListener {

    private LocationManager lm;

    private double latitude;
    private double longitude;
    private double altitude;
    private long time;
    private String provider;
    private float h_accuracy;
    private float speed;

    private boolean permission;
    private boolean logging;
    private int format;
    private Context context;

    private static final String PREFS_NAME = "ctos_preference";
    private static final int DDD = Location.FORMAT_DEGREES;
    private static final int DMM = Location.FORMAT_MINUTES;
    private static final int DMS = Location.FORMAT_SECONDS;

    TextView wgs84_res;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout of the activity
        setContentView(R.layout.activity_main);

        // Load the text display
        wgs84_res = (TextView) findViewById(R.id.result);

        // Store the context for later use in objects
        context = this;

        // Implement the first button to switch display
        final Button button1 = (Button) findViewById(R.id.button_switch);
        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Switch between the 3 display modes
                format += 1;
                format %= 3;
                // Update immediately the display
                updateDisplay();
            }
        });

        // Implement the second button to share location
        final Button button2 = (Button) findViewById(R.id.button_share);
        button2.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Create the GMaps compatible message
                String lat, lon, message;
                lat = String.valueOf(latitude);
                lon = String.valueOf(longitude);
                message = String.format(getResources().getString(R.string.gmaps_url), lat, lon);
                // Start the sharing activity
                Intent sendIntent = new Intent();
                sendIntent.setAction(Intent.ACTION_SEND);
                sendIntent.putExtra(Intent.EXTRA_TEXT, message);
                sendIntent.setType("text/plain");
                startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_to)));
            }
        });

        // Implement the third button to log locations
        final Button button3 = (Button) findViewById(R.id.button_view);
        button3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Start or stop logging service depending on the latter's state
                Intent intent = new Intent(context, LogPositionService.class);
                if (!logging) {
                    startService(intent);
                } else {
                    stopService(intent);
                }
                logging = !logging;
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        // Check activity permissions only during the first launch
        checkActivityPermissions();
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Initialize the location manager
        if(permission) initLocation();

        // Load the old values from previous launches to avoid display errors and double logging
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        logging = settings.getBoolean("logging", false);
        format = settings.getInt("format", DMS);
        latitude = settings.getFloat("latitude", 0);
        longitude = settings.getFloat("longitude", 0);
        altitude = settings.getFloat("altitude", 0);
        time = settings.getLong("time", 0);
        provider = settings.getString("provider", "network");
        h_accuracy = settings.getFloat("h_accuracy", 0);
        speed = settings.getFloat("speed", 0);

        // Updates the display immediately
        updateDisplay();
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Deactivate the location manager if activated
        if (permission) {
            lm.removeUpdates(this);
        }

        // Save the current settings for later use
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("logging", logging);
        editor.putInt("format", format);
        editor.putFloat("latitude", (float) latitude);
        editor.putFloat("longitude", (float) longitude);
        editor.putFloat("altitude", (float) altitude);
        editor.putLong("time", time);
        editor.putString("provider", provider);
        editor.putFloat("h_accuracy", h_accuracy);
        editor.putFloat("speed", speed);
        editor.apply();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Stop logging service
        Intent intent = new Intent(context, LogPositionService.class);
        if (logging) {
            stopService(intent);
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        // Get new location values
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        altitude = location.getAltitude();
        h_accuracy = location.getAccuracy();
        speed = location.getSpeed();
        time = location.getTime();
        provider = location.getProvider();
        // Update display immediately
        updateDisplay();
    }

    @Override
    public void onProviderDisabled(String provider) {
        // Tell the user that his GPS is disabled
        String msg = String.format(
                getResources().getString(R.string.provider_disabled), provider);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        // Open the GPS setting activity
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    // Update display immediately
    private void updateDisplay() {
        String wgs84_txt = formatText();
        wgs84_res.setText(wgs84_txt);
    }

    // Format the text to display in the activity
    private String formatText() {
        String lat, lon, alt, acc, spe, dte;
        // Change location display format
        lat = Location.convert(latitude, format);
        lon = Location.convert(longitude, format);
        switch (format) {
            case DDD:
                lat += "°";
                lon += "°";
                break;
            case DMM:
                lat = lat.replaceFirst(":", "° ");
                lat += "'";
                lon = lon.replaceFirst(":", "° ");
                lon += "'";
                break;
            case DMS:
                lat = lat.replaceFirst(":", "° ");
                lat = lat.replaceFirst(":", "' ");
                lat += "\"";
                lon = lon.replaceFirst(":", "° ");
                lon = lon.replaceFirst(":", "' ");
                lon += "\"";
                break;
        }
        alt = String.valueOf(altitude);
        acc = String.valueOf(h_accuracy);
        spe = String.valueOf(speed);
        dte = new SimpleDateFormat("h:mm a dd.MM.yyyy").format(new Date(time));
        return String.format(getResources().getString(R.string.result), lat, lon, acc, alt, spe, dte, provider);
    }

    // Check activity's permissions to avoid errors
    private void checkActivityPermissions() {
        PackageManager pm = this.getPackageManager();
        int fine = pm.checkPermission(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                this.getPackageName());
        int coarse = pm.checkPermission(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                this.getPackageName());
        int sd_w = pm.checkPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE,
                this.getPackageName());
        int sd_r = pm.checkPermission(Manifest.permission.READ_EXTERNAL_STORAGE,
                this.getPackageName());
        if (fine != PackageManager.PERMISSION_GRANTED ||
                coarse != PackageManager.PERMISSION_GRANTED ||
                sd_r != PackageManager.PERMISSION_GRANTED ||
                sd_w != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 200);
        } else {
            // If the required permissions are granted, the app is rebooted
            permission = true;
            onResume();
        }
    }

    private void initLocation() {
        PackageManager pm = this.getPackageManager();
        int fine = pm.checkPermission(
                android.Manifest.permission.ACCESS_FINE_LOCATION,
                this.getPackageName());
        int coarse = pm.checkPermission(
                android.Manifest.permission.ACCESS_COARSE_LOCATION,
                this.getPackageName());
        if (fine == PackageManager.PERMISSION_GRANTED ||
                coarse == PackageManager.PERMISSION_GRANTED) {
            lm = (LocationManager) this.getSystemService(LOCATION_SERVICE);
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 2000, 0, this);
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 2000, 0, this);
            }
        }
    }

    // Triggered by the requestPermissions function, when the user has answered
    @Override
    public void onRequestPermissionsResult(int permsRequestCode, String[] permissions, int[] grantResults) {
        switch (permsRequestCode) {
            case 200:
                // While the required permission are not granted, the app keeps asking
                boolean fine = grantResults[0] == PackageManager.PERMISSION_GRANTED;
                boolean coarse = grantResults[1] == PackageManager.PERMISSION_GRANTED;
                boolean sd_r = grantResults[2] == PackageManager.PERMISSION_GRANTED;
                boolean sd_w = grantResults[3] == PackageManager.PERMISSION_GRANTED;
                if (!(fine && coarse && sd_r && sd_w)) {
                    ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION,
                            Manifest.permission.READ_EXTERNAL_STORAGE,
                            Manifest.permission.WRITE_EXTERNAL_STORAGE}, 200);
                } else {
                    // If the required permissions are granted, the app is rebooted
                    permission = true;
                    onResume();
                }
                break;
        }
    }
}
