package de.tum.frm2.nicos_android.gui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;

import de.tum.frm2.nicos_android.R;
import de.tum.frm2.nicos_android.nicos.Device;
import de.tum.frm2.nicos_android.nicos.NicosStatus;
import de.tum.frm2.nicos_android.util.ReadOnlyList;


public class DeviceViewAdapter extends ArrayAdapter<Device> {
    private final Context context;
    private ArrayList<Device> devices;

    public DeviceViewAdapter(Context context, ArrayList<Device> devices) {
        super(context, R.layout.sample_device_view, devices);
        this.context = context;
        this.devices = devices;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        // Inflater constructs "widgets" (views) via xml files.
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(
                Context.LAYOUT_INFLATER_SERVICE);
        // Construct each list entry with sample_device_view.xml "widget".
        View deviceView = inflater.inflate(R.layout.sample_device_view, parent, false);
        // Get references to the text and image views on the list entry
        TextView deviceNameTextView = (TextView) deviceView.findViewById(R.id.deviceNameTextView);
        TextView deviceValueTextView = (TextView) deviceView.findViewById(R.id.deviceValueTextView);
        ImageView statusledView = (ImageView) deviceView.findViewById(R.id.statusledView);
        // Map position to index of array.
        Device device = devices.get(position);

        deviceNameTextView.setText(device.getName());
        deviceValueTextView.setText(getFormattedDeviceValue(device));
        statusledView.setImageResource(NicosStatus.getStatusResource(device.getStatus()));

        return deviceView;
    }

    public static String getFormattedDeviceValue(Device device) {
        String format = (String) device.getParam("fmtstr");
        String unit = (String) device.getParam("unit");
        return formatValue(device.getValue(), format, unit);
    }

    private static String formatValue(Object value, String fmt, String unit) {
        if (unit == null) {
            unit = "";
        } else {
            unit = " " + unit;
        }
        if (value != null) {
            String formatted;

            try {
                Class classOfValue = value.getClass();

                if (classOfValue == String.class) {
                    // Check if value is 'hiding' in a String
                    String strValue = (String) value;

                    try {
                        int val = Integer.parseInt(strValue);
                        formatted = String.format(fmt, val);
                    } catch (NumberFormatException e) {
                        // Isn't int.
                        // No try/catch: if this fails, too, there is no hidden value - falls
                        // though to the catch where we just use value.toString()
                        double val = Double.parseDouble(strValue);
                        formatted = String.format(fmt, val);
                    }
                }

                else if (classOfValue == Integer.class) {
                    // int
                    formatted = String.format(fmt, (int) value);
                }

                else if (classOfValue == Double.class) {
                    // float is double after depickling
                    formatted = String.format(fmt, (double) value);
                }

                else if (classOfValue == ReadOnlyList.class ||
                        value.getClass() == ArrayList.class ||
                        value.getClass() == Object[].class) {
                    // list, tuple (Nicos tuplifies all lists; I'm doing "the same")
                    formatted = "(";
                    Object[] tuple;
                    if (value.getClass() == Object[].class) {
                        tuple = (Object[]) value;
                    }
                    else {
                        tuple = ((ArrayList) value).toArray();
                    }
                    try {
                        // Try to append the format using fmtstring.
                        String formattedList = String.format(fmt, tuple);
                        formatted += formattedList;
                    }
                    catch (Exception e) {
                        // Formatting failed.
                        formatted = "("; // resets String if previous attempt left over stuff
                        for (int i = 0; i < tuple.length; ++i) {
                            try {
                                String obj = tuple[i].toString();
                                if (obj.isEmpty()) {
                                    obj = "''";
                                }
                                formatted += obj;
                                if (i + 1 != tuple.length) {
                                    formatted += ", ";
                                }
                            }
                            catch (Exception e2) {
                                // Tuple contains null or some garbage.
                                formatted += "''";
                                if (i + 1 != tuple.length) {
                                    formatted += ", ";
                                }                            }
                        }
                    }
                    formatted += ")";
                }

                else {
                    throw new RuntimeException("Unkown class");
                }
            }
            catch (Exception e) {
                formatted = value.toString();
            }
            return formatted + unit;
        }
        return "None";
    }
}