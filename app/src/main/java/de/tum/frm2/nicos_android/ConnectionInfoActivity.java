package de.tum.frm2.nicos_android;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ListView;
import android.widget.SimpleAdapter;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ConnectionInfoActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_connection_info);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        HashMap connectionInfo = (HashMap) getIntent().getSerializableExtra(
                MainActivity.MESSAGE_CONNECTION_INFO);

        // Android quirks...
        List<Map<String, String>> data = new ArrayList<Map<String, String>>();
        for (Object key : connectionInfo.keySet()) {
            Map<String, String> subdata = new HashMap<String, String>();
            subdata.put("FIRST_LINE", key.toString());
            subdata.put("SECOND_LINE", connectionInfo.get(key).toString());
            data.add(subdata);
        }
        ListView listView = (ListView) findViewById(R.id.listView);
        SimpleAdapter adapter = new SimpleAdapter(
                this,
                data,
                android.R.layout.simple_list_item_2,
                new String[] {"FIRST_LINE", "SECOND_LINE"},
                new int[] {android.R.id.text1, android.R.id.text2});
        listView.setAdapter(adapter);
    }

}
