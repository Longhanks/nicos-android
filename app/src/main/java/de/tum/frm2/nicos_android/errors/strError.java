package de.tum.frm2.nicos_android.errors;


import java.util.HashMap;

public class strError {
    private String message;

    public strError(String message) {
        this.message = message;
    }

    public void __setstate__(HashMap<String, Object> values) {

    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "strError";
    }
}
