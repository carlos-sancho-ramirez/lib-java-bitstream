package sword.bitstream;

import java.util.Iterator;

public class NaturalNumberHuffmanTable implements HuffmanTable<Long> {

    private static boolean isValidLevel(int level) {
        return level >= 7 && (level % 8 == 7);
    }

    private static int getSymbolsAtLevel(int level) {
        return 1 << (((level + 1) >> 3) * 7);
    }

    @Override
    public int symbolsAtLevel(int level) {
        return isValidLevel(level)? getSymbolsAtLevel(level) : 0;
    }

    private static long getBaseFromLevel(int level) {
        long base = 0;
        int exp = level >> 3;
        while (exp > 0) {
            base += 1 << (exp * 7);
            exp--;
        }

        return base;
    }

    @Override
    public Long getSymbol(int level, int index) {
        if (!isValidLevel(level)) {
            throw new IllegalArgumentException();
        }

        return getBaseFromLevel(level) + index;
    }

    private static class LevelIterator implements Iterator<Long> {

        private final long _base;
        private final long _lastLevelSymbol;

        private long _next;

        LevelIterator(int level) {
            _base = getBaseFromLevel(level);
            _lastLevelSymbol = _base + getSymbolsAtLevel(level) - 1;
            _next = _base;
        }

        @Override
        public boolean hasNext() {
            return _next <= _lastLevelSymbol;
        }

        @Override
        public Long next() {
            return _next++;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static class LevelIterable implements Iterable<Long> {

        private final int _level;

        LevelIterable(int level) {
            _level = level;
        }

        @Override
        public Iterator<Long> iterator() {
            return new LevelIterator(_level);
        }
    }

    private static final Iterator<Long> _invalidLevelIterator = new Iterator<Long>() {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Long next() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    };

    private static final Iterable<Long> _invalidLevelIterable = new Iterable<Long>() {

        @Override
        public Iterator<Long> iterator() {
            return _invalidLevelIterator;
        }
    };

    private static class TableIterator implements Iterator<Iterable<Long>> {

        private int _level;

        @Override
        public boolean hasNext() {
            return true;
        }

        @Override
        public Iterable<Long> next() {
            final int level = _level++;
            return isValidLevel(level)? new LevelIterable(level) : _invalidLevelIterable;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Iterator<Iterable<Long>> iterator() {
        return new TableIterator();
    }
}
