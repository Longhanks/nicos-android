package de.tum.frm2.nicos_android;

class TupleOfTwo<T1, T2> {
    private T1 t1;
    private T2 t2;

    public TupleOfTwo(T1 t1, T2 t2) {
        this.t1 = t1;
        this.t2 = t2;
    }

    public T1 getFirst() {
        return t1;
    }

    public T2 getSecond() {
        return t2;
    }
}