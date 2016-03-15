package de.tum.frm2.nicos_android.gui;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.GridView;
import android.widget.ListView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

import de.tum.frm2.nicos_android.nicos.ConnectionData;
import de.tum.frm2.nicos_android.nicos.Device;
import de.tum.frm2.nicos_android.util.NicosCallbackHandler;
import de.tum.frm2.nicos_android.nicos.NicosClient;
import de.tum.frm2.nicos_android.R;
import de.tum.frm2.nicos_android.util.TupleOfTwo;


public class MainActivity extends AppCompatActivity implements NicosCallbackHandler {
    public final static String MESSAGE_DAEMON_INFO =
            "de.tum.frm2.nicos_android.MESSAGE_DAEMON_INFO";
    private ArrayList<Device> _moveables;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        _moveables = new ArrayList<Device>();

        ConnectionData connData = (ConnectionData) getIntent().getSerializableExtra(
                LoginActivity.MESSAGE_CONNECTION_DATA);

        NicosClient.getClient().registerCallbackHandler(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                on_client_connected();
            }
        }).start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            AlertDialog alertDialog = new AlertDialog.Builder(MainActivity.this).create();
            alertDialog.setTitle("Not yet implemented");
            alertDialog.setMessage("The settings dialog wasn't implemented yet.");
            alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "Okay",
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            int version = Build.VERSION.SDK_INT;
            int color;
            if (version >= 23) {
                color = ContextCompat.getColor(this, R.color.colorPrimary);
            }
            else {
                color = getResources().getColor(R.color.colorPrimary);
            }
            alertDialog.show();
            alertDialog.getButton(alertDialog.BUTTON_NEUTRAL).setTextColor(color);
            return true;
        }

        if (id == R.id.action_connection_info) {
            Intent intent = new Intent(this, ConnectionInfoActivity.class);
            intent.putExtra(MESSAGE_DAEMON_INFO, NicosClient.getClient().getNicosBanner());
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        NicosClient.getClient().unregisterCallbackHandler(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                NicosClient.getClient().disconnect();
            }
        }).start();
        super.onBackPressed();
    }

    @Override
    public void handleSignal(String signal, Object data, Object args) {
        if (signal.equals("broken")) {
            final String error = (String) data;
            // Connection is broken. Try to disconnect what's left and go back to login screen.
            NicosClient.getClient().unregisterCallbackHandler(this);
            NicosClient.getClient().disconnect();
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    try {
                        AlertDialog alertDialog =
                                new AlertDialog.Builder(MainActivity.this).create();
                        alertDialog.setTitle("Disconnected");
                        alertDialog.setMessage(error);
                        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Okay",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        finish();
                                    }
                                });
                        alertDialog.show();
                    }
                    catch (Exception e) {
                        try {
                            finish();
                        }
                        catch (Exception e2) {
                            // User probably quit the application.
                        }
                        // Activity isn't running anymore
                        // (user probably put application in background).
                    }
                }
            });
        }
    }

    private void on_client_connected() {
        // Query moveables.
        ArrayList<String> lowercaseMoveables =
                (ArrayList<String>) NicosClient.getClient().getDeviceList(
                        "nicos.core.device.Moveable", true, null, null);

        // Ask for current status.
        Object untyped_state = NicosClient.getClient().ask("getstatus", null);
        if (untyped_state == null) {
            return;
        }
        HashMap<String, Object> state = (HashMap<String, Object>) untyped_state;

        // Extract device list from status. We need this for the device's "real" name (with upper
        // case letters).
        ArrayList<String> devlist = (ArrayList<String>) state.get("devices");
        for (String device : devlist) {
            String cachekey = device.toLowerCase();
            if (lowercaseMoveables.contains(cachekey)) {
                Device moveable = new Device(device, cachekey);
                Object untypedStatus = NicosClient.getClient().getDeviceStatus(device);
                Object[] tupleStatus;
                if (untypedStatus == null) {
                    tupleStatus = new Object[] {-1, null};
                }
                else {
                    tupleStatus = (Object[]) untypedStatus;
                }
                moveable.setStatus((int) tupleStatus[0]);
                moveable.setValue(NicosClient.getClient().getDeviceValue(device));
                _moveables.add(moveable);
            }
        }
        Collections.sort(_moveables, new Comparator<Device>() {
            @Override
            public int compare(Device lhs, Device rhs) {
                return lhs.getCacheName().compareTo(rhs.getCacheName());
            }
        });

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // Fill list with devices.
                View content_main = findViewById(R.id.content_main);
                DeviceViewAdapter adapter = new DeviceViewAdapter(MainActivity.this,
                        _moveables.toArray(new Device[_moveables.size()]));
                ListView deviceListView = (ListView) content_main.findViewById(R.id.deviceListView);
                deviceListView.setAdapter(adapter);
            }
        });
    }
}
