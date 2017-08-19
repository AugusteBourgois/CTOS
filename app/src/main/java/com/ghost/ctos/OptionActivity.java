package com.ghost.ctos;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import java.util.Locale;

import static com.ghost.ctos.GPSActivity.DDD;
import static com.ghost.ctos.GPSActivity.DMM;
import static com.ghost.ctos.GPSActivity.DMS;
import static com.ghost.ctos.GPSActivity.KMH;
import static com.ghost.ctos.GPSActivity.MPH;
import static com.ghost.ctos.GPSActivity.MS;
import static com.ghost.ctos.GPSActivity.NMPH;
import static com.ghost.ctos.GPSActivity.U_DIST;
import static com.ghost.ctos.GPSActivity.U_TIME;
import static com.ghost.ctos.MainActivity.PREFS_NAME;

public class OptionActivity extends AppCompatActivity {

    private int format;
    private int unit;
    private int update;
    private int time_rate;
    private int dist_rate;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_option);

        final EditText rate_time   = (EditText)findViewById(R.id.rate_time);
        final EditText rate_dist   = (EditText)findViewById(R.id.rate_dist);
        if(rate_time!=null) rate_time.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = rate_time.getText().toString();
                if(!text.equals("")) {
                    time_rate = Integer.parseInt(text);
                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, OptionActivity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putInt("time_rate", time_rate);
                    editor.apply();
                }
            }
        });

        if(rate_dist!=null) rate_dist.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                String text = rate_dist.getText().toString();
                if(!text.equals("")) {
                    dist_rate = Integer.parseInt(text);
                    SharedPreferences settings = getSharedPreferences(PREFS_NAME, OptionActivity.MODE_PRIVATE);
                    SharedPreferences.Editor editor = settings.edit();
                    editor.putInt("dist_rate", dist_rate);
                    editor.apply();
                }
            }
        });
    }

    @Override
    protected void onResume(){
        super.onResume();

        SharedPreferences settings = getSharedPreferences(PREFS_NAME, OptionActivity.MODE_PRIVATE);
        format = settings.getInt("format", DMS);
        unit = settings.getInt("unit", KMH);
        update = settings.getInt("update",0);
        dist_rate = settings.getInt("dist_rate",5);
        time_rate = settings.getInt("time_rate",2);

        EditText rate_time   = (EditText)findViewById(R.id.rate_time);
        if(rate_time!=null) rate_time.setText(String.format(Locale.getDefault(),"%d",time_rate));

        EditText rate_dist   = (EditText)findViewById(R.id.rate_dist);
        if(rate_dist!=null) rate_dist.setText(String.format(Locale.getDefault(),"%d",dist_rate));

        RadioGroup g1 = (RadioGroup)findViewById(R.id.radioGroup_coord);
        if (g1!=null) switch(format){
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
        if(g2!=null) switch(unit){
            case MS:
                g2.check(R.id.speed_ms);
                break;
            case KMH:
                g2.check(R.id.speed_kmh);
                break;
            case MPH:
                g2.check(R.id.speed_mph);
                break;
            case NMPH:
                g2.check(R.id.speed_nmph);
                break;
        }

        RadioGroup g3 = (RadioGroup)findViewById(R.id.radioGroup_update);
        if(g3!=null) switch(update){
            case U_TIME:
                g3.check(R.id.rdb_time);
                if(rate_time!=null) rate_time.setEnabled(true);
                if(rate_dist!=null) rate_dist.setEnabled(false);
                break;
            case U_DIST:
                g3.check(R.id.rdb_dist);
                if(rate_time!=null) rate_time.setEnabled(false);
                if(rate_dist!=null) rate_dist.setEnabled(true);
                break;
        }
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
        // Save the current settings for later use
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, OptionActivity.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("format", format);
        editor.apply();
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
        // Save the current settings for later use
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, OptionActivity.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("unit", unit);
        editor.apply();
    }

    public void onSwitchUpdate(View view){
        // Is the button now checked?
        boolean checked = ((RadioButton) view).isChecked();
        EditText rate_time   = (EditText)findViewById(R.id.rate_time);
        EditText rate_dist   = (EditText)findViewById(R.id.rate_dist);

        // Switch between the 3 display modes
        switch(view.getId()) {
            case R.id.rdb_time:
                if (checked) {
                    update = U_TIME;
                    if (rate_time != null) rate_time.setEnabled(true);
                    if (rate_dist != null) rate_dist.setEnabled(false);
                }
                break;
            case R.id.rdb_dist:
                if (checked) {
                    update = U_DIST;
                    if (rate_time != null) rate_time.setEnabled(false);
                    if (rate_dist != null) rate_dist.setEnabled(true);
                }
                break;
        }
        // Save the current settings for later use
        SharedPreferences settings = getSharedPreferences(PREFS_NAME, OptionActivity.MODE_PRIVATE);
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt("update", update);
        editor.apply();
    }
}
