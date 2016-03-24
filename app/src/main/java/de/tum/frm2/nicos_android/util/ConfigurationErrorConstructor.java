package de.tum.frm2.nicos_android.util;

import net.razorvine.pickle.IObjectConstructor;
import net.razorvine.pickle.PickleException;

import java.util.HashMap;

public class ConfigurationErrorConstructor implements IObjectConstructor {
    @Override
    public Object construct(Object[] args) throws PickleException {
        return new ConfigurationError((String) args[0]);
    }
}
