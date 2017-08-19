package sword.bitstream;

import java.util.*;

/**
 * Version of finite Huffman table that can be encoded within the stream.
 *
 * This Huffman table is exhaustive, which means that there is no combination of bits
 * that is not included on it.
 *
 * This Huffman table is finite, which means that there is a maximum amount of bits
 * defined for the encoded symbols. Thus, on iterating, there is always end.
 *
 * @param <E> Type of the symbol to encode or decode.
 */
public final class DefinedHuffmanTable<E> implements HuffmanTable<E> {
    private final Object[][] _table;
    private transient int _hashCode;

    // TODO: Check that there is not repeated symbols
    private void assertExhaustiveTable() {
        final int tableLength = _table.length;

        if (_table[0].length == 0) {
            int remain = 1;
            for (int i = 1; i < tableLength; i++) {
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
        else if (_table[0].length > 1) {
            throw new IllegalArgumentException("Impossible to have more than one symbol for 0 bits");
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

    /**
     * Create a DefinedHuffmanTable resulting of iterating over the given structure of symbols.
     *
     * This is a complex method and should be avoided.
     * Try using {@link #withFrequencies(Map)} or {@link #from(Iterable)} instead.
     *
     * It is expected here that the given table has its symbols sorted from most probable to less
     * probable in order to ensure an optimal encoding.
     *
     * It is also expected that the symbols within the iterable are not repeated.
     *
     * It is also expected that the given iterable is finite.
     *
     * @param table An {@link java.lang.Iterable} of iterable of symbols.
     *              The main iterable must contain all symbols grouped for the number of
     *              bits that each symbol should use when encoded depending on its
     *              appearing frequency. The order on each of the sub iterable is
     *              irrelevant in terms of optimizations.
     *
     * @see #withFrequencies(Map)
     * @see #from(Iterable)
     */
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
            return Arrays.hashCode(_level);
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof DefinedHuffmanTable.LevelIterable)) {
                return false;
            }

            final HuffmanLevelIterator thisIt = iterator();
            final HuffmanLevelIterator thatIt = ((LevelIterable) other).iterator();
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
        if (_hashCode == 0) {
            final int length = _table.length;
            final int[] hashes = new int[length];

            for (int i = 0; i < length; i++) {
                hashes[i] = Arrays.hashCode(_table[i]);
            }

            _hashCode = Arrays.hashCode(hashes);
        }

        return _hashCode;
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
            if (!it.hasNext() || !iterable.equals(it.next())) {
                return false;
            }
        }

        return !it.hasNext();
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
    public int symbolsWithBits(int bits) {
        return _table[bits].length;
    }

    @Override
    public E getSymbol(int bits, int index) {
        return (E) _table[bits][index];
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

        @Override
        public int hashCode() {
            return (symbol != null)? symbol.hashCode() : 0;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof Leaf)) {
                return false;
            }

            Leaf that = (Leaf) other;
            return frequency == that.frequency && (
                    symbol == null && that.symbol == null ||
                    symbol != null && symbol.equals(that.symbol));
        }
    }

    private static class InnerNode<E> extends Node<E> {
        final Node<E> left;
        final Node<E> right;
        private int hashCode;

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

        @Override
        public int hashCode() {
            if (hashCode == 0) {
                hashCode = (left.hashCode() * 17) ^ right.hashCode();
            }

            return hashCode;
        }

        @Override
        public boolean equals(Object other) {
            if (other == null || !(other instanceof InnerNode)) {
                return false;
            }

            InnerNode that = (InnerNode) other;
            return left.equals(that.left) && right.equals(that.right);
        }
    }

    /**
     * Build a {@link DefinedHuffmanTable} based on the given map of frequencies.
     * This method will take for the map the most probable symbols and will assign
     * to them less bits when encoding, leaving the less probable symbols with the
     * biggest amount of bits. This will ensure less amount of data to be written
     * or read from stream, compressing the data.
     *
     * @param frequency Map of frequencies.
     *                  Key of this map are the symbols to be encoded or decoded.
     *                  Values of this map are the number or times this symbol is usually found.
     *                  The bigger the value of a symbol the less probable it is.
     *                  Values on the map must be all positive numbers. Zero is also not allowed.
     * @param <E> Type of the symbol to encode.
     *            It is strongly recommended that this type has a proper {@link Object#hashCode}
     *            method implemented. That will ensure that the table will be always the same
     *            for the same frequency map.
     * @return A DefinedHuffmanTable optimized for the map of frequencies given.
     */
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
            bits++;
        }

        return new DefinedHuffmanTable<>(table);
    }

    /**
     * Build a {@link DefinedHuffmanTable} using the given symbol array as base.
     *
     * This method builds a map of frequencies counting all the symbols found and
     * call {@link #withFrequencies(Map)} in order to build the map.
     *
     * @param symbols Array of symbols from where the map of frequencies will be extracted.
     *                Thus, this map should contain a good sample of the kind of data to be
     *                compressed in an optimal way, or the whole data if it can fit in memory.
     * @param <E> Type of the symbol to encode
     * @return A new {@link DefinedHuffmanTable} instance.
     */
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
