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
        deviceValueTextView.setText(device.getFormattedValue());
        statusledView.setImageResource(NicosStatus.getStatusResource(device.getStatus()));

        return deviceView;
    }
}