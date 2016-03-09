package de.tum.frm2.nicos_android;

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
import android.widget.TextView;


public class MainActivity extends AppCompatActivity implements NicosCallbackHandler {
    public final static String MESSAGE_DAEMON_INFO =
            "de.tum.frm2.nicos_android.MESSAGE_DAEMON_INFO";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        TextView textView = (TextView) findViewById(R.id.textView);

        ConnectionData connData = (ConnectionData) getIntent().getSerializableExtra(
                LoginActivity.MESSAGE_CONNECTION_DATA);
        String connectionString = String.format("Connected to: %s@%s, user_level: %s",
                connData.getUser(),
                connData.getHost(),
                String.valueOf(NicosClient.getClient().getUserLevel()));
        textView.setText(connectionString);

        NicosClient.getClient().registerCallbackHandler(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                // Demo for querying and printing all moveable devices.
                System.out.println(NicosClient.getClient().getDeviceList(
                        "nicos.core.device.Moveable", true, null, null));
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
        // Should at one point also implement GUI display of events.
    }
}
