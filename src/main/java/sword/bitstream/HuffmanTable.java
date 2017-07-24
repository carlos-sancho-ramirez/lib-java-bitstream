package sword.bitstream;

import java.util.ArrayList;
import java.util.Iterator;

public final class HuffmanTable<E> implements Iterable<Iterable<E>> {
    private final Object[][] _table;

    // TODO: Check that there is not repeated symbols
    private void assertExhaustiveTable() {
        final int tableLength = _table.length;
        int remain = 1;
        for (int i = 0; i < tableLength; i++) {
            remain <<= 1;

            int thisLength = _table[i].length;
            remain -= thisLength;
            if (remain <= 0 && i != tableLength - 1) {
                throw new IllegalArgumentException("Found symbols in the tree that never will be used");
            }
        }

        if (remain != 0) {
            throw new IllegalArgumentException("Provided tree is not exhaustive");
        }
    }

    HuffmanTable(Object[][] table) {
        _table = table;
        assertExhaustiveTable();
    }

    private static <U> Object[][] iterableToArray(Iterable<Iterable<U>> table) {
        ArrayList<Object[]> middle = new ArrayList<>();

        for (Iterable iterable : table) {
            ArrayList list = new ArrayList<>();
            for (Object element : iterable) {
                list.add(element);
            }

            middle.add(list.toArray());
        }

        return middle.toArray(new Object[0][]);
    }

    public HuffmanTable(Iterable<Iterable<E>> table) {
        this(iterableToArray(table));
    }

    private class HuffmanLevelIterator implements Iterator<E> {
        private final Object[] _level;
        private int _index;

        private HuffmanLevelIterator(Object[] level) {
            _level = level;
        }

        @Override
        public boolean hasNext() {
            return _index < _level.length;
        }

        @Override
        public E next() {
            return (E) _level[_index++];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private class LevelIterable implements Iterable<E> {

        private final Object[] _level;

        private LevelIterable(int tableIndex) {
            _level = _table[tableIndex];
        }

        @Override
        public HuffmanLevelIterator iterator() {
            return new HuffmanLevelIterator(_level);
        }

        @Override
        public int hashCode() {
            return _level.length;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof HuffmanTable.LevelIterable)) {
                return false;
            }

            final HuffmanLevelIterator thisIt = iterator();
            final Iterator thatIt = ((LevelIterable) other).iterator();
            while (thisIt.hasNext()) {
                if (!thatIt.hasNext()) {
                    return false;
                }

                final Object thisObj = thisIt.next();
                final Object thatObj = thatIt.next();
                if (thisObj == null && thatObj != null || thisObj != null && !thisObj.equals(thatObj)) {
                    return false;
                }
            }

            return !thatIt.hasNext();
        }
    }

    private class TableIterator implements Iterator<Iterable<E>> {

        private int _index;

        @Override
        public boolean hasNext() {
            return _index < _table.length;
        }

        @Override
        public Iterable<E> next() {
            return new LevelIterable(_index++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    @Override
    public Iterator<Iterable<E>> iterator() {
        return new TableIterator();
    }

    @Override
    public int hashCode() {
        return _table.length;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof HuffmanTable)) {
            return false;
        }

        final HuffmanTable that = (HuffmanTable) other;
        if (_table.length != that._table.length) {
            return false;
        }

        final Iterator<Iterable> it = that.iterator();
        for (Iterable<E> iterable : this) {
            if (!iterable.equals(it.next())) {
                return false;
            }
        }

        return true;
    }

    int symbolsAtLevel(int level) {
        return _table[level].length;
    }

    E getSymbol(int level, int index) {
        return (E) _table[level][index];
    }
}
