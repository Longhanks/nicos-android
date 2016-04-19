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
}
