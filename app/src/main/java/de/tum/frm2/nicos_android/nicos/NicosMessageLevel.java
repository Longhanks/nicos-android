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
