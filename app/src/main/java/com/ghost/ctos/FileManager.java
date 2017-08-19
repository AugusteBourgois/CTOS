package com.ghost.ctos;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static com.ghost.ctos.MainActivity.PATH;

public class FileManager extends AppCompatActivity {

    private Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_file_manager);
        context=this;
    }

    @Override
    protected void onResume(){
        super.onResume();

        // Read all files sorted into the values-array
        List<String> values = new ArrayList<>();
        File dir = Environment.getExternalStoragePublicDirectory(PATH);
        String[] list = dir.list();
        if (list != null) {
            for (String file : list) {
                if (!file.startsWith(".") && file.endsWith(".gpx")) {
                    values.add(file);
                }
            }
        }
        Collections.sort(values);

        // Put the data into the list
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                android.R.layout.simple_list_item_1, android.R.id.text1, values);

        ListView listview = (ListView) findViewById(R.id.list);
        if(listview!=null) listview.setAdapter(adapter);
        if(listview!=null) listview.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                File dir = Environment.getExternalStoragePublicDirectory(PATH);
                String path = dir.getPath();
                String filename = (String) parent.getItemAtPosition(position);
                if (path.endsWith(File.separator)) {
                    filename = path + filename;
                } else {
                    filename = path + File.separator + filename;
                }
                if (new File(filename).isFile()) {
                    final String new_filename = filename;
                    AlertDialog.Builder builderSingle = new AlertDialog.Builder(context);

                    final ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(context, android.R.layout.select_dialog_singlechoice);
                    arrayAdapter.add(getResources().getString(R.string.dialog_share));
                    arrayAdapter.add(getResources().getString(R.string.dialog_display));
                    arrayAdapter.add(getResources().getString(R.string.dialog_delete));

                    builderSingle.setNegativeButton(getResources().getString(R.string.dialog_cancel), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });

                    builderSingle.setAdapter(arrayAdapter, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which){
                                case 0:
                                    Intent sendIntent = new Intent();
                                    sendIntent.setAction(Intent.ACTION_SEND);
                                    sendIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(new File(new_filename)));
                                    sendIntent.setType("text/plain");
                                    sendIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(Intent.createChooser(sendIntent, getResources().getText(R.string.send_file_to)));
                                    break;
                                case 1:
                                    Intent displayIntent = new Intent();
                                    displayIntent.setAction(Intent.ACTION_VIEW);
                                    displayIntent.setDataAndType(Uri.fromFile(new File(new_filename)),"text/plain");
                                    displayIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                                    startActivity(Intent.createChooser(displayIntent, getResources().getText(R.string.display_file)));
                                    break;
                                case 2:
                                    File fdelete = new File(new_filename);
                                    if (fdelete.exists()) {
                                        boolean del = fdelete.delete();
                                        if(del){
                                            Toast.makeText(context,getResources().getText(R.string.delete_file_ok),Toast.LENGTH_SHORT).show();
                                        }else{
                                            Toast.makeText(context,getResources().getText(R.string.delete_file_nok),Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                    onResume();
                                    break;
                            }
                        }
                    });
                    builderSingle.show();
                }
            }
        });
    }
}
