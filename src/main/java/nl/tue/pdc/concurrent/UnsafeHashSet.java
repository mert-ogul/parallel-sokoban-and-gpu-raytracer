package nl.tue.pdc.concurrent;

import java.util.HashSet;

public class UnsafeHashSet<E> extends HashSet<E> implements WriteSet<E> {
    public UnsafeHashSet(int initialCapacity) {
        super(initialCapacity);
    }
}
