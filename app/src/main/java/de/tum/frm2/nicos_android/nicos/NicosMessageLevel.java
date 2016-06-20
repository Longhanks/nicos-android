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

public class NicosMessageLevel {
    // From Python logging levels
    public static final int CRITICAL = 50;
    public static final int ERROR = 40;
    public static final int WARNING = 30;
    public static final int INFO = 20;
    public static final int DEBUG = 10;
    public static final int NOTSET = 0;

    // From nicos/utils/loggers.py
    public static final int ACTION = INFO + 1;
    public static final int INPUT = INFO + 6;

    public static String level2name(int level) {
        switch (level) {
            case CRITICAL:
                return "CRITICAL";
            case ERROR:
                return "ERROR";
            case WARNING:
                return "WARNING";
            case INFO:
                return "INFO";
            case DEBUG:
                return "DEBUG";
            case NOTSET:
                return "NOTSET";
            case ACTION:
                return "ACTION";
            case INPUT:
                return "INPUT";
            default:
                return "UNKOWN";
        }
    }
}
