package nl.tue.pdc.concurrent;

public interface WriteSet<E> {

    boolean add(E e);
    int size();
}
