package de.tum.frm2.nicos_android.util;

import net.razorvine.pickle.IObjectConstructor;
import net.razorvine.pickle.PickleException;
import net.razorvine.pickle.objects.ClassDictConstructor;

import java.util.HashMap;

/**
 * Created by aschulz on 17.03.16.
 */
public class ReadOnlyDictConstructor implements IObjectConstructor {

    @Override
    public Object construct(Object[] args) throws PickleException {
        return new HashMap();
    }

    public Object reconstruct(Object o1, Object o2) {
        return o2;
    }
}
