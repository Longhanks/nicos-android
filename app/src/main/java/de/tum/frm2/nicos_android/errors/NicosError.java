package de.tum.frm2.nicos_android.errors;


import java.util.HashMap;

public class NicosError {
    private String message;

    public NicosError(String message) {
        this.message = message;
    }

    public void __setstate__(HashMap<String, Object> values) {

    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "NicosError";
    }
}
