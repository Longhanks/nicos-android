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


package de.tum.frm2.nicos_android.nicos;

import de.tum.frm2.nicos_android.R;

public class DeviceStatus {
    public static final int OK = 200;
    public static final int WARN = 210;
    public static final int BUSY = 220;
    public static final int NOTREACHED = 230;
    public static final int ERROR = 240;
    public static final int UNKNOWN = 999;

    public static int getStatusResource(int status) {
        switch (status) {
            case OK:
                return R.drawable.simplegreen;
            case WARN:
                return R.drawable.simplewarn;
            case BUSY:
                return R.drawable.simpleyellow;
            case UNKNOWN:
                return R.drawable.simplewhite;
            case ERROR:
                return R.drawable.simplered;
            case NOTREACHED:
                return R.drawable.simplered;
            default:
                return R.drawable.simplegreen;
        }
    }
    
    public static int getStatusColor(int status) {
        switch (status) {
            // Colors for the background are a little lighter that the real ones (commented below)
            case OK:
                return 0xFFC1EAC4;
                // return 0xFF33BC3D;
            case DeviceStatus.WARN:
                return 0xFFF938D;
                // return 0xFFBB8425;
            case DeviceStatus.BUSY:
                return 0xFFFFFFb2;
                // return 0xFFFFFF00;
            case DeviceStatus.UNKNOWN:
                return 0xFF929292;
            case DeviceStatus.ERROR:
                return 0xFFFFBAB2;
                // return 0xFFFF1A00;
            case DeviceStatus.NOTREACHED:
                return 0xFFFFBAB2;
            // return 0xFFFF1A00;
            default:
                return 0xFFC1EAC4;
            // return 0xFF33BC3D;
        }
    }
}
