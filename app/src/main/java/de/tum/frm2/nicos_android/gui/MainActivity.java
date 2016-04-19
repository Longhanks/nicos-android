package de.tum.frm2.nicos_android.gui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import net.razorvine.pickle.objects.ClassDict;
import net.razorvine.pickle.objects.ClassDictConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import de.tum.frm2.nicos_android.nicos.ConnectionData;
import de.tum.frm2.nicos_android.nicos.Device;
import de.tum.frm2.nicos_android.nicos.DeviceStatus;
import de.tum.frm2.nicos_android.nicos.NicosStatus;
import de.tum.frm2.nicos_android.util.NicosCallbackHandler;
import de.tum.frm2.nicos_android.nicos.NicosClient;
import de.tum.frm2.nicos_android.R;


public class MainActivity extends AppCompatActivity implements NicosCallbackHandler {
    public final static String MESSAGE_DAEMON_INFO =
            "de.tum.frm2.nicos_android.MESSAGE_DAEMON_INFO";
    private ArrayList<Device> _moveables;
    private DeviceViewAdapter _devicesAdapter;
    private Handler _uiThread;
    private boolean _visible;
    private boolean _canAccessDevices;
    private int _current_status;
    private SlidingUpPanelLayout _slidingUpPanelLayout;
    private TextView _currentDeviceTextView;
    private TextView _currentDeviceValueTextView;
    private ImageView _currentDeviceStatusImageView;
    private Button _coarseStepLeftButton;
    private Button _fineStepLeftButton;
    private Button _stopButton;
    private Button _fineStepRightButton;
    private Button _coarseStepRightButton;
    private Device _currentDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up the container and its adapter for devices.
        _moveables = new ArrayList<Device>();
        View content_main = findViewById(R.id.content_main);
        _devicesAdapter = new DeviceViewAdapter(MainActivity.this,
                _moveables);
        final ListView deviceListView = (ListView) content_main.findViewById(R.id.deviceListView);
        deviceListView.setAdapter(_devicesAdapter);

        // Set up _uiThread to be a handler that runs runnables on the UI thread.
        // advantage over runOnUiThread() is that we can actually control the state of the thread
        // and cancel it, if needed.
        _uiThread = new Handler(Looper.getMainLooper());

        // Default boolean values: Activity is visible, devices not yet fetched
        _visible = true;
        _canAccessDevices = false;

        ConnectionData connData = (ConnectionData) getIntent().getSerializableExtra(
                LoginActivity.MESSAGE_CONNECTION_DATA);

        // References to the 'steps' views.
        final EditText coarseStepEditText = (EditText) findViewById(R.id.coarseStepEditText);
        final EditText fineStepEditText = (EditText) findViewById(R.id.fineStepEditText);
        final Button applyButton = (Button) findViewById(R.id.applyButton);

        // When clickking 'enter' in the first EditText, switch to the next one.
        coarseStepEditText.setNextFocusDownId(R.id.fineStepEditText);

