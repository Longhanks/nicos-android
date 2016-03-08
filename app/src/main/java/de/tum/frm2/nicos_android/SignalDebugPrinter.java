package de.tum.frm2.nicos_android;


public class SignalDebugPrinter implements NicosCallbackHandler {
    @Override
    public void handleSignal(String signal, Object data, Object args) {
        if (signal.equals("cache")) {
            Object[] tuple = (Object[]) data;
            System.out.println("cache event: " +
                    (String) tuple[1] + (String) tuple[2] + (String) tuple[3]);
        }
        else {
            System.out.print("Other event: " + signal);
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
}
