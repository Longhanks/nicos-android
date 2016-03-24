package de.tum.frm2.nicos_android.util;


public class SignalDebugPrinter implements NicosCallbackHandler {
    @Override
    public void handleSignal(String signal, Object data, Object args) {
        if (signal.equals("cache")) {
            return;
        }
        System.out.print(signal);
        if (data != null) {
            System.out.print(": " + data.toString());
            if (args != null) {
                System.out.println(", " + args.toString());
            }
            else {
                System.out.println();
            }
        }
        else {
            System.out.println();
        }
    }
}
