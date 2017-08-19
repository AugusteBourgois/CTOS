package com.ghost.ctos;


import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.text.SimpleDateFormat;
import java.util.Locale;

import static com.ghost.ctos.GPSActivity.U_DIST;
import static com.ghost.ctos.GPSActivity.U_TIME;
import static com.ghost.ctos.MainActivity.PATH;
import static com.ghost.ctos.MainActivity.PREFS_NAME;

public class LogPositionService extends Service implements LocationListener {

    private NotificationManager mNM;
    private LocationManager lm;
    private int NOTIFICATION = R.string.service_start;
    NotificationCompat.Builder notification;

    private double latitude;
    private double longitude;
    private double altitude;
    private long time;
    private float speed;
    private String filename;
    private int number = 0;

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        this.startLoggingService();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i("LogPositionService", "Received start id " + startId + ": " + intent);
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        lm.removeUpdates(this);
        closeGPXFile();

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, LogPositionService.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putBoolean("logging",false);
        editor.apply();

        stopForeground(true);

        // Tell the user we stopped.
        Toast.makeText(this, R.string.service_stop, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onLocationChanged(Location location) {
        latitude = location.getLatitude();
        longitude = location.getLongitude();
        altitude = location.getAltitude();
        speed = location.getSpeed();
        time = location.getTime();
        writeGPX(formatGPX());
        // Update the number of logged positions
        notification.setNumber(++number);
        Notification not = notification.build();
        not.flags = Notification.FLAG_ONGOING_EVENT;
        // Send the notification.
        mNM.notify(NOTIFICATION, not);
    }

    @Override
    public void onProviderDisabled(String provider) {
        String msg = getResources().getString(R.string.error_provider_disabled);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    private void startLoggingService(){
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, LogPositionService.MODE_PRIVATE);
        int update = settings.getInt("update",0);
        int dist_rate = settings.getInt("dist_rate",5);
        int time_rate = settings.getInt("time_rate",2);
        boolean fine = ContextCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!fine) {
            Toast.makeText(this, R.string.error_permission_localisation, Toast.LENGTH_SHORT).show();
            this.stopSelf();
        }else if(!isSDW() || !isSDR()){
            Toast.makeText(this, R.string.error_availability_storage, Toast.LENGTH_SHORT).show();
            this.stopSelf();
        }else{
            lm = (LocationManager) this.getSystemService(LOCATION_SERVICE);
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                switch (update){
                    case U_TIME:
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, time_rate*1000, 0, this);
                        break;
                    case U_DIST:
                        lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, dist_rate, this);
                        break;
                }
            }
            showNotification();
            createGPXFile();
            SharedPreferences.Editor editor = settings.edit();
            editor.putBoolean("logging",true);
            editor.apply();
        }
    }

    private void showNotification() {
        Toast.makeText(this, R.string.service_start, Toast.LENGTH_SHORT).show();
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.service_start);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        notification = new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.pin)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.logging_service))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setNumber(number)
                .setContentIntent(contentIntent);  // The intent to send when the entry is clicked
        Notification not = notification.build();
        not.flags = Notification.FLAG_ONGOING_EVENT;

        // Send the notification.
        startForeground(NOTIFICATION, not);
    }

    private String formatGPX(){
        String lat, lon, alt, tm, spd;
        lat = String.valueOf(latitude);
        lon = String.valueOf(longitude);
        alt = String.valueOf(altitude);
        tm = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",Locale.getDefault()).format(new Date(time));
        spd = String.valueOf(speed);
        return String.format(getResources().getString(R.string.gpx_trkpt),lon,lat,alt,tm,spd);
    }

    private void writeGPX(String data){
        File file;
        FileOutputStream outputStream;
        try {
            file = new File(Environment.getExternalStoragePublicDirectory(PATH), filename);
            outputStream = new FileOutputStream(file, true);
            outputStream.write(data.getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createGPXFile(){
        String currentDateTimeString = new SimpleDateFormat("yyyy-MM-dd'_'HH:mm:ss", Locale.getDefault()).format(new Date());
        filename = getResources().getString(R.string.filename)+currentDateTimeString+".gpx";
        File file;
        FileOutputStream outputStream;
        try {
            file = new File(Environment.getExternalStoragePublicDirectory(PATH), filename);
            outputStream = new FileOutputStream(file, true);
            outputStream.write(getResources().getString(R.string.gpx_header).getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void closeGPXFile(){
        File file;
        FileOutputStream outputStream;
        try {
            file = new File(Environment.getExternalStoragePublicDirectory(PATH), filename);
            outputStream = new FileOutputStream(file, true);
            outputStream.write(getResources().getString(R.string.gpx_footer).getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* Checks if external storage is available for read and write */
    private boolean isSDW() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }

    /* Checks if external storage is available to at least read */
    private boolean isSDR() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state) ||
                Environment.MEDIA_MOUNTED_READ_ONLY.equals(state);
    }

}
