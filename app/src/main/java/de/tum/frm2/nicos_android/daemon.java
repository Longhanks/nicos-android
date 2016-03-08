package de.tum.frm2.nicos_android;

// Resembles nicos-core/nicos/protocols/daemon.py

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class daemon {
    // Resembles nicos-core/nicos/clients/base.py
    // It's STRONGLY recommended to look at the Python source when concerned about the
    // functionality of this class.

    // protocol version
    public static final int PROTO_VERSION = 13;

    public static final byte ENQ = 0x05;
    public static final byte ACK = 0x06;
    public static final byte NAK = 0x15;
    public static final byte STX = 0x03;

    public static final Map<String, Byte> command2code = Collections.unmodifiableMap(
            new HashMap<String, Byte>() {{
                put("start",         (byte) 0x01);
                put("simulate",      (byte) 0x02);
                put("queue",         (byte) 0x03);
                put("unqueue",       (byte) 0x04);
                put("update",        (byte) 0x05);
                // script flow commands
                put("break",         (byte) 0x11);
                put("continue",      (byte) 0x12);
                put("stop",          (byte) 0x13);
                put("emergency",     (byte) 0x14);
                // async execution commands
                put("exec",          (byte) 0x21);
                put("eval",          (byte) 0x22);
                // watch variable commands
                put("watch",         (byte) 0x31);
                put("unwatch",       (byte) 0x32);
                // enquiries
                put("getversion",    (byte) 0x41);
                put("getstatus",     (byte) 0x42);
                put("getmessages",   (byte) 0x43);
                put("getscript",     (byte) 0x44);
                put("gethistory",    (byte) 0x45);
                put("getcachekeys",  (byte) 0x46);
                put("gettrace",      (byte) 0x47);
                put("getdataset",    (byte) 0x48);
                // miscellaneous commands
                put("complete",      (byte) 0x51);
                put("transfer",      (byte) 0x52);
                put("debug",         (byte) 0x53);
                put("debuginput",    (byte) 0x54);
                // connection related commands
                put("eventmask",     (byte) 0x61);
                put("unlock",        (byte) 0x62);
                put("quit",          (byte) 0x63);
                put("authenticate",  (byte) 0x64);  // only used during handshake
            }}
    );

    public static final Map<Byte, String> code2command = Collections.unmodifiableMap(
            new HashMap<Byte, String>() {{
                for (String key : command2code.keySet()) {
                    put(command2code(key), key);
                }
            }}
    );

    public static byte command2code(String command) {
        return command2code.get(command);
    }

    public static String code2command(byte code) {
        return code2command.get(code);
    }

    public static final Map<String, TupleOfTwo<Boolean, Integer>> command2event =
            Collections.unmodifiableMap(
                new HashMap<String, TupleOfTwo<Boolean, Integer>>() {{
                    // a new message arrived
                    put("message",    new TupleOfTwo<Boolean, Integer>(true,  0x1001));
                    // a new request arrived
                    put("request",    new TupleOfTwo<Boolean, Integer>(true,  0x1002));
                    // a request is now being processed
                    put("processing", new TupleOfTwo<Boolean, Integer>(true,  0x1003));
                    // one or more requests have been blocked from execution
                    put("blocked",    new TupleOfTwo<Boolean, Integer>(true,  0x1004));
                    // the execution status changed
                    put("status",     new TupleOfTwo<Boolean, Integer>(true,  0x1005));
                    // the watched variables changed
                    put("watch",      new TupleOfTwo<Boolean, Integer>(true,  0x1006));
                    // the mode changed
                    put("mode",       new TupleOfTwo<Boolean, Integer>(true,  0x1007));
                    // a new cache value has arrived
                    put("cache",      new TupleOfTwo<Boolean, Integer>(true,  0x1008));
                    // a new dataset was created
                    put("dataset",    new TupleOfTwo<Boolean, Integer>(true,  0x1009));
                    // a new point was added to a dataset
                    put("datapoint",  new TupleOfTwo<Boolean, Integer>(true,  0x100A));
                    // a new fit curve was added to a dataset
                    put("datacurve",  new TupleOfTwo<Boolean, Integer>(true,  0x100B));
                    // new parameters for the data sent with the "livedata" event
                    put("liveparams", new TupleOfTwo<Boolean, Integer>(true,  0x100C));
                    // live detector data to display
                    put("livedata",   new TupleOfTwo<Boolean, Integer>(false, 0x100D));
                    // a simulation finished with the given result
                    put("simresult",  new TupleOfTwo<Boolean, Integer>(true,  0x100E));
                    // request to display given help page
                    put("showhelp",   new TupleOfTwo<Boolean, Integer>(true,  0x100F));
                    // request to execute something on the client side
                    put("clientexec", new TupleOfTwo<Boolean, Integer>(true,  0x1010));
                    // a watchdog notification has arrived
                    put("watchdog",   new TupleOfTwo<Boolean, Integer>(true,  0x1011));
                    // the remote-debugging status changed
                    put("debugging",  new TupleOfTwo<Boolean, Integer>(true,  0x1012));
                    // a plug-and-play/sample-environment event occurred
                    put("plugplay",   new TupleOfTwo<Boolean, Integer>(true,  0x1013));
                    // a setup was loaded or unloaded
                    put("setup",      new TupleOfTwo<Boolean, Integer>(true,  0x1014));
                    // a device was created or destroyed
                    put("device",     new TupleOfTwo<Boolean, Integer>(true,  0x1015));
                    // the experiment has changed
                    put("experiment", new TupleOfTwo<Boolean, Integer>(true,  0x1016));
                }}
    );

    public static final Map<Integer, String> event2command = Collections.unmodifiableMap(
            new HashMap<Integer, String>() {{
                for (String key : command2event.keySet()) {
                    put(event2command(key), key);
                }
            }}
    );

    public static int event2command(String event) {
        return command2event.get(event).getSecond();
    }

    public static String command2event(Integer command) {
        return event2command.get(command);
    }

    public static boolean eventNeedsUnserialize(String event) {
        return command2event.get(event).getFirst();
    }

    public static final Set<String> ACTIVE_COMMANDS = Collections.unmodifiableSet(
            new HashSet<String>() {{
                add("start");
                add("queue");
                add("unqueue");
                add("update");
                add("break");
                add("continue");
                add("stop");
                add("exec");
            }}
    );
}
