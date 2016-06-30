//
// Copyright (C) 2016 Andreas Schulz <andreas.schulz@frm2.tum.de>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 US


package de.tum.frm2.nicos_android.gui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextWatcher;
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
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.sothree.slidinguppanel.SlidingUpPanelLayout;

import net.razorvine.pickle.objects.ClassDict;
import net.razorvine.pickle.objects.ClassDictConstructor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;

import de.tum.frm2.nicos_android.nicos.ConnectionData;
import de.tum.frm2.nicos_android.nicos.Device;
import de.tum.frm2.nicos_android.nicos.DeviceStatus;
import de.tum.frm2.nicos_android.nicos.NicosMessageLevel;
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
    private String _uniquePrefix;
    private SlidingUpPanelLayout _slidingUpPanelLayout;
    private TextView _currentDeviceTextView;
    private TextView _currentDeviceValueTextView;
    private ImageView _currentDeviceStatusImageView;
    private Button _coarseStepLeftButton;
    private Button _fineStepLeftButton;
    private Button _stopButton;
    private Button _fineStepRightButton;
    private Button _coarseStepRightButton;
    private EditText _coarseStepEditText;
    private EditText _fineStepEditText;
    private Device _currentDevice;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Set up the container and its adapter for devices.
        _moveables = new ArrayList<>();
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
        _uniquePrefix = connData.getHost() + connData.getUser();

        // References to the 'steps' views.
        _coarseStepEditText = (EditText) findViewById(R.id.coarseStepEditText);
        _fineStepEditText = (EditText) findViewById(R.id.fineStepEditText);

        TextView.OnEditorActionListener onEditorActionListener =
                new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int actionID, KeyEvent keyEvent) {
                if (actionID == EditorInfo.IME_ACTION_DONE) {
                    _fineStepEditText.clearFocus();
                    // Hide keyboard.
                    InputMethodManager manager =
                            (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                    manager.hideSoftInputFromWindow(textView.getWindowToken(), 0);
                }
                return false;
            }
        };

        // When hitting 'enter' or 'ok' on the keyboard while in EditText, apply changes and hide
        // keyboard.
        _coarseStepEditText.setOnEditorActionListener(onEditorActionListener);
        _fineStepEditText.setOnEditorActionListener(onEditorActionListener);

        TextWatcher textWatcher =  new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {}

            @Override
            public void afterTextChanged(Editable editable) {
                saveSteps();
            }
        };

        _coarseStepEditText.addTextChangedListener(textWatcher);
        _fineStepEditText.addTextChangedListener(textWatcher);

        // Reference to the bottom slider panel + initial height.
        _slidingUpPanelLayout = (SlidingUpPanelLayout) findViewById(R.id.sliding_layout);
        RelativeLayout currentDeviceLayout = (RelativeLayout) findViewById(R.id.currentDeviceView);
        currentDeviceLayout.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        _slidingUpPanelLayout.setPanelHeight(currentDeviceLayout.getMeasuredHeight());

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
                    _coarseStepEditText.clearFocus();
                    _coarseStepEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
                    _fineStepEditText.clearFocus();
                    _fineStepEditText.setImeOptions(EditorInfo.IME_ACTION_DONE);
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
        _coarseStepLeftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onStepButtonClicked(_coarseStepLeftButton);
            }
        });
        _fineStepLeftButton = (Button) findViewById(R.id.fineStepLeftButton);
        _fineStepLeftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onStepButtonClicked(_fineStepLeftButton);
            }
        });
        _stopButton = (Button) findViewById(R.id.stopButton);
        _stopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onStopButtonClicked();
            }
        });
        _fineStepRightButton = (Button) findViewById(R.id.fineStepRightButton);
        _fineStepRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onStepButtonClicked(_fineStepRightButton);
            }
        });
        _coarseStepRightButton = (Button) findViewById(R.id.coarseStepRightButton);
        _coarseStepRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onStepButtonClicked(_coarseStepRightButton);
            }
        });
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
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
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
        _slidingUpPanelLayout.setEnabled(true);
        _slidingUpPanelLayout.setPanelState(SlidingUpPanelLayout.PanelState.COLLAPSED);
        _slidingUpPanelLayout.setEnabled(false);
        _visible = true;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        System.out.print("Use JK design: ");
        System.out.println(prefs.getBoolean(getResources().getString(R.string.key_jk_design_switch), false));
    }

    @Override
    protected void onSaveInstanceState(Bundle instance) {
        if (_currentDevice != null) {
            instance.putString("currentDevice", _currentDevice.getName());
        }
        super.onSaveInstanceState(instance);
    }

    @Override
    protected void onRestoreInstanceState(Bundle instance) {
        final String previousDeviceName = instance.getString("currentDevice");
        if (previousDeviceName != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    while (!_canAccessDevices) {
                        try {
                            Thread.sleep(100);
                        } catch (InterruptedException e) {
                            return;
                        }
                    }
                    _uiThread.post(new Runnable() {
                        @Override
                        public void run() {
                            onDeviceSelected(getDeviceByCacheName(
                                    previousDeviceName.toLowerCase()));
                        }
                    });
                }
            }).start();
        }
        super.onRestoreInstanceState(instance);
    }

    private void saveSteps() {
        // Try saving the steps, if they are valid. Else, just ignore saving.
        SharedPreferences.Editor editor = PreferenceManager.getDefaultSharedPreferences(this).edit();
        try {
            double coarse = Double.parseDouble(_coarseStepEditText.getText().toString());
            double fine = Double.parseDouble(_fineStepEditText.getText().toString());
            String coarseKey = _uniquePrefix + _currentDevice.getName() + "coarse";
            String fineKey = _uniquePrefix + _currentDevice.getName() + "fine";
            editor.putLong(coarseKey, Double.doubleToRawLongBits(coarse));
            editor.putLong(fineKey, Double.doubleToRawLongBits(fine));
            editor.apply();
        } catch (Exception e) {
            // Probably invalid steps
        }
    }

    @Override
    public void handleSignal(String signal, Object data, Object args) {
        switch (signal) {
            case "broken":
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
                        AlertDialog alertDialog;
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
                break;

            case "cache":
                on_client_cache((Object[]) data);
                break;

            case "status":
                // data = tuple of (status, linenumber)
                _current_status = (int) ((Object[]) data)[0];
                break;

            case "message":
                final ArrayList msgList = (ArrayList) data;
                if ((int) msgList.get(2) == NicosMessageLevel.ERROR) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),
                                    NicosMessageLevel.level2name(NicosMessageLevel.ERROR) + ": " +
                                            msgList.get(3), Toast.LENGTH_SHORT).show();
                        }
                    });
                }
                break;

            case "error":
                final String msg = (String) data;
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(getApplicationContext(), msg, Toast.LENGTH_SHORT).show();
                    }
                });
                break;
        }
    }

    private void onStopButtonClicked() {
        exec_command("stop(" + _currentDevice.getName() + ")");
    }

    private void onStepButtonClicked(Button btn) {
        // The EditText which needs to be parsed for the correct step.
        EditText editTextToBeParsed = null;
        // Whether to multiply by -1 (for right or left steps)
        short factor = 0;

        if (btn == _coarseStepLeftButton) {
            editTextToBeParsed = _coarseStepEditText;
            factor = -1;
        }

        else if (btn == _fineStepLeftButton) {
            editTextToBeParsed = _fineStepEditText;
            factor = -1;
        }

        else if (btn == _coarseStepRightButton) {
            editTextToBeParsed = _coarseStepEditText;
            factor = 1;
        }

        else if (btn == _fineStepRightButton) {
            editTextToBeParsed = _fineStepEditText;
            factor = 1;
        }

        Double step = null;
        try {
            if (editTextToBeParsed != null) {
                Editable doubleString = editTextToBeParsed.getText();
                if (doubleString != null) {
                    step = Double.parseDouble(doubleString.toString());
                }
            }
        } catch (Exception e) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),
                            "Invalid step.", Toast.LENGTH_SHORT).show();
                }
            });
            return;
        }

        if (step != null &&  _currentDevice.getValue() != null) {
            Double current;
            try {
                current = (double) _currentDevice.getValue();
            } catch (ClassCastException e) {
                try {
                    current = Double.parseDouble(_currentDevice.getValue().toString());
                } catch (NumberFormatException e1) {
                    return;
                }
            }
            double newVal = current + step * factor;
            exec_command("move(" + _currentDevice.getName() + ", " + String.valueOf(newVal) + ")");
        }
    }

    private void onDeviceSelected(Device device) {
        Object limits = device.getParam("userlimits");
        Object mapping = device.getParam("mapping");
        if (limits == null && mapping == null) {
            Toast.makeText(getApplicationContext(), "Cannot move " + device.getName() +
                    ": Limits unknown", Toast.LENGTH_SHORT).show();
            return;
        }

        _currentDevice = device;
        _currentDeviceTextView.setText(device.getName());
        _currentDeviceValueTextView.setText(device.getFormattedValue());
        _currentDeviceStatusImageView.setImageResource(
                DeviceStatus.getStatusResource(device.getStatus()));

        if (limits != null) {
            Object o_max = ((Object[]) limits)[1];
            double max = (double) o_max;

            // Try to read a value from the preferences.
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String coarseKey = _uniquePrefix + _currentDevice.getName() + "coarse";
            String fineKey = _uniquePrefix + _currentDevice.getName() + "fine";
            if (prefs.contains(coarseKey) && prefs.contains(fineKey)) {
                long dec_coarse = prefs.getLong(coarseKey, 0);
                long dec_fine = prefs.getLong(fineKey, 0);
                _coarseStepEditText.setText(String.valueOf(Double.longBitsToDouble(dec_coarse)));
                _fineStepEditText.setText(String.valueOf(Double.longBitsToDouble(dec_fine)));
            }
            else {
                // infer default steps from the max limit.
                _coarseStepEditText.setText(String.valueOf(max / 5));
                _fineStepEditText.setText(String.valueOf(max / 10));
            }
        }
        else {
            // TODO: Implement devices with mapping!
            return;
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
        final ArrayList lowercaseMoveables = (ArrayList) NicosClient.getClient().getDeviceList(
                        "nicos.core.device.Moveable", true, null, null);

        // Ask for current daemon status.
        Object untyped_state = NicosClient.getClient().ask("getstatus", null);
        if (untyped_state == null) {
            return;
        }
        final HashMap state = (HashMap) untyped_state;
        Object[] statusTuple = (Object[]) state.get("status");
        _current_status = (int) statusTuple[0];

        // Fill _moveables.
        Runnable uiAddMoveables = new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    // Filter device list for moveables.
                    ArrayList devlist = (ArrayList) state.get("devices");
                    for (Object deviceObject : devlist) {
                        String device = (String) deviceObject;
                        String cachekey = device.toLowerCase();
                        if (!lowercaseMoveables.contains(cachekey)) {
                            continue;
                        }
                        // Create device and add it.
                        Device moveable = new Device(device, cachekey);
                        _moveables.add(moveable);

                    }

                    // Sort devices in place.
                    Collections.sort(_moveables, new Comparator<Device>() {
                        @Override
                        public int compare(Device lhs, Device rhs) {
                            return lhs.getCacheName().compareTo(rhs.getCacheName());
                        }
                    });
                    notify();
                }
            }
        };

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (uiAddMoveables) {
            _uiThread.post(uiAddMoveables);
            try {
                uiAddMoveables.wait();
            } catch (InterruptedException e) {
                return;
            }
        }

        // Query parameters.
        for (final Device device : _moveables) {
            final ArrayList params = NicosClient.getClient().getDeviceParams(device.getCacheName());
            if (params == null) {
                continue;
            }

            // Add params to device.
            Runnable uiAddParams = new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        for (Object param : params) {
                            Object[] tuple = (Object[]) param;
                            // Split device name from parameter name.
                            // e.g. t/fmtstr -> fmtstr
                            String[] keyParts = ((String) tuple[0]).split(("/"));
                            device.addParam(keyParts[1], tuple[1]);
                        }
                        notify();
                    }
                }
            };

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (uiAddParams) {
                _uiThread.post(uiAddParams);
                try {
                    uiAddParams.wait();
                } catch (InterruptedException e) {
                    return;
                }
            }
        }

        // Remove all moveables without abslimits.
        Runnable uiRemoveDevicesWithoutLimits = new Runnable() {
            @Override
            public void run() {
                synchronized (this) {
                    Iterator<Device> it = _moveables.iterator();
                    while (it.hasNext()) {
                        Device device = it.next();
                        if (device.getParam("abslimits") == null) {
                            it.remove();
                        }
                    }
                    _devicesAdapter.notifyDataSetChanged();
                    _canAccessDevices = true;
                    notify();
                }
            }
        };

        //noinspection SynchronizationOnLocalVariableOrMethodParameter
        synchronized (uiRemoveDevicesWithoutLimits) {
            _uiThread.post(uiRemoveDevicesWithoutLimits);
            try {
                uiRemoveDevicesWithoutLimits.wait();
            } catch (InterruptedException e) {
                return;
            }
        }

        // Query statuses and values of all devices.
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

            if (valuetype == null) {
                // Response timed out
                continue;
            }
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
            Runnable uiChangeValue = new Runnable() {
                @Override
                public void run() {
                    synchronized (this) {
                        device.setStatus(status);
                        device.setValue(value);
                        device.setValuetype(valueclass);
                        _devicesAdapter.notifyDataSetChanged();
                        if (_currentDevice == device) {
                            _currentDeviceValueTextView.setText(
                                    device.getFormattedValue());
                        }
                        notify();
                    }
                }
            };

            //noinspection SynchronizationOnLocalVariableOrMethodParameter
            synchronized (uiChangeValue) {
                _uiThread.post(uiChangeValue);
                try {
                    uiChangeValue.wait();
                } catch (InterruptedException e) {
                    return;
                }
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

    private void exec_command(final String command) {
        if (!(_current_status == NicosStatus.STATUS_IDLE ||
                _current_status == NicosStatus.STATUS_IDLEEXC)) {
            // Server is not idle.
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    DialogInterface.OnClickListener dialogClickListener =
                            new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            switch (which) {
                                case DialogInterface.BUTTON_NEUTRAL:
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            NicosClient.getClient().tell("exec", command);
                                        }
                                    }).start();
                                    break;
                                case DialogInterface.BUTTON_NEGATIVE:
                                    return;
                                case DialogInterface.BUTTON_POSITIVE:
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            NicosClient.getClient().run(command);
                                        }
                                    }).start();
                                    break;
                            }
                        }
                    };

                    AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                    builder.setMessage("A script is currently running. What do you want to do?");
                    builder.setNeutralButton("Execute now!", dialogClickListener);
                    builder.setNegativeButton("Cancel", dialogClickListener);
                    builder.setPositiveButton("Queue script", dialogClickListener);
                    int version = Build.VERSION.SDK_INT;
                    int color;
                    if (version >= 23) {
                        color = ContextCompat.getColor(MainActivity.this, R.color.colorPrimary);
                    }
                    else {
                        // It's only deprecated since API level 23.
                        //noinspection deprecation
                        color = getResources().getColor(R.color.colorPrimary);
                    }
                    AlertDialog dlg = builder.create();
                    dlg.show();
                    dlg.getButton(AlertDialog.BUTTON_NEUTRAL).setTextColor(color);
                    dlg.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(Color.RED);
                    dlg.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
                }
            });
        }

        else {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    NicosClient.getClient().run(command);
                }
            }).start();
        }
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

        switch (subkey) {
            case "status":
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
                break;

            case "value":
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
                break;

            case "fmtstr":
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
                break;
        }
    }
}
