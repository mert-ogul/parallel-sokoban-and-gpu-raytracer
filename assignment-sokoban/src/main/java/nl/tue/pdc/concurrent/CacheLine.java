package nl.tue.pdc.concurrent;

import java.util.NoSuchElementException;
import java.util.PrimitiveIterator;

public final class CacheLine implements Iterable<Integer> {
    public static final int CACHE_LINE = 3;
    public static final int CACHE_LINE_SIZE = 1 << CACHE_LINE;
    private final int start;

    private CacheLine(int start) {
        this.start = start;
    }

    public static CacheLine walk(int start) {
        return new CacheLine(start);
    }

    @Override
    public PrimitiveIterator.OfInt iterator() {
        return new CacheLineIterator(start);
    }

    private static class CacheLineIterator implements PrimitiveIterator.OfInt {
        private final int start;
        private final int bucketStart;
        private int count;

        public CacheLineIterator(int start) {
            this.bucketStart = start / CACHE_LINE_SIZE * CACHE_LINE_SIZE;
            this.start = start - bucketStart;
            this.count = 0;
        }

        @Override
        public boolean hasNext() {
            return count < CACHE_LINE_SIZE;
        }

        @Override
        public int nextInt() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }
            return bucketStart + (start + count++) % CACHE_LINE_SIZE;
        }
    }
}
