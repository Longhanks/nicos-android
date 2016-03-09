package de.tum.frm2.nicos_android;

import android.util.Base64;

import net.razorvine.pickle.Pickler;
import net.razorvine.pickle.PythonException;
import net.razorvine.pickle.Unpickler;

import org.apache.commons.codec.binary.Hex;
import org.apache.commons.codec.digest.DigestUtils;

import org.spongycastle.asn1.ASN1InputStream;
import org.spongycastle.asn1.ASN1Primitive;
import org.spongycastle.asn1.x509.RSAPublicKeyStructure;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;


public class NicosClient {
    // Resembles nicos-core/nicos/clients/base.py

    // Custom Exceptions
    public class ErrorResponse extends Exception {
        public ErrorResponse(String error) {
            super(error);
        }
    }

    public class ProtocolError extends Exception {
        public ProtocolError(String response) {
            super(response);
        }
    }

    // The client singleton
    private static NicosClient client;

    // The main socket and its streams
    private Socket socket;
    private OutputStream socketOut;
    private InputStream socketIn;

    // Unique id of client instance
    private byte[] client_id;

    // The banner, the first response from the server accessible without authentication
    private HashMap nicosBanner;

    // Whether the user is connected to a NICOS daemon
    private boolean connected;

    // Whether client is currently disconnecting
    private boolean disconnecting;

    // The last payload received after calling the run() method
    private Object last_reqno;

    // Whether the user is connected in view only mode
    private boolean viewonly;

    // The user's privileges once logged in
    private int user_level;

    // The event socket and its streams
    private Socket eventSocket;
    private OutputStream eventSocketOut;
    private InputStream eventSocketIn;

    // The list of handlers that receive the signal() calls
    private ArrayList<NicosCallbackHandler> callbackHandlers;

    // constants
    private final static int BUFSIZE = 8192;
    private final static int TIMEOUT = 30000;

    private NicosClient() {
        // private constructor -> Singleton
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
        callbackHandlers = new ArrayList<NicosCallbackHandler>();
        socket = null;
        eventSocket = null;
        connected = false;
        disconnecting = false;
        last_reqno = null;
        viewonly = true;
        user_level = -1;
        client_id = getMD5().digest(getUniqueID().getBytes());

        // Add debug printer for signals.
        callbackHandlers.add(new SignalDebugPrinter());
    }

    public static NicosClient getClient() {
        // Get singleton
        if (client == null) {
            client = new NicosClient();
        }
        return client;
    }

    public void registerCallbackHandler(NicosCallbackHandler handler) {
        // A NicosCallbackHandler can register itself to receive the signal calls.
        callbackHandlers.add(handler);
    }

    public void unregisterCallbackHandler(NicosCallbackHandler handler) {
        callbackHandlers.remove(handler);
    }

    // methods replicated from nicos-core/nicos/clients/base.py
    private void signal(String signal) {
        // Since Java doesn't support default arguments, this is a convenience method for calling
        // signal without data or args.
        signal(signal, null, null);
    }

    private void signal(String signal, Object data) {
        // Same as above: Convenience for data, but no args.
        signal(signal, data, null);
    }

    private void signal(String signal, Object data, Object args) {
        // Every handler's handleSignal(String, Object) will be called.
        // For performance, a handler should return as fast as possible and then handle the data
        // asynchronously.
        for (NicosCallbackHandler handler : callbackHandlers) {
            handler.handleSignal(signal, data, args);
        }
    }

    public void connect(ConnectionData connData) throws RuntimeException {
        connect(connData, null);
    }

