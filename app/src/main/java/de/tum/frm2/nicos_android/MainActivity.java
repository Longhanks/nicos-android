package de.tum.frm2.nicos_android;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {
    public final static String MESSAGE_CONNECTION_INFO =
            "de.tum.frm2.nicos_android.MESSAGE_CONNECTION_INFO";
    private NicosClient client;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        char[] password = {};
        ConnectionData conndata = new ConnectionData("172.25.2.7", 1301, "guest", password, false);
        client = new NicosClient(new NicosHandler(this), conndata);
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
            intent.putExtra(MESSAGE_CONNECTION_INFO, client.getNicosBanner());
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private static class NicosHandler extends Handler {
        private final WeakReference<MainActivity> mainActivityRef;

        NicosHandler(MainActivity mainActivity) {
            this.mainActivityRef = new WeakReference<MainActivity>(mainActivity);
        }

        @Override
        public void handleMessage(Message inputMessage) {
            MainActivity mainActivity = mainActivityRef.get();
            if (mainActivity == null) {
                return;
            }

            View content_main = mainActivity.findViewById(R.id.content_main);
            switch (inputMessage.what) {
                case NicosClientMessages.CONNECTION_SUCCESSFUL:
                    TextView textView = (TextView) content_main.findViewById(R.id.textView);
                    textView.setText((String) inputMessage.obj);
                    break;
            }
        }
    }
}
