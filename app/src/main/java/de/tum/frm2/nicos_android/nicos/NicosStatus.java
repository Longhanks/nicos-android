package de.tum.frm2.nicos_android.nicos;

public class NicosStatus {
    public static final int STATUS_IDLEEXC  = -2;  // nothing started, last script raised exception
    public static final int STATUS_IDLE     = -1;  // nothing started
    public static final int STATUS_RUNNING  = 0;   // execution running
    public static final int STATUS_INBREAK  = 1;   // execution halted, in break function
    public static final int STATUS_STOPPING = 2;   // stop exception raised, waiting for propagation
}
