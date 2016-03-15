package de.tum.frm2.nicos_android.util;

public interface NicosCallbackHandler {
    public void handleSignal(String signal, Object data, Object args);
}
