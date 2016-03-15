package de.tum.frm2.nicos_android.nicos;

public class Device {
    private String name;
    private String cacheName;
    private Object value;
    private int status;

    public Device(String name, String cacheName) {
        this.name = name;
        this.cacheName = cacheName;
        value = null;
        status = -1;
    }

    public String getName() {
        return name;
    }

    public String getCacheName() {
        return cacheName;
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public int getStatus() {
        return status;
    }

    public void setStatus(int status) {
        this.status = status;
    }
}
