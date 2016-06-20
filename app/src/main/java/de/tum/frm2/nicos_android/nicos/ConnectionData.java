//
// Copyright (C) 2016 Andreas Schulz <andreas.schulz@frm2.tum.de>
//
// This program is free software; you can redistribute it and/or modify
// it under the terms of the GNU General Public License as published by
// the Free Software Foundation; either version 2 of the License, or
// (at your option) any later version.
//
// This program is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// GNU General Public License for more details.
//
// You should have received a copy of the GNU General Public License
// along with this program; if not, write to the Free Software
// Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 US


package de.tum.frm2.nicos_android.nicos;


import java.io.Serializable;

@SuppressWarnings("unused")
public class ConnectionData implements Serializable {
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
