package de.tum.frm2.nicos_android;

// Resembles nicos-core/nicos/protocols/daemon.py

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class daemon {
    public static final byte ENQ = 0x05;
    public static final byte ACK = 0x06;
    public static final byte NAK = 0x15;
    public static final byte STX = 0x03;

    public static final Map<String, Byte> command2code;

    static {
        command2code = Collections.unmodifiableMap(
                new HashMap<String, Byte>() {{
                    put("start",        (byte) 0x01);
                    put("simulate",     (byte) 0x02);
                    put("queue",        (byte) 0x03);
                    put("unqueue",      (byte) 0x04);
                    put("update",       (byte) 0x05);
                    // script flow commands
                    put("break",        (byte) 0x11);
                    put("continue",     (byte) 0x12);
                    put("stop",         (byte) 0x13);
                    put("emergency",    (byte) 0x14);
                    // async execution commands
                    put("exec",         (byte) 0x21);
                    put("eval",         (byte) 0x22);
                    // watch variable commands
                    put("watch",        (byte) 0x31);
                    put("unwatch",      (byte) 0x32);
                    // enquiries
                    put("getversion",   (byte) 0x41);
                    put("getstatus",    (byte) 0x42);
                    put("getmessages",  (byte) 0x43);
                    put("getscript",    (byte) 0x44);
                    put("gethistory",   (byte) 0x45);
                    put("getcachekeys", (byte) 0x46);
                    put("gettrace",     (byte) 0x47);
                    put("getdataset",   (byte) 0x48);
                    // miscellaneous commands
                    put("complete",     (byte) 0x51);
                    put("transfer",     (byte) 0x52);
                    put("debug",        (byte) 0x53);
                    put("debuginput",   (byte) 0x54);
                    // connection related commands
                    put("eventmask",    (byte) 0x61);
                    put("unlock",       (byte) 0x62);
                    put("quit",         (byte) 0x63);
                    put("authenticate", (byte) 0x64);  // only used during handshake
                }});
    }

    public static final Map<Byte, String> code2command = Collections.unmodifiableMap(
            new HashMap<Byte, String>() {{
        for (String key : command2code.keySet()) {
            put(command2code(key), key);
        }
    }});

    public static byte command2code(String command) {
        return command2code.get(command);
    }

    public static String code2command(byte code) {
        return code2command.get(code);
    }
}
