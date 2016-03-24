package de.tum.frm2.nicos_android.errors;

import net.razorvine.pickle.IObjectConstructor;
import net.razorvine.pickle.PickleException;

public class InvalidValueErrorConstructor implements IObjectConstructor {
    @Override
    public Object construct(Object[] args) throws PickleException {
        return new InvalidValueError((String) args[0]);
    }
}
