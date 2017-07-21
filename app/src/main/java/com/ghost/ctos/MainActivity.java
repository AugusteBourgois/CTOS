package com.ghost.ctos;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.content.Context;
import android.view.View;
import android.widget.Button;

public class MainActivity extends Activity {

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Store the context for later use in objects
        context = this;
        createButton(R.id.button_gps, GPSActivity.class);
        createButton(R.id.button_vr, VRActivity.class);
    }

    private void createButton(int buttonId, final Class activity) {
        // Implement the third button to log locations
        final Button button = (Button) findViewById(buttonId);
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // Start or stop logging service depending on the latter's state
                Intent intent = new Intent(context, activity);
                startActivity(intent);
            }
        });
    }
}
