package de.tum.frm2.nicos_android.nicos;

import java.util.HashMap;

public class Device {
    private String name;
    private String cacheName;
    private Object value;
    private int status;
    private HashMap<String, Object> params;

    public Device(String name, String cacheName) {
        this.name = name;
        this.cacheName = cacheName;
        value = null;
        status = -1;
        params = new HashMap<String, Object>();
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

    public Object getParam(String key) {
        if (!params.containsKey(key)) {
            return null;
        }
        return params.get(key);
    }

    public void addParam(String key, Object param) {
        params.put(key, param);
    }
}
