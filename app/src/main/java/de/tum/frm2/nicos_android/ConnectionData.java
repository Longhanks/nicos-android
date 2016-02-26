package de.tum.frm2.nicos_android;


public class ConnectionData {
    private String host;
    private int port;
    private String user;
    private char[] password;
    private boolean viewonly;

    public ConnectionData(String host,
                          int port,
                          String user,
                          char[] password,
                          boolean viewonly) {
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.viewonly = viewonly;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public char[] getPassword() {
        return password;
    }

    public void setPassword(char[] password) {
        this.password = password;
    }

    public boolean getViewonly() {
        return viewonly;
    }

    public void setViewonly(boolean viewonly) {
        this.viewonly = viewonly;
    }
}