    public void connect(ConnectionData connData, Object[] eventmask) throws RuntimeException {
        if (connected) {
            throw new RuntimeException("client already connected");
        }
        disconnecting = false;

        SocketAddress sockaddr;
        try {
            // If ANY code of this scope failes, communication is entirely impossible.
            // That means, no need to catch all exceptions one by one.
            InetAddress addr = InetAddress.getByName(connData.getHost());
            sockaddr = new InetSocketAddress(addr, connData.getPort());

            // Initialize empty socket.
            socket = new Socket();

            // Connects this socket to the server with a specified timeout value
            // If timeout occurs, SocketTimeoutException is thrown
            socket.connect(sockaddr, TIMEOUT);
            socketOut = socket.getOutputStream();
            socketIn = socket.getInputStream();

            // Write client identification: we are a new client
            socketOut.write(client_id);
        }
        catch (Exception e) {
            String msg;
            if (e instanceof IOException) {
                // "null reference" error messages won't help the user.
                msg = "Socket communication failed (server not responding).";
            }
            else {
                msg = "Server connection failed: " + e.getMessage() + ".";
            }
            signal("failed", msg);
            return;
        }

        // read banner
        try {
            TupleOfTwo<Byte, Object> response = _read();
            byte ret = response.getFirst();
            if (ret != daemon.STX) {
                throw new ProtocolError("invalid response format");
            }
            nicosBanner = (HashMap) response.getSecond();
            if (!nicosBanner.containsKey("daemon_version")) {
                throw new ProtocolError("daemon version missing from response");
            }
            int daemon_proto = (int) nicosBanner.get("protocol_version");
            if (daemon_proto != daemon.PROTO_VERSION) {
                throw new ProtocolError("daemon uses protocol " +
                        String.valueOf(daemon_proto) + ", but this client requires protocol " +
                        String.valueOf(daemon.PROTO_VERSION));
            }
        }
        catch (Exception e) {
            signal("failed", "Server(" + connData.getHost() + ":" +
                    String.valueOf(connData.getPort()) + ") handshake failed: " + e.getMessage());
            return;
        }

        // log-in sequence
        char[] password = connData.getPassword();
        Object unwrap = nicosBanner.get("pw_hashing");
        String pw_hashing = "sha1";
        if (unwrap != null) {
            pw_hashing = unwrap.toString();
        }

        String encryptedPassword = null;
        boolean supportsRSA = false;
        try {
            String rsaSupportString = pw_hashing.substring(0, 4);
            supportsRSA = rsaSupportString.equals("rsa,");
        }
        catch (StringIndexOutOfBoundsException e) {
            // Does not start with "rsa," -> does not support RSA encryption.
            // boolean supportsRSA stays at false.
        }
        if (supportsRSA) {
            byte[] keyBytes = Base64.decode(nicosBanner.get("rsakey").toString(), Base64.DEFAULT);
            String publicKeyString = new String(keyBytes, StandardCharsets.UTF_8);
            PublicKey publicKey = extractPublicKey(publicKeyString);

            Cipher cipher = null;
            try {
                cipher = Cipher.getInstance("RSA/None/PKCS1Padding", "BC");
            } catch (NoSuchAlgorithmException |
                     NoSuchProviderException  |
                     NoSuchPaddingException e) {
                // Cannot happen.
            }
            try {
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
            }
            catch (InvalidKeyException e) {
                throw new RuntimeException("The server's RSA key is invalid or incompatible.");
            }

            byte[] encrypted;
            try {
                encrypted = cipher.doFinal(String.valueOf(password).getBytes());
            } catch (IllegalBlockSizeException | BadPaddingException e) {
                e.printStackTrace();
                encrypted = new byte[0];
            }
            encryptedPassword = "RSA:" + Base64.encodeToString(encrypted, Base64.DEFAULT);
        }

        if (pw_hashing.equals("sha1")) {
            encryptedPassword = new String(Hex.encodeHex(
                    DigestUtils.sha1(String.valueOf(password))));
        }

        else if(pw_hashing.equals("md5")) {
            encryptedPassword = new String(Hex.encodeHex(
                    DigestUtils.md5(String.valueOf(password))));
        }

        HashMap<String, String> credentials = new HashMap<String, String>();
        credentials.put("login", connData.getUser());
        credentials.put("passwd", encryptedPassword);
        credentials.put("display", "");

        // Server requires credentials to be wrapped in a tuple with 1 item
        // e.g. python: payload = (credentials,)
        // Pyrolite library matches java.lang.Object arrays to tuples with the array's length.
        Object[] data = {credentials};
        Object untypedAuthResponse = ask("authenticate", data);
        if (untypedAuthResponse == null) {
            return;
        }

        // Login was successful.
        HashMap authResponse = (HashMap) untypedAuthResponse;
        user_level = (int) authResponse.get("user_level");

        if (eventmask != null) {
            tell("eventmask", eventmask);
        }

        // connect to event port
        eventSocket = new Socket();
        try {
            eventSocket.connect(sockaddr);
            eventSocketOut = eventSocket.getOutputStream();
            eventSocketIn = eventSocket.getInputStream();
            eventSocketOut.write(client_id);
        } catch (IOException e) {
            signal("failed", "Event connection failed: " + e.getMessage() + ".", e);
            return;
        }

        // Start event handler
        final Thread event_thread = new Thread(new Runnable() {
            @Override
            public void run() {
                // equals event_handler.
                event_handler();
            }
        });
        event_thread.start();

        connected = true;
        viewonly = connData.getViewonly();
        signal("connected");
    }

    public void event_handler() {
        while (true) {
            try {
                // receive STX (1 byte) + eventcode (2) + length (4)
                byte[] start = new byte[7];
                try {
                    eventSocketIn.read(start);
                } catch (IOException e) {
                    if (!disconnecting) {
                        signal("broken", "Server connection broken.");
                        _close();
                    }
                    return;
                }
                if (start[0] != daemon.STX) {
                    // Every event starts with STX. Else, something's wrong.
                    if (!disconnecting) {
                        signal("broken", "Server connection broken.");
                        _close();
                    }
                    return;
                }
                byte[] slice = Arrays.copyOfRange(start, 3, 7);
                ByteBuffer bb = ByteBuffer.wrap(slice);
                bb.order(ByteOrder.BIG_ENDIAN);
                int length = bb.getInt();

                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                while (buf.size() < length) {
                    byte[] read = new byte[BUFSIZE];
                    try {
                        eventSocketIn.read(read);
                        buf.write(read);
                    } catch (IOException e) {
                        if (!disconnecting) {
                            signal("broken", "Server connection broken.");
                            _close();
                        }
                    }
                }

                boolean should_signal = true;
                String event = null;
                Object data = null;
                try {
                    // Stackoverflow magic to convert 2 bytes to int which can be compared in
                    // daemon.command2event().
                    int eventcode = ((start[1] & 0xff) << 8) | (start[2] & 0x00ff);
                    event = daemon.command2event(eventcode);
                    // serialized or raw data?
                    if (daemon.eventNeedsUnserialize(event)) {
                        Unpickler unpickler = new Unpickler();
                        data = unpickler.loads(buf.toByteArray());
                    } else {
                        data = buf.toByteArray();
                    }
                }
                catch (Exception e) {
                    // Garbled event
                    should_signal = false;
                }
                if (should_signal) {
                    signal(event, data);
                }
            }
            catch (Exception e) {
                if (!disconnecting) {
                    signal("broken", "Server connection broken.");
                    _close();
                }
                return;
            }
        }
    }

    public void disconnect() {
        tell("quit", null);
        _close();
    }

    public void _close() {
        try {
            socket.close();
            eventSocket.close();
        }
        catch (Exception e) {
            // Socket already disconnected.
        }
        socket = null;
        if (connected) {
            connected = false;
            signal("disconnect");
        }
    }

    public void handle_error(Exception e) {
        if (e instanceof ErrorResponse) {
            signal("error", "Error from daemon: " + e.getMessage() + ".");
        }
        else {
            String msg;
            if (e instanceof ProtocolError) {
                msg = "Communication error: ";
            }
            else if (e instanceof IOException) {
                // Handles socket.timeout, any of the streams failing
                // Combines python's socket.timeout and socket.error
                msg = "Server connection broken: ";
            }
            else {
                msg = "Exception occurred: ";
            }
            signal("error", msg + e.getMessage() + ".", e);
            _close();
        }
    }

