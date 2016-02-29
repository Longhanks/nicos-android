package de.tum.frm2.nicos_android;

import android.content.Context;
import android.os.Handler;
import android.os.Message;

import net.razorvine.pickle.Unpickler;

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
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;

public class NicosClient {
    // Resembles nicos-core/nicos/clients/base.py
    private Socket socket;
    private OutputStream socketOut;
    private InputStream socketIn;
    private Handler callbackHandler;
    private HashMap nicosBanner;

    public NicosClient(Handler callbackHandler, final ConnectionData conndata) {
        this.callbackHandler = callbackHandler;
        // Android forbids networking in main thread. That's why a thread gets started here.
        new Thread(new Runnable() {
            @Override
            public void run() {
                connect(conndata);
            }
        }).start();
    }

    public void connect(ConnectionData conndata) {
        try {
            InetAddress addr = InetAddress.getByName(conndata.getHost());
            SocketAddress sockaddr = new InetSocketAddress(addr, conndata.getPort());

            // Initialize empty socket.
            socket = new Socket();
            int timeout = 30000;

            // Connects this socket to the server with a specified timeout value
            // If timeout occurs, SocketTimeoutException is thrown
            socket.connect(sockaddr, timeout);
            Message msg = callbackHandler.obtainMessage(NicosClientMessages.CONNECTION_SUCCESSFUL,
                                                        "Connected to: " + conndata.getHost());
            msg.sendToTarget();
        }
        catch (UnknownHostException e) {
            System.out.println("Host not found: " + e.getMessage());
        }
        catch (IOException ioe) {
            System.out.println("I/O Error: " + ioe.getMessage());
        }

        try {
            socketOut = socket.getOutputStream();
            socketIn = socket.getInputStream();
        } catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
        }

        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("md5");
        } catch (NoSuchAlgorithmException e) {
            // Cannot occur.
        }

        byte[] client_id = digest.digest(getUniqueID().getBytes());
        try {
            // Write client identification: we are a new client
            socketOut.write(client_id);
        }
        catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
        }

        // read banner
        TupleOfTwo<Byte, Object> response = _read();
        nicosBanner = (HashMap) response.getSecond();
    }

    public TupleOfTwo<Byte, Object> _read() {
        // receive first byte + (possibly) length
        byte[] start = new byte[5];
        try {
            socketIn.read(start);
        }
        catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
        }
        byte[] slice = Arrays.copyOfRange(start, 1, 5);
        ByteBuffer bb = ByteBuffer.wrap(slice);
        bb.order(ByteOrder.BIG_ENDIAN);
        int length = bb.getInt();

        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        while (buf.size() < length) {
            byte[] read = new byte[8192];
            try {
                socketIn.read(read);
            } catch (IOException e) {
                System.out.println("I/O Error: " + e.getMessage());
            }
            try {
                buf.write(read);
            } catch (IOException e) {
                System.out.println("I/O Error: " + e.getMessage());
            }
        }
        Unpickler unpickler = new Unpickler();
        Object result = null;
        try {
            result = unpickler.loads(buf.toByteArray());
        } catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
        }

        return new TupleOfTwo<Byte, Object>(start[0], result);
    }

    public HashMap getNicosBanner() {
        return nicosBanner;
    }

    public String getUniqueID() {
        long millis = System.currentTimeMillis();
        long seconds = millis / 1000l;
        long twoDigitsAfterComma = (millis - seconds * 1000l) / 10;
        String time = String.valueOf(seconds) + "." + String.valueOf(twoDigitsAfterComma);
        return time + String.valueOf(android.os.Process.myPid());
    }
}

class TupleOfTwo<T1, T2> {
    private T1 t1;
    private T2 t2;

    public TupleOfTwo(T1 t1, T2 t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    public T1 getFirst() {
        return t1;
    }

    public T2 getSecond() {
        return t2;
    }
}