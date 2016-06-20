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
import de.tum.frm2.nicos_android.nicos.DeviceStatus;


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
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.sample_device_view, parent, false);
        }
        // Get references to the text and image views on the list entry
        TextView deviceNameTextView = (TextView) convertView.findViewById(R.id.deviceNameTextView);
        TextView deviceValueTextView = (TextView) convertView.findViewById(
                R.id.deviceValueTextView);
        ImageView statusledView = (ImageView) convertView.findViewById(R.id.statusledView);
        // Map position to index of array.
        Device device = devices.get(position);

        deviceNameTextView.setText(device.getName());
        deviceValueTextView.setText(device.getFormattedValue());
        statusledView.setImageResource(DeviceStatus.getStatusResource(device.getStatus()));
        convertView.setBackgroundColor(DeviceStatus.getStatusColor(device.getStatus()));

        return convertView;
    }
}