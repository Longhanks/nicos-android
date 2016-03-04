package de.tum.frm2.nicos_android;

import android.content.Context;
import android.os.Handler;
import android.os.Message;
import android.util.Base64;

import net.razorvine.pickle.Pickler;
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
import java.util.Arrays;
import java.util.HashMap;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;

public class NicosClient {
    // Resembles nicos-core/nicos/clients/base.py
    private static NicosClient client;
    private Socket socket = null;
    private OutputStream socketOut;
    private InputStream socketIn;
    private HashMap nicosBanner;
    private int user_level;

    private NicosClient() {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    public static NicosClient getClient() {
        if (client == null) {
            client = new NicosClient();
        }
        return client;
    }

    public void disconnect() {
        try {
            socket.close();
        }
        catch (Exception e) {
            // Socket already disconnected.
        }
    }

    public void connect(ConnectionData connData) throws RuntimeException {
        try {
            InetAddress addr = InetAddress.getByName(connData.getHost());
            SocketAddress sockaddr = new InetSocketAddress(addr, connData.getPort());

            // Initialize empty socket.
            socket = new Socket();
            int timeout = 30000;

            // Connects this socket to the server with a specified timeout value
            // If timeout occurs, SocketTimeoutException is thrown
            socket.connect(sockaddr, timeout);
        }
        catch (UnknownHostException e) {
            throw new RuntimeException("Host not found.");
        }
        catch (IOException ioe) {
            throw new RuntimeException("I/O Error: " + ioe.getMessage());
        }

        try {
            socketOut = socket.getOutputStream();
            socketIn = socket.getInputStream();
        } catch (IOException e) {
            throw new RuntimeException("I/O Error: " + e.getMessage());
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
            socketOut.flush();
        }
        catch (IOException e) {
            throw new RuntimeException("I/O Error: " + e.getMessage());
        }

        // read banner
        TupleOfTwo<Byte, Object> response = _read();
        byte start = response.getFirst();
        nicosBanner = (HashMap) response.getSecond();

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
        if (untypedAuthResponse.getClass() == String.class) {
            // Credentials not accepted.
            throw new RuntimeException("Server didn't accept username/password.");
        }

        // Login was successful.
        HashMap authResponse = (HashMap) untypedAuthResponse;
        user_level = (int) authResponse.get("user_level");
    }

    public Object ask(String command, Object args) {
        // Executes command with payload 'args'.
        // Returns the server's response payload.
        _write(command, args);
        TupleOfTwo<Byte, Object> tuple = _read();
        return tuple.getSecond();
    }

    public void _write(String command, Object args) {
        // Data to send:
        // ENQ + commandcode + length + payload
        Pickler pickler = new Pickler();
        byte[] data;
        try {
            data = pickler.dumps(args);
        } catch (IOException e) {
            data = new byte[0];
            System.out.println("I/O Error: " + e.getMessage());
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        buffer.write(daemon.ENQ);
        // frame format requires: length of (ENQ + commandcode) == 3 bytes
        // That's why an empty byte gets inserted here.
        buffer.write((byte) 0x00);
        buffer.write(daemon.command2code(command));
        ByteBuffer bb = ByteBuffer.allocate(4);
        bb.order(ByteOrder.BIG_ENDIAN);
        bb.putInt(data.length);
        try {
            buffer.write(bb.array());
            buffer.write(data);
        } catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
        }
        try {
            socketOut.write(buffer.toByteArray());
            socketOut.flush();
        } catch (IOException e) {
            System.out.println("I/O Error: " + e.getMessage());
        }

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
            System.out.println("I/O Error: " + e.getMessage());
        }
        RSAPublicKeyStructure keyStructure = RSAPublicKeyStructure.getInstance(obj);
        RSAPublicKeySpec keySpec = new RSAPublicKeySpec(keyStructure.getModulus(),
                keyStructure.getPublicExponent());

        KeyFactory keyFactory = null;
        try {
            keyFactory = KeyFactory.getInstance("RSA");
        }
        catch (NoSuchAlgorithmException e) {
            System.out.println("No RSA algorithm.");
            // Cannot happen.
        }

        PublicKey pubkey = null;
        try {
            pubkey = keyFactory.generatePublic(keySpec);
        }
        catch (InvalidKeySpecException e) {
            System.out.println("Decrypting RSA key failed.");
            System.out.println(e.getMessage());
        }
        return pubkey;
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