        // When hitting 'enter' or 'ok' on the keyboard while in the last EditText, apply changes.
        fineStepEditText.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionID, KeyEvent keyEvent) {
                if (actionID == EditorInfo.IME_ACTION_DONE) {
                    fineStepEditText.clearFocus();
                    applyButton.callOnClick();
                }
                return false;
            }
        });

        // Clicked the 'apply' button.
        applyButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                view.requestFocus();
                // Hide keyboard.
                InputMethodManager manager =
                        (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                manager.hideSoftInputFromWindow(view.getWindowToken(), 0);
                // TODO: Implement applying steps!
            }
        });

        // Reference to the bottom slider panel.
        _slidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);

        // Change behavior of Panel when state changes.
        _slidingUpPanelLayout.addPanelSlideListener(new SlidingUpPanelLayout.PanelSlideListener() {
            @Override
            public void onPanelSlide(View panel, float slideOffset) {
            }

            @Override
            public void onPanelStateChanged(View panel,
                                            SlidingUpPanelLayout.PanelState previousState,
                                            SlidingUpPanelLayout.PanelState newState) {
                if (newState == SlidingUpPanelLayout.PanelState.COLLAPSED) {
                    // When hiding the panel, also hide the keyboard and clear all focuses.
                    InputMethodManager manager =
                            (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    manager.hideSoftInputFromWindow(panel.getWindowToken(), 0);
                    coarseStepEditText.clearFocus();
                    coarseStepEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                    fineStepEditText.clearFocus();
                    fineStepEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                }

            }
        });

        // Reference to current device and the 3 subviews of currentDeviceView.
        // That means the name label, value label and status image.
        _currentDevice = null;
        _currentDeviceTextView = (TextView) findViewById(R.id.deviceNameTextView);
        _currentDeviceValueTextView = (TextView) findViewById(R.id.deviceValueTextView);
        _currentDeviceStatusImageView = (ImageView) findViewById(R.id.statusledView);

        // References to the 5 control buttons.
        _coarseStepLeftButton = (Button) findViewById(R.id.coarseStepLeftButton);
        _fineStepLeftButton = (Button) findViewById(R.id.fineStepLeftButton);
        _stopButton = (Button) findViewById(R.id.stopButton);
        _fineStepRightButton = (Button) findViewById(R.id.fineStepRightButton);
        _coarseStepRightButton = (Button) findViewById(R.id.coarseStepRightButton);
        _slidingUpPanelLayout.setEnabled(false);


        // Change behavior when clicking/tapping on a device.
        deviceListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                Device device = (Device) deviceListView.getItemAtPosition(position);
                onDeviceSelected(device);
            }
        });

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
        if (_slidingUpPanelLayout.getPanelState() == SlidingUpPanelLayout.PanelState.EXPANDED) {
            _slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
            return;
        }
        _visible = false;
        NicosClient.getClient().unregisterCallbackHandler(this);
        new Thread(new Runnable() {
            @Override
            public void run() {
                NicosClient.getClient().disconnect();
            }
        }).start();
        // null parameter -> remove ALL runnables.
        _uiThread.removeCallbacks(null);
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        _visible = false;
        NicosClient.getClient().unregisterCallbackHandler(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        _visible = true;
    }

    @Override
    public void handleSignal(String signal, Object data, Object args) {
        if (signal.equals("broken")) {
            final String error = (String) data;
            // Connection is broken. Try to disconnect what's left and go back to login screen.
            NicosClient.getClient().unregisterCallbackHandler(this);
            NicosClient.getClient().disconnect();
            if (!_visible) return;

            // Activity is still visible, user probably didn't intend to shut down connection.
            // We display an error.
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog alertDialog = null;
                    try {
                        alertDialog = new AlertDialog.Builder(MainActivity.this).create();
                        alertDialog.setTitle("Disconnected");
                        alertDialog.setMessage(error);
                        alertDialog.setButton(AlertDialog.BUTTON_NEGATIVE, "Okay",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                        finish();
                                    }
                                });
                        alertDialog.setCancelable(false);
                        alertDialog.setCanceledOnTouchOutside(false);
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
        else if (signal.equals("cache")) {
            on_client_cache((Object[]) data);
        }
        else if (signal.equals("status")) {
            // data = tuple of (status, linenumber)
            _current_status = (int) ((Object[]) data)[0];
        }
    }

    private void onDeviceSelected(Device device) {
        Object limits = device.getParam("userlimits");
        Object mapping = device.getParam("mapping");
        if (limits == null && mapping == null) {
            Toast.makeText(getApplicationContext(), "Cannot move " + device.getName() +
                    ": Neither abslimit nor mapping known", Toast.LENGTH_SHORT).show();
            return;
        }

        _currentDevice = device;
        _currentDeviceTextView.setText(device.getName());
        _currentDeviceValueTextView.setText(device.getFormattedValue());
        _currentDeviceStatusImageView.setImageResource(
                DeviceStatus.getStatusResource(device.getStatus()));

        if (limits != null) {
            Object min = ((Object[]) limits)[0];
            Object max = ((Object[]) limits)[1];
            System.out.println("min: " + min.toString() + ", max: " + max.toString());
        }
        else {
            HashMap map = (HashMap) mapping;
            for (Object key : map.keySet()) {
                System.out.print("--- ");
                System.out.print(key);
                System.out.print(": ");
                System.out.println(map.get(key));
            }
        }

        _coarseStepLeftButton.setEnabled(true);
        _fineStepLeftButton.setEnabled(true);
        _stopButton.setEnabled(true);
        _fineStepRightButton.setEnabled(true);
        _coarseStepRightButton.setEnabled(true);
        _slidingUpPanelLayout.setEnabled(true);
    }

    private void on_client_connected() {
        // Query moveables.
        final ArrayList<String> lowercaseMoveables =
                (ArrayList<String>) NicosClient.getClient().getDeviceList(
                        "nicos.core.device.Moveable", true, null, null);

        // Ask for current status.
        Object untyped_state = NicosClient.getClient().ask("getstatus", null);
        if (untyped_state == null) {
            return;
        }
        final HashMap<String, Object> state = (HashMap<String, Object>) untyped_state;
        Object[] statusTuple = (Object[]) state.get("status");
        _current_status = (int) statusTuple[0];

        // Extract device list from status. We need this for the device's "real" name (with upper
        // case letters).
        final AtomicBoolean uiAddDevicesDone = new AtomicBoolean(false);
        final Runnable uiAddDevices = new Runnable() {
            @Override
            public void run() {
                // Block access to this runnable.
                synchronized (this) {
                    ArrayList<String> devlist = (ArrayList<String>) state.get("devices");
                    for (String device : devlist) {
                        String cachekey = device.toLowerCase();
                        if (!lowercaseMoveables.contains(cachekey)) {
                            continue;
                        }
                        // Create device.
                        final Device moveable = new Device(device, cachekey);

                        // Sort devices in place.
                        _moveables.add(moveable);
                        Collections.sort(_moveables, new Comparator<Device>() {
                            @Override
                            public int compare(Device lhs, Device rhs) {
                                return lhs.getCacheName().compareTo(rhs.getCacheName());
                            }
                        });

                        // Notify adapter to update UI.
                        _devicesAdapter.notifyDataSetChanged();
                    }
                    uiAddDevicesDone.set(true);
                    // Tell other threads they can now safely access this runnable.
                    notify();
                }
            }
        };

        // Ask ui thread to run the runnable, we wait until ui thread is done.
        _uiThread.post(uiAddDevices);
        // Try to accesss runnable.
        synchronized (uiAddDevices) {
            while (!uiAddDevicesDone.get()) {
                try {
                    // Wait for runnable to call notify().
                    uiAddDevices.wait();
                } catch (InterruptedException e) {
                    // Thread cancelled -> Activity probably destroyed.
                }
            }
        }

        // UI thread is done adding devices.
        _canAccessDevices = true;

        // Query statuses and values of all devices.

        // List of currently running runnables that update devices.
        for (final Device device : _moveables) {
            // Query this device's status.
            Object untypedStatus = NicosClient.getClient().getDeviceStatus(device.getName());
            Object[] tupleStatus;

            try {
                tupleStatus = (Object[]) untypedStatus;
                if (tupleStatus == null) {
                    throw new RuntimeException();
                }
            }
            catch (Exception e) {
                tupleStatus = new Object[] {-1, null};
            }

            final int status = (int) tupleStatus[0];
            final Object value = NicosClient.getClient().getDeviceValue(device.getName());

            // Query value types.
            Object valuetype = NicosClient.getClient().getDeviceValuetype(device.getName());
            String pyclass;
            final Class valueclass;

            if (valuetype.getClass() == ClassDictConstructor.class) {
                // Java ô.o
                Object[] o = {};
                ClassDict s = ((ClassDict) ((ClassDictConstructor) valuetype).construct(o));
                s.__setstate__(new HashMap<String, Object>());
                pyclass = (String) s.get("__class__");
            } else {
                pyclass = (String) ((ClassDict) valuetype).get("__class__");
            }

            switch (pyclass) {
                case "__builtin__.float":
                    valueclass = Double.class;
                    break;
                case "__builtin__.double":
                    valueclass = Double.class;
                    break;
                case "nicos.core.params.tupleof":
                    valueclass = Object[].class;
                    break;
                case "nicos.core.params.oneof":
                    valueclass = String.class;
                    break;
                case "nicos.core.params.oneofdict":
                    valueclass = String.class;
                    break;
                case "nicos.core.params.limits":
                    valueclass = Object[].class;
                    break;
                default:
                    valueclass = String.class;
                    break;
            }

            // A runnable to update the device in UI thread with new status + value.
            final Runnable uiChangeValue = new Runnable() {
                @Override
                public void run() {
                    device.setStatus(status);
                    device.setValue(value);
                    device.setValuetype(valueclass);
                    _devicesAdapter.notifyDataSetChanged();
                }
            };
            // Let UI thread run the runnable.
            _uiThread.post(uiChangeValue);
        }

        // We continue here,  although updating all values may not be done yet. But we can already
        // start querying the parameters, they also just update the devices.
        // Whether value or fmtstr get added first doesn't matter: Both force the UI to update
        // itself. So the latter one always ensures correct display.
        for (final Device device : _moveables) {
            ArrayList<Object> params = NicosClient.getClient().getDeviceParams(
                    device.getCacheName());
            if (params == null) {
                continue;
            }

            for (Object param : params) {
                final Object[] tuple = (Object[]) param;
                // Split device name from parameter name.
                // e.g. t/fmtstr -> fmtstr
                final String[] keyParts = ((String) tuple[0]).split(("/"));
                _uiThread.post(new Runnable() {
                    @Override
                    public void run() {
                        device.addParam(keyParts[1], tuple[1]);
                        _devicesAdapter.notifyDataSetChanged();
                    }
                });

            }
        }
    }

    private Device getDeviceByCacheName(String cacheName) {
        for (Device d : _moveables) {
            if (d.getCacheName().equals(cacheName)) {
                return d;
            }
        }
        return null;
    }

    private void on_client_cache(Object[] data) {
        if (!_canAccessDevices) {
            return;
        }
        String key = (String) data[1];
        String[] splitted = key.split("/");
        String devname = splitted[0];
        String subkey = splitted[1];

        Device maybeDevice;
        maybeDevice = getDeviceByCacheName(devname);
        if (maybeDevice == null) {
            // A device not in the list, probably not a moveable.
            return;
        }

        final Device curdev = maybeDevice;
        final Object value = data[3];

        if (subkey.equals("status")) {
            // Cache string.
            String tuple = (String) value;
            // cut '(' and ')'
            tuple = tuple.substring(1, tuple.length() - 1);
            String[] tupelupel = tuple.split(",");
            final int status = Integer.valueOf(tupelupel[0]);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    curdev.setStatus(status);
                    if (_currentDevice == curdev) {
                        _currentDeviceStatusImageView.setImageResource(
                                DeviceStatus.getStatusResource(status));
                    }
                    _devicesAdapter.notifyDataSetChanged();
                }
            });
        }
        else if (subkey.equals("value")) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    curdev.setValueFromCache(value.toString());
                    if (_currentDevice == curdev) {
                        _currentDeviceValueTextView.setText(
                                curdev.getFormattedValue());
                    }
                    _devicesAdapter.notifyDataSetChanged();
                }
            });
        }
        else if (subkey.equals("fmtstr")) {
            final String fmt = (String) data[3];
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    curdev.addParam("fmtstr", fmt);
                    if (_currentDevice == curdev) {
                        _currentDeviceValueTextView.setText(
                                curdev.getFormattedValue());
                    }
                    _devicesAdapter.notifyDataSetChanged();
                }
            });
        }
    }
}
