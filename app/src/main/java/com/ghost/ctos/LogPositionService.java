package com.ghost.ctos;


import android.Manifest;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;
import java.text.DateFormat;
import java.text.SimpleDateFormat;

public class LogPositionService extends Service implements LocationListener {

    private NotificationManager mNM;
    private LocationManager lm;
    private int NOTIFICATION = R.string.service_start;
    private boolean permission;

    private double latitude;
    private double longitude;
    private double altitude;
    private long time;
    private float speed;
    private String filename;

    @Override
    public void onCreate() {
        mNM = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
        if(!permission) {
            this.checkServicePermissions();
        }
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
        // Cancel the persistent notification.
        mNM.cancel(NOTIFICATION);

        if(permission){
            lm.removeUpdates(this);
            closeGPXFile();
        }

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
    }

    @Override
    public void onProviderDisabled(String provider) {
        String msg = String.format(
                getResources().getString(R.string.provider_disabled), provider);
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
        Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
        startActivity(intent);
    }

    @Override
    public void onProviderEnabled(String provider) {}

    @Override
    public void onStatusChanged(String provider, int status, Bundle extras) {}

    private void showNotification() {
        Toast.makeText(this, R.string.service_start, Toast.LENGTH_SHORT).show();
        // In this sample, we'll use the same text for the ticker and the expanded notification
        CharSequence text = getText(R.string.service_start);

        // The PendingIntent to launch our activity if the user selects this notification
        PendingIntent contentIntent = PendingIntent.getActivity(this, 0,
                new Intent(this, MainActivity.class), 0);

        // Set the info for the views that show in the notification panel.
        Notification notification = new Notification.Builder(this)
                .setSmallIcon(R.drawable.pin)  // the status icon
                .setTicker(text)  // the status text
                .setWhen(System.currentTimeMillis())  // the time stamp
                .setContentTitle(getText(R.string.logging_service))  // the label of the entry
                .setContentText(text)  // the contents of the entry
                .setContentIntent(contentIntent)  // The intent to send when the entry is clicked
                .build();

        // Send the notification.
        mNM.notify(NOTIFICATION, notification);
    }

    private void checkServicePermissions(){
        if(permission){
            lm = (LocationManager) this.getSystemService(LOCATION_SERVICE);
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER) &&
                    lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 60000, 0, this);
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 60000, 0, this);
            }
            showNotification();
            createGPXFile();
        }else{
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
                    coarse != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.error_permission_localisation, Toast.LENGTH_SHORT).show();
                permission = false;
                this.stopSelf();
            }else if(sd_r != PackageManager.PERMISSION_GRANTED ||
                    sd_w != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.error_permission_storage, Toast.LENGTH_SHORT).show();
                permission = false;
                this.stopSelf();
            }else if(!isSDW() || !isSDR()){
                Toast.makeText(this, R.string.error_availability_storage, Toast.LENGTH_SHORT).show();
                permission = false;
                this.stopSelf();
            }else{
                permission = true;
                checkServicePermissions();
            }
        }
    }

    private String formatGPX(){
        String lat, lon, alt, tm, spd;
        lat = String.valueOf(latitude);
        lon = String.valueOf(longitude);
        alt = String.valueOf(altitude);
        tm = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'").format(new Date(time));
        spd = String.valueOf(speed);
        return String.format(getResources().getString(R.string.gpx_trkpt),lon,lat,alt,tm,spd);
    }

    private void writeGPX(String data){
        File file;
        FileOutputStream outputStream;
        try {
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), filename);
            outputStream = new FileOutputStream(file, true);
            outputStream.write(data.getBytes());
            outputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void createGPXFile(){
        String currentDateTimeString = DateFormat.getDateTimeInstance().format(new Date());
        filename = getResources().getString(R.string.filename)+currentDateTimeString+".gpx";
        File file;
        FileOutputStream outputStream;
        try {
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), filename);
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
            file = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOCUMENTS), filename);
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
