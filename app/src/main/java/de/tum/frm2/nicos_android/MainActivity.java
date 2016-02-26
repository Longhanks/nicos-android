package de.tum.frm2.nicos_android;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        char[] password = {};
        ConnectionData conndata = new ConnectionData("172.25.2.7", 1301, "guest", password, false);
        NicosClient client = new NicosClient(new NicosHandler(this), conndata);
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
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

                case NicosClientMessages.BANNER_DICTIONARY_DATA:
                    HashMap dict = (HashMap) inputMessage.obj;
                    ArrayList<String> listKeys = new ArrayList<String>();
                    ArrayList<String> listValues = new ArrayList<String>();
                    for (Object key : dict.keySet()) {
                        listKeys.add((String) key);
                        listValues.add(dict.get(key).toString());
                    }
                    ListView listViewKeys = (ListView) content_main.findViewById(R.id.listViewKeys);
                    ListView listViewValues = (ListView) content_main.findViewById(R.id.listViewValues);
                    ArrayAdapter<String> adapterListKeys = new ArrayAdapter<String>(
                            mainActivity, android.R.layout.simple_list_item_1, listKeys);
                    ArrayAdapter<String> adapterListValues = new ArrayAdapter<String>(
                            mainActivity, android.R.layout.simple_list_item_1, listValues);
                    listViewKeys.setAdapter(adapterListKeys);
                    listViewValues.setAdapter(adapterListValues);
                    break;
            }
        }
    }
}
