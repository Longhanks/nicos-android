package de.tum.frm2.nicos_android.nicos;

import java.util.ArrayList;
import java.util.HashMap;

import de.tum.frm2.nicos_android.util.ReadOnlyList;

public class Device {
    private String name;
    private String cacheName;
    private Object value;
    private int status;
    private Class valuetype;
    private HashMap<String, Object> params;

    public Device(String name, String cacheName) {
        this.name = name;
        this.cacheName = cacheName;
        value = null;
        status = -1;
        valuetype = null;
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

    public String getFormattedValue() {
        String fmt = (String) getParam("fmtstr");
        String unit = (String) getParam("unit");

        if (unit == null) {
            unit = "";
        } else {
            unit = " " + unit;
        }

        if (value != null) {
            String formatted;

            try {
                if (valuetype == Integer.class) {
                    // int
                    formatted = String.format(fmt, (int) value);
                }

                else if (valuetype == Double.class) {
                    // float is double after depickling
                    formatted = String.format(fmt, (double) value);
                }

                else if (valuetype == ReadOnlyList.class ||
                        valuetype == ArrayList.class ||
                        valuetype == Object[].class) {
                    // list, tuple (Nicos tuplifies all lists; I'm doing "the same")
                    formatted = "";
                    Object[] tuple;
                    if (valuetype == Object[].class) {
                        tuple = (Object[]) value;
                    }
                    else {
                        tuple = ((ArrayList) value).toArray();
                    }
                    try {
                        // Try to append the format using fmtstring.
                        String formattedList = String.format(fmt, tuple);
                        formatted += formattedList;
                    }
                    catch (Exception e) {
                        // Formatting failed.
                        formatted = ""; // resets String if previous attempt left over stuff
                        for (int i = 0; i < tuple.length; ++i) {
                            try {
                                String obj = tuple[i].toString();
                                if (obj.isEmpty()) {
                                    obj = "''";
                                }
                                formatted += obj;
                                if (i + 1 != tuple.length) {
                                    formatted += ", ";
                                }
                            }
                            catch (Exception e2) {
                                // Tuple contains null or some garbage.
                                formatted += "''";
                                if (i + 1 != tuple.length) {
                                    formatted += ", ";
                                }                            }
                        }
                    }
                }

                else {
                    throw new RuntimeException("Unkown class");
                }
            }
            catch (Exception e) {
                formatted = value.toString();
            }
            return formatted + unit;
        }
        return "None";
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public void setValueFromCache(String cachevalue) {
        if (valuetype == Object[].class) {
            cachevalue = cachevalue.substring(1, cachevalue.length() - 1);
            if (cachevalue.endsWith(",")) {
                cachevalue = cachevalue.substring(0, cachevalue.length() - 1);
            }
            String[] splits = cachevalue.split(",");
            Object[] newval = new Object[splits.length];
            for (int i = 0; i < splits.length; ++i) {
                Object val;
                try {
                    val = Integer.parseInt(splits[i]);
                    newval[i] = val;
                    continue;
                } catch (NumberFormatException e) {
                }
                try {
                    val = Double.parseDouble(splits[i]);
                    newval[i] = val;
                    continue;
                } catch (NumberFormatException e) {
                }
                newval[i] = splits[i].toString();
            }
            value = newval;
        }
        else if (valuetype == Double.class) {
            value = Double.parseDouble(cachevalue);
        }
        else {
            value = cachevalue;
        }
    }

    public int getStatus() {
        return status;
    }

    public Class getValuetype() {
        return valuetype;
    }

    public void setValuetype(Class valuetype) {
        this.valuetype = valuetype;
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
