package sword.bitstream;

import java.util.*;

public final class DefinedHuffmanTable<E> implements HuffmanTable<E> {
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

    DefinedHuffmanTable(Object[][] table) {
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

    public DefinedHuffmanTable(Iterable<Iterable<E>> table) {
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
            if (other == null || !(other instanceof DefinedHuffmanTable.LevelIterable)) {
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
        if (other == null || !(other instanceof DefinedHuffmanTable)) {
            return false;
        }

        final DefinedHuffmanTable that = (DefinedHuffmanTable) other;
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

    @Override
    public String toString() {
        final StringBuilder str = new StringBuilder("\n");
        final int levels = _table.length;
        for (int i = 0; i < levels; i++) {
            final int levelLength = _table[i].length;
            str.append("[");
            for (int j = 0; j < levelLength; j++) {
                str.append("" + _table[i][j]);
                if (j < levelLength - 1) {
                    str.append(", ");
                }
            }
            str.append("]\n");
        }

        return str.toString();
    }

    @Override
    public int symbolsAtLevel(int level) {
        return _table[level].length;
    }

    @Override
    public E getSymbol(int level, int index) {
        return (E) _table[level][index];
    }

    private abstract static class Node<E> {
        final int frequency;

        Node(int frequency) {
            this.frequency = frequency;
        }

        abstract void fillSymbolLengthMap(Map<E, Integer> map, int depth);
    }

    private static class Leaf<E> extends Node<E> {
        final E symbol;

        Leaf(E symbol, int frequency) {
            super(frequency);
            this.symbol = symbol;
        }

        @Override
        void fillSymbolLengthMap(Map<E, Integer> map, int depth) {
            map.put(symbol, depth);
        }
    }

    private static class InnerNode<E> extends Node<E> {
        final Node<E> left;
        final Node<E> right;

        InnerNode(Node<E> left, Node<E> right) {
            super(left.frequency + right.frequency);
            this.left = left;
            this.right = right;
        }

        @Override
        void fillSymbolLengthMap(Map<E, Integer> map, int depth) {
            left.fillSymbolLengthMap(map, depth + 1);
            right.fillSymbolLengthMap(map, depth + 1);
        }
    }

    public static <E> DefinedHuffmanTable<E> withFrequencies(Map<E, Integer> frequency) {
        final Set<Node<E>> set = new HashSet<>(frequency.size());

        for (Map.Entry<E, Integer> entry : frequency.entrySet()) {
            set.add(new Leaf<>(entry.getKey(), entry.getValue()));
        }

        while (set.size() > 1) {
            final Iterator<Node<E>> it = set.iterator();
            Node<E> min1 = it.next();
            Node<E> min2 = it.next();
            if (min2.frequency < min1.frequency) {
                Node<E> temp = min1;
                min1 = min2;
                min2 = temp;
            }

            while (it.hasNext()) {
                Node<E> next = it.next();
                if (next.frequency < min1.frequency) {
                    min2 = min1;
                    min1 = next;
                }
                else if (next.frequency < min2.frequency) {
                    min2 = next;
                }
            }

            set.remove(min1);
            set.remove(min2);
            set.add(new InnerNode<>(min2, min1));
        }

        final Node<E> masterNode = set.iterator().next();
        final Map<E, Integer> symbolLengthMap = new HashMap<>();
        masterNode.fillSymbolLengthMap(symbolLengthMap, 0);

        int bits = 0;
        final ArrayList<Iterable<E>> table = new ArrayList<>();
        while (symbolLengthMap.size() > 0) {
            bits++;
            final ArrayList<E> level = new ArrayList<>();
            final Iterator<Map.Entry<E, Integer>> it = symbolLengthMap.entrySet().iterator();
            while (it.hasNext()) {
                final Map.Entry<E, Integer> entry = it.next();
                if (entry.getValue() == bits) {
                    level.add(entry.getKey());
                    it.remove();
                }
            }
            table.add(level);
        }

        return new DefinedHuffmanTable<>(table);
    }

    public static <E> DefinedHuffmanTable<E> from(Iterable<E> symbols) {
        final Map<E, Integer> frequency = new HashMap<>();
        for (E symbol : symbols) {
            final Integer mapValue = frequency.get(symbol);
            final int newValue = 1 + ((mapValue != null)? mapValue : 0);
            frequency.put(symbol, newValue);
        }

        return withFrequencies(frequency);
    }
}
