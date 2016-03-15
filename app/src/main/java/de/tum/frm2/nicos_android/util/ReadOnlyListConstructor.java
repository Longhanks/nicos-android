package de.tum.frm2.nicos_android.util;

import net.razorvine.pickle.IObjectConstructor;
import net.razorvine.pickle.PickleException;


public class ReadOnlyListConstructor implements IObjectConstructor {
    public ReadOnlyListConstructor() {
    }

    @Override
    public Object construct(Object[] args) throws PickleException {
        return new ReadOnlyList();
    }
}