    public void _write(String command, Object args) throws IOException {
        // Write a command to the server
        // Format: ENQ + commandcode + length + payload
        Pickler pickler = new Pickler();
        byte[] data = pickler.dumps(args);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write(daemon.ENQ);
        // frame format requires: length of (ENQ + commandcode) == 3 bytes
        // That's why an empty byte gets inserted here.
        buffer.write((byte) 0x00);
        buffer.write(daemon.command2code(command));
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putInt(data.length);
        buffer.write(bb.array());
        buffer.write(data);
        socketOut.write(buffer.toByteArray());
        socketOut.flush();
    }

    public TupleOfTwo<Byte, Object> _read() throws ProtocolError {
        // receive first byte + (possibly) length
        byte[] start = new byte[5];
        try {
            socketIn.read(start);
        }
        catch (IOException e) {
            throw new ProtocolError("connection broken");
        }
        if (start[0] == daemon.ACK) {
            // ACK == executed ok, no more information follows
            return new TupleOfTwo<Byte, Object>(start[0], null);
        }

        if (start[0] != daemon.NAK && start[0] != daemon.STX) {
            // Server respondend with neither NAK (error) nor STX (ok)
            throw new ProtocolError("invalid response " + start.toString());
        }

        // it has a length...
        byte[] slice = Arrays.copyOfRange(start, 1, 5);
        ByteBuffer bb = ByteBuffer.wrap(slice);
        bb.order(ByteOrder.BIG_ENDIAN);
        int length = bb.getInt();

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        while (buf.size() < length) {
            byte[] read = new byte[BUFSIZE];
            try {
                socketIn.read(read);
                buf.write(read);
            } catch (IOException e) {
                throw new ProtocolError("connection broken");
            }
        }
        Unpickler unpickler = new Unpickler();
        Object result = null;
        try {
            result = unpickler.loads(buf.toByteArray());
        } catch (IOException e) {
            // result stays at null.
            handle_error(e);
        }
        return new TupleOfTwo<Byte, Object>(start[0], result);
    }

    public boolean tell(String command, Object arguments) {
        // Excecute a command that does not generate a response.
        // The arguments are the command and its parameter(s), if necessary.

        // This function has the python signature tell(self, *args)
        // *args always is a tuple, even if it's only one argument.
        // That means we need to wrap java.lang.Object args into an Object[] array, because
        // This gets mapped to a tuple, when serializing to python.
        Object[] args = {arguments};

        if (arguments == null) {
            // Oh Java.
            // "array initializer not allowed here" for args = {}...
            Object[] _ = {};
            args = _;
        }

        if (socket == null) {
            signal("error", "You are not connected to a server.");
            return false;
        }
        if (viewonly && daemon.ACTIVE_COMMANDS.contains(command)) {
            signal("error", "Your client is set to view-only mode.");
            return false;
        }
        try {
            _write(command, args);
            TupleOfTwo<Byte, Object> response = _read();
            if (response.getFirst() == daemon.NAK) {
                // NAK means server responded with error code.
                throw new ErrorResponse((String) response.getSecond());
            }
            return true;
        }
        catch (Exception e) {
            handle_error(e);
            return false;
        }
    }

    public Object ask(String command, Object args) {
        // Since Java doesn't support default arguments (and of course not keywords), this acts
        // as a convenience method for ask(command, args, quiet=False).
        return ask(command, args, false);
    }

    public Object ask(String command, Object args, boolean quiet) {
        // Executes command with payload 'args'.
        // Returns the server's response payload.
        if (socket == null) {
            if (!quiet) {
                signal("error", "You are not connected to a server.");
            }
            return null;
        }

        else if (viewonly && daemon.ACTIVE_COMMANDS.contains(command)) {
            signal("error", "Your client is set to view-only mode.");
            return null;
        }
        try {
            _write(command, args);
            TupleOfTwo<Byte, Object> tuple = _read();
            if (tuple.getFirst() == daemon.NAK) {
                throw new ErrorResponse((String) tuple.getSecond());
            }
            return tuple.getSecond();
        }
        catch (Exception e) {
            handle_error(e);
            return null;
        }
    }

    public Object run(String code) {
        return run(code, "");
    }

