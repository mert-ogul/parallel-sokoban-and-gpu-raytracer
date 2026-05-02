package nl.tue.pdc.concurrent;

import java.util.Collection;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ConcurrentHashSet<E> implements Set<E>, WriteSet<E> {

    private final transient ConcurrentHashMap<E, Object> map;
    static final Object PRESENT = new Object();

    public ConcurrentHashSet() {
        this(16);
    }

    public ConcurrentHashSet(int initialCapacity) {
        this(initialCapacity, 1);
    }

    public ConcurrentHashSet(int initialCapacity, int concurrencyLevel) {
        map = new ConcurrentHashMap<>(initialCapacity, 0.75f, concurrencyLevel);
    }

    @Override
    public int size() {
        return map.size();
    }

    @Override
    public boolean isEmpty() {
        return map.isEmpty();
    }

    @Override
    public boolean contains(Object o) {
        return map.containsKey(o);
    }

    @Override
    public Iterator<E> iterator() {
        return map.keySet().iterator();
    }

    @Override
    public Object[] toArray() {
        return map.keySet().toArray();
    }

    @Override
    public <T> T[] toArray(T[] ts) {
        return map.keySet().toArray(ts);
    }

    @Override
    public boolean add(E e) {
        return map.put(e, PRESENT) == null;
    }

    @Override
    public boolean remove(Object o) {
        return this.map.remove(o) == PRESENT;
    }

    @Override
    public boolean containsAll(Collection<?> collection) {
        return map.keySet().containsAll(collection);
    }

    @Override
    public boolean addAll(Collection<? extends E> collection) {
        boolean changed = false;
        for (E e : collection) {
            changed |= add(e);
        }
        return changed;
    }

    @Override
    public boolean retainAll(Collection<?> collection) {
        boolean changed = false;
        for (E e : map.keySet()) {
            if (!collection.contains(e)) {
                changed |= remove(e);
            }
        }
        return changed;
    }

    @Override
    public boolean removeAll(Collection<?> collection) {
        boolean changed = false;
        for (Object o : collection) {
            changed |= remove(o);
        }
        return changed;
    }

    @Override
    public void clear() {
        this.map.clear();
    }
}
