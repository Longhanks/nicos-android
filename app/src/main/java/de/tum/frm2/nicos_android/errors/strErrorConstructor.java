package de.tum.frm2.nicos_android.errors;

import net.razorvine.pickle.IObjectConstructor;
import net.razorvine.pickle.PickleException;

public class strErrorConstructor implements IObjectConstructor {
    @Override
    public Object construct(Object[] args) throws PickleException {
        return new strError((String) args[0]);
    }
}