    public Object run(String code, String filename) {
        // Run a piece of code.
        Object[] tuple = {filename, code};
        last_reqno = ask("queue", tuple);
        return last_reqno;
    }

    public Object eval(String expr) throws PythonException {
        return eval(expr, null, false);
    }

    public Object eval(String expr, boolean stringify) throws PythonException {
        return eval(expr, null, stringify);
    }

    public Object eval(String expr, Object default_) throws PythonException {
        return eval(expr, default_, false);
    }

    public Object eval(String expr, Object default_, boolean stringify) throws PythonException {
        // Evaluate a Python expression in the daemon's namespace and return the result.
        // If default is given and an exception occurs, default will be returned.
        // If default is not given and an exception occurs, the exception will be thrown for the
        // client to handle.
        Object[] tuple = {expr, stringify};
        Object result = ask("eval", tuple, true);
        if (result instanceof PythonException) {
            if (default_ != null) {
                return default_;
            }
            throw (PythonException) result;
        }
        return result;
    }

    // high-level functionality

    public List<String> getDeviceList(String needs_class, boolean only_explicit,
                                      String exclude_class, String special_clause) {
        // This method has WAY to many keywords for Java to implement all of the possibilities
        // via overloading.
        // This is the only way to call it.
        if (needs_class == null) {
            needs_class = "nicos.core.device.Device";
        }
        String query = "list(dn for (dn, d) in session.devices.items() if '" + needs_class +
                "' in d.classes";
        if (exclude_class != null) {
            query += " and '" + exclude_class + "' not in d.classes";
        }
        if (only_explicit) {
            query += " and dn in session.explicit_devices";
        }
        if (special_clause != null) {
            query += " and " + special_clause;
        }
        query += ")";
        ArrayList<Object> result = (ArrayList<Object>) eval(query, new ArrayList<Object>());
        ArrayList<String> deviceList = new ArrayList<String>();
        for (Object device : result) {
            deviceList.add(((String) device).toLowerCase());
        }
        Collections.sort(deviceList);
        return deviceList;
    }

    // Helper functions not existent in nicos-core/nicos/clients/base.py
    private MessageDigest getMD5() {
        try {
            return MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            return null;
        }
    }

    private PublicKey extractPublicKey(String source) {
        // Java wants the key formatted without prefix and postfix.
        String prefix = "-----BEGIN RSA PUBLIC KEY-----";
        String postfix = "\n-----END RSA PUBLIC KEY-----";

        // Java's string formatting/slicing is... 'slightly' inferior to python's.
        String keyNoPrefix = source.substring(prefix.length());
        String reversed = new StringBuilder(keyNoPrefix).reverse().toString();
        String reversedNoPostfix = reversed.substring(postfix.length());
        String keyString = new StringBuilder(reversedNoPostfix).reverse().toString();
        keyString = keyString.replace("\n", "");

        ASN1InputStream in = new ASN1InputStream(Base64.decode(keyString, Base64.NO_WRAP));
        ASN1Primitive obj = null;
        try {
            obj = in.readObject();
        } catch (IOException e) {
        }
        RSAPublicKeyStructure keyStructure = RSAPublicKeyStructure.getInstance(obj);
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(keyStructure.getModulus(),
                keyStructure.getPublicExponent());

        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        }
        catch (NoSuchAlgorithmException e) {
            // Cannot happen.
        }

        PublicKey pubkey = null;
        try {
            pubkey = keyFactory.generatePublic(keySpec);
        }
        catch (InvalidKeySpecException e) {
            // Cannot (SHOULD NOT) happen.
        }
        return pubkey;
    }

    public boolean isConnected() {
        return connected;
    }

    public HashMap getNicosBanner() {
        return nicosBanner;
    }

    public int getUserLevel() {
        return user_level;
    }

    public String getUniqueID() {
        long millis = System.currentTimeMillis();
        long seconds = millis / 1000l;
        long twoDigitsAfterComma = (millis - seconds * 1000l) / 10;
        String time = String.valueOf(seconds) + "." + String.valueOf(twoDigitsAfterComma);
        return time + String.valueOf(android.os.Process.myPid());
    }
}