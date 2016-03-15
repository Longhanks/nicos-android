package de.tum.frm2.nicos_android.gui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import de.tum.frm2.nicos_android.R;
import de.tum.frm2.nicos_android.nicos.Device;
import de.tum.frm2.nicos_android.nicos.status;

public class DeviceViewAdapter extends ArrayAdapter<Device> {
    private final Context context;
    private final Device[] devices;

    public DeviceViewAdapter(Context context, Device[] devices) {
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
        Device device = devices[position];

        deviceNameTextView.setText(device.getName());
        String value = "";
        if (device.getValue() != null) {
            value = device.getValue().toString();
        }
        deviceValueTextView.setText(value);
        switch (device.getStatus()) {
            case status.OK:
                statusledView.setImageResource(R.drawable.simplegreen);
                break;
            case status.WARN:
                statusledView.setImageResource(R.drawable.simplewarn);
                break;
            case status.BUSY:
                statusledView.setImageResource(R.drawable.simpleyellow);
                break;
            case status.UNKNOWN:
                statusledView.setImageResource(R.drawable.simplewhite);
                break;
            case status.ERROR:
                statusledView.setImageResource(R.drawable.simplered);
                break;
            case status.NOTREACHED:
                statusledView.setImageResource(R.drawable.simplered);
                break;
            default:
                statusledView.setImageResource(R.drawable.simplegreen);
        }
        return deviceView;
    }
}