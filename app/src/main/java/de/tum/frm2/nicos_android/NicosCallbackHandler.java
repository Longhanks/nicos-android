package de.tum.frm2.nicos_android;

public interface NicosCallbackHandler {
    public void handleSignal(String signal, Object data, Object args);
}
