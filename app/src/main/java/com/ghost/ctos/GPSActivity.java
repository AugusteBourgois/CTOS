package com.ghost.ctos;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import java.sql.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static com.ghost.ctos.MainActivity.PREFS_NAME;

public class GPSActivity extends AppCompatActivity implements LocationListener {

    private LocationManager lm;

    private double latitude;
    private double longitude;
    private double altitude;
    private long time;
    private float h_accuracy;
    private float speed;
    private int time_rate;
    private int dist_rate;

    private int format;
    private int unit;
    private int update;
    private Context context;

    public static final int U_TIME = 0;
    public static final int U_DIST = 1;
    public static final int DDD = Location.FORMAT_DEGREES;
    public static final int DMM = Location.FORMAT_MINUTES;
    public static final int DMS = Location.FORMAT_SECONDS;
    public static final int MS = 0;
    public static final int KMH = 1;
    public static final int MPH = 2;
    public static final int NMPH = 3;
    public static final double KMH_U = 3.6;
    public static final double MPH_U = 3600/1609.344;
    public static final double NMPH_U = 3600/1852;

    TextView textview_result;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Set the layout of the activity
        setContentView(R.layout.activity_gps);

        // Load the text display
        textview_result = (TextView) findViewById(R.id.textview_result);
        Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(myToolbar);
        // Get a support ActionBar corresponding to this toolbar
        ActionBar ab = getSupportActionBar();
        // Enable the Up button
        if(ab!=null) ab.setDisplayHomeAsUpEnabled(true);

        // Store the context for later use in objects
        context = this;

        // Implement the second button to open the tracks directory
        final Button button1 = (Button) findViewById(R.id.button_open);
        if(button1!=null) button1.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                Intent intent = new Intent(context, FileManager.class);
                startActivity(intent);
            }
        });

        // Implement the second button to share location
        final Button button2 = (Button) findViewById(R.id.button_share);
        if(button2!=null) button2.setOnClickListener(new View.OnClickListener() {
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
        if(button3!=null) button3.setText(getResources().getString(R.string.button_log_on));
        if(button3!=null) button3.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Start or stop logging service depending on the latter's state
                SharedPreferences settings = context.getSharedPreferences(PREFS_NAME, GPSActivity.MODE_PRIVATE);
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
    protected void onResume() {
        super.onResume();

        // Load the old values from previous launches to avoid display errors and double logging
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, GPSActivity.MODE_PRIVATE);
        boolean logging = settings.getBoolean("logging",false);
        format = settings.getInt("format", DMS);
        unit = settings.getInt("unit", KMH);
        latitude = settings.getFloat("latitude", 0);
        longitude = settings.getFloat("longitude", 0);
        altitude = settings.getFloat("altitude", 0);
        time = settings.getLong("time", 0);
        h_accuracy = settings.getFloat("h_accuracy", 0);
        speed = settings.getFloat("speed", 0);
        update = settings.getInt("update",0);
        dist_rate = settings.getInt("dist_rate",5);
        time_rate = settings.getInt("time_rate",2);

        Button button3 = (Button)findViewById(R.id.button_log);
        if(logging){
            if(button3!=null) button3.setText(getResources().getString(R.string.button_log_off));
        }else{
            if(button3!=null) button3.setText(getResources().getString(R.string.button_log_on));
        }

        // Initialize the location manager
        initLocation();

        // Update display immediately
        updateDisplay();

    }

    @Override
    protected void onStop() {
        super.onStop();
        // Deactivate the location manager if activated
        lm.removeUpdates(this);

        // Save the current settings for later use
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, GPSActivity.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putFloat("latitude", (float) latitude);
        editor.putFloat("longitude", (float) longitude);
        editor.putFloat("altitude", (float) altitude);
        editor.putLong("time", time);
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_gps, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, OptionActivity.class);
                startActivity(intent);
                break;

            default:
                // If we got here, the user's action was not recognized.
                // Invoke the superclass to handle it.
                return super.onOptionsItemSelected(item);

        }
        return true;
    }

    // Update display immediately
    private void updateDisplay() {
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
                spe = String.format(Locale.getDefault(),"%.2f",speed)+" m/s";
                break;
            case KMH:
                spe = String.format(Locale.getDefault(),"%.2f",speed*KMH_U)+" km/h";
                break;
            case MPH:
                spe = String.format(Locale.getDefault(),"%.2f",speed*MPH_U)+" mph";
                break;
            case NMPH:
                spe = String.format(Locale.getDefault(),"%.2f",speed*NMPH_U)+" knots";
                break;
        }
        alt = String.valueOf(altitude)+" m";
        acc = String.valueOf(h_accuracy)+" m";
        dte = new SimpleDateFormat("h:mm a dd.MM.yyyy",Locale.getDefault()).format(new Date(time));
        String result = String.format(getResources().getString(R.string.textview_result), lat, lon, acc, alt, spe, dte);
        textview_result.setText(result);
    }

    private void initLocation() {
        boolean fine = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (fine) {
            lm = (LocationManager) this.getSystemService(LOCATION_SERVICE);
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                switch (update){
                    case U_TIME:
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, time_rate*1000, 0, this);
                        // Tell the user that his GPS is disabled
                        String msg1 = String.format(getResources().getString(R.string.launch_gps),Integer.toString(time_rate)+" seconds");
                        Toast.makeText(this, msg1, Toast.LENGTH_SHORT).show();
                        break;
                    case U_DIST:
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, dist_rate, this);
                        // Tell the user that his GPS is disabled
                        String msg2 = String.format(getResources().getString(R.string.launch_gps),Integer.toString(dist_rate)+" meters");
                        Toast.makeText(this, msg2, Toast.LENGTH_SHORT).show();
                        break;
                }
            }else{
                this.onProviderDisabled(LocationManager.GPS_PROVIDER);
            }
        }
    }
}
