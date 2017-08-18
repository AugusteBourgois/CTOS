package com.ghost.ctos;

import android.Manifest;
import android.app.Activity;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Date;
import java.text.SimpleDateFormat;


public class GPSActivity extends Activity implements LocationListener {

    private LocationManager lm;

    private double latitude;
    private double longitude;
    private double altitude;
    private long time;
    private String provider;
    private float h_accuracy;
    private float speed;

    private boolean permission;
    private int format;
    private int unit;
    private Context context;

    private static final String PREFS_NAME = "ctos_preference";
    private static final int DDD = Location.FORMAT_DEGREES;
    private static final int DMM = Location.FORMAT_MINUTES;
    private static final int DMS = Location.FORMAT_SECONDS;

    private static final int MS = 0;
    private static final int KMH = 1;
    private static final int MPH = 2;
    private static final int NMPH = 3;
    private static final double KMH_U = 3.6;
    private static final double MPH_U = 3600/1609.344;
    private static final double NMPH_U = 3600/1852;

    TextView textview_result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout of the activity
        setContentView(R.layout.activity_gps);

        // Load the text display
        textview_result = (TextView) findViewById(R.id.textview_result);

        // Store the context for later use in objects
        context = this;

        // Implement the second button to open the tracks directory
        final Button button1 = (Button) findViewById(R.id.button_open);
        button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Uri selectedUri = Uri.parse(Environment.DIRECTORY_DOCUMENTS);
                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(selectedUri, "resource/*");
                if (intent.resolveActivityInfo(getPackageManager(), 0) != null){
                    startActivity(intent);
                }else{
                    String msg = getResources().getString(R.string.error_file_explorer);
                    Toast.makeText(context, msg, Toast.LENGTH_SHORT).show();
                }
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
        final Button button3 = (Button) findViewById(R.id.button_log);
        button3.setText(getResources().getString(R.string.button_log_on));
        button3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Start or stop logging service depending on the latter's state
                SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
                boolean logging = settings.getBoolean("logging",false);
                Intent intent = new Intent(context, LogPositionService.class);
                if(logging){
                    button3.setText(getResources().getString(R.string.button_log_on));
                    stopService(intent);
                }else{
                    button3.setText(getResources().getString(R.string.button_log_off));
                    startService(intent);
                }
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
        boolean logging = settings.getBoolean("logging",false);
        format = settings.getInt("format", DMS);
        unit = settings.getInt("unit", KMH);
        latitude = settings.getFloat("latitude", 0);
        longitude = settings.getFloat("longitude", 0);
        altitude = settings.getFloat("altitude", 0);
        time = settings.getLong("time", 0);
        provider = settings.getString("provider", "network");
        h_accuracy = settings.getFloat("h_accuracy", 0);
        speed = settings.getFloat("speed", 0);

        // Updates the display immediately
        updateDisplay();
        Button button3 = (Button)findViewById(R.id.button_log);
        if(logging){
            button3.setText(getResources().getString(R.string.button_log_off));
        }else{
            button3.setText(getResources().getString(R.string.button_log_on));
        }

        RadioGroup g1 = (RadioGroup)findViewById(R.id.radioGroup_coord);
        switch(format){
            case DDD:
                g1.check(R.id.lat_lon_ddd);
                break;
            case DMM:
                g1.check(R.id.lat_lon_dmm);
                break;
            case DMS:
                g1.check(R.id.lat_lon_dms);
                break;
        }

        RadioGroup g2 = (RadioGroup)findViewById(R.id.radioGroup_speed);
        switch(format){
            case MS:
                g1.check(R.id.speed_ms);
                break;
            case KMH:
                g1.check(R.id.speed_kmh);
                break;
            case MPH:
                g1.check(R.id.speed_mph);
                break;
            case NMPH:
                g1.check(R.id.speed_nmph);
                break;
        }
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
        editor.putInt("format", format);
        editor.putInt("unit", unit);
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
        String msg = getResources().getString(R.string.error_provider_disabled);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        // Open the GPS setting activity
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {
    }

    public void onSwitchLatLon(View view){
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Switch between the 3 display modes
        switch(view.getId()) {
            case R.id.lat_lon_ddd:
                if (checked)
                    format =DDD;
                break;
            case R.id.lat_lon_dmm:
                if (checked)
                    format =DMM;
                break;
            case R.id.lat_lon_dms:
                if (checked)
                    format =DMS;
                break;
        }
        // Update immediately the display
        updateDisplay();
    }

    public void onSwitchSpeed(View view){
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();

        // Check which radio button was clicked
        switch(view.getId()) {
            case R.id.speed_ms:
                if (checked)
                    unit = MS;
                break;
            case R.id.speed_kmh:
                if (checked)
                    unit = KMH;
                break;
            case R.id.speed_mph:
                if (checked)
                    unit = MPH;
                break;
            case R.id.speed_nmph:
                if (checked)
                    unit = NMPH;
                break;
        }
        // Update immediately the display
        updateDisplay();
    }

    // Update display immediately
    private void updateDisplay() {
        String text = formatText();
        textview_result.setText(text);
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
        spe="";
        switch (unit) {
            case MS:
                spe = String.valueOf(speed)+" m/s";
                break;
            case KMH:
                spe = String.valueOf(speed*KMH_U)+" km/h";
                break;
            case MPH:
                spe = String.valueOf(speed*MPH_U)+" mph";
                break;
            case NMPH:
                spe = String.valueOf(speed*NMPH_U)+" knots";
                break;
        }
        alt = String.valueOf(altitude)+" m";
        acc = String.valueOf(h_accuracy)+" m";
        dte = new SimpleDateFormat("h:mm a dd.MM.yyyy").format(new Date(time));
        return String.format(getResources().getString(R.string.textview_result), lat, lon, acc, alt, spe, dte, provider);
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
            }else{
                this.onProviderDisabled(LocationManager.GPS_PROVIDER);
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
