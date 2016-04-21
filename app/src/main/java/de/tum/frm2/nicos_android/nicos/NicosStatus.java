package de.tum.frm2.nicos_android.nicos;

public class NicosStatus {
    /*idle*/
    public static final int STATUS_IDLEEXC  = -2;  // nothing started, last script raised exception

    /*idle*/
    public static final int STATUS_IDLE     = -1;  // nothing started

    /*running*/
    public static final int STATUS_RUNNING  = 0;   // execution running

    /*paused*/
    public static final int STATUS_INBREAK  = 1;   // execution halted, in break function

    /*running*/
    public static final int STATUS_STOPPING = 2;   // stop exception raised, waiting for propagation
}
