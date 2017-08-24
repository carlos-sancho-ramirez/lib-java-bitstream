package sword.bitstream;

import java.util.*;

/**
 * Version of finite Huffman table that can be encoded within the stream.
 * <p>
 * This Huffman table is exhaustive, which means that there is no combination of bits
 * that is not included on it.
 * <p>
 * This Huffman table is finite, which means that there is a maximum amount of bits
 * defined for the encoded symbols. Thus, on iterating, there is always end.
 *
 * @param <E> Type of the symbol to encode or decode.
 */
public final class DefinedHuffmanTable<E> implements HuffmanTable<E> {
    private final int[] _levelIndexes;
    private final Object[] _symbols;
    private transient int _hashCode;

    // TODO: Check that there is not repeated symbols
    private void assertExhaustiveTable() {
        final int levelsLength = _levelIndexes.length;

        if (_levelIndexes.length > 0) {
            int remain = 1;
            for (int i = 1; i < levelsLength + 1; i++) {
                remain <<= 1;

                int thisLength = symbolsWithBits(i);
                remain -= thisLength;
                if (remain <= 0 && i != levelsLength) {
                    throw new IllegalArgumentException("Found symbols in the tree that never will be used");
                }
            }

            if (remain != 0) {
                throw new IllegalArgumentException("Provided tree is not exhaustive");
            }
        }
        else if (_symbols.length > 1) {
            throw new IllegalArgumentException("Impossible to have more than one symbol for 0 bits");
        }
    }

    DefinedHuffmanTable(int[] levelIndexes, Object[] symbols) {
        _levelIndexes = levelIndexes;
        _symbols = symbols;

        assertExhaustiveTable();
    }

    /**
     * Create a DefinedHuffmanTable resulting of iterating over the given structure of symbols.
     * <p>
     * This is a complex method and should be avoided.
     * Try using {@link #withFrequencies(Map, Comparator)} or {@link #from(Iterable, Comparator)} instead.
     * <p>
     * It is expected here that the given table has its symbols sorted from most probable to less
     * probable in order to ensure an optimal encoding.
     * <p>
     * It is also expected that the symbols within the iterable are not repeated.
     * <p>
     * It is also expected that the given iterable is finite.
     *
     * @param table An {@link java.lang.Iterable} of iterable of symbols.
     *              The main iterable must contain all symbols grouped for the number of
     *              bits that each symbol should use when encoded depending on its
     *              appearing frequency. The order on each of the sub iterable is
     *              irrelevant in terms of optimizations.
     *
     * @see #withFrequencies(Map, Comparator)
     * @see #from(Iterable, Comparator)
     */
    static <U> DefinedHuffmanTable<U> fromIterable(Iterable<Iterable<U>> table) {
        ArrayList<U> symbols = new ArrayList<>();
        ArrayList<Integer> indexes = new ArrayList<>();

        int bits = 0;
        int index = 0;
        for (Iterable<U> iterable : table) {
            if (bits != 0) {
                indexes.add(index);
            }

            for (U element : iterable) {
                symbols.add(element);
                index++;
            }

            bits++;
        }

        final int[] indexesArray = new int[indexes.size()];
        for (int i = 0; i < indexesArray.length; i++) {
            indexesArray[i] = indexes.get(i);
        }

        final Object[] symbolsArray = new Object[symbols.size()];
        for (int i = 0; i < symbolsArray.length; i++) {
            symbolsArray[i] = symbols.get(i);
        }

        return new DefinedHuffmanTable<>(indexesArray, symbolsArray);
    }

    private class HuffmanLevelIterator implements Iterator<E> {
        private final int _last;
        private int _index;

        private HuffmanLevelIterator(int bits) {
            _last = (bits == _levelIndexes.length)? _symbols.length : _levelIndexes[bits];
            _index = (bits == 0)? 0 : _levelIndexes[bits - 1];
        }

        @Override
        public boolean hasNext() {
            return _index < _last;
        }

        @Override
        public E next() {
            if (_index >= _last) {
                throw new UnsupportedOperationException();
            }

            return (E) _symbols[_index++];
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private class LevelIterable implements Iterable<E> {

        private final int _bits;

        private LevelIterable(int bits) {
            _bits = bits;
            //_level = _table[tableIndex];
        }

        @Override
        public HuffmanLevelIterator iterator() {
            return new HuffmanLevelIterator(_bits);
        }

        @Override
        public int hashCode() {
            return _bits;
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

        private int _bits;

        @Override
        public boolean hasNext() {
            return _bits <= _levelIndexes.length;
        }

        @Override
        public Iterable<E> next() {
            return new LevelIterable(_bits++);
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
            _hashCode = Arrays.hashCode(_symbols);
        }

        return _hashCode;
    }

    @Override
    public boolean equals(Object other) {
        if (other == null || !(other instanceof DefinedHuffmanTable)) {
            return false;
        }

        final DefinedHuffmanTable that = (DefinedHuffmanTable) other;
        return Arrays.equals(_levelIndexes, that._levelIndexes) && Arrays.equals(_symbols, that._symbols);
    }

    @Override
    public String toString() {
        final StringBuilder str = new StringBuilder("\n");
        final int levels = _levelIndexes.length + 1;
        for (int bits = 0; bits < levels; bits++) {
            final int levelLength = symbolsWithBits(bits);
            str.append("[");
            for (int j = 0; j < levelLength; j++) {
                str.append("" + getSymbol(bits, j));
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
        final int levelIndex = (bits == 0)? 0 : _levelIndexes[bits - 1];
        final int nextLevelIndex = (_levelIndexes.length == bits)? _symbols.length : _levelIndexes[bits];
        return nextLevelIndex - levelIndex;
    }

    @Override
    public E getSymbol(int bits, int index) {
        final int offset = (bits == 0)? 0 : _levelIndexes[bits - 1];
        return (E) _symbols[offset + index];
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
     *                  The bigger the value the more probable the symbol is.
     *                  Values on the map must be all positive numbers. Zero is also not allowed.
     * @param comparator Comparator for the symbols. After finding the number
     *                   of bits that each symbol should have, this will be
     *                   used to provide an order of symbols within the symbols
     *                   with the same number of bits. Ordering the symbols
     *                   properly may optimize the way the table will be
     *                   written in a stream.
     * @param <E> Type of the symbol to encode.
     *            It is strongly recommended that this type has a proper {@link Object#hashCode}
     *            method implemented. That will ensure that the table will be always the same
     *            for the same frequency map.
     * @return A DefinedHuffmanTable optimized for the map of frequencies given.
     */
    public static <E> DefinedHuffmanTable<E> withFrequencies(Map<E, Integer> frequency, Comparator<? super E> comparator) {
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

        int maxLength = 0;
        for (int length : symbolLengthMap.values()) {
            if (length > maxLength) {
                maxLength = length;
            }
        }

        final int[] tableIndexes = new int[maxLength];
        final Object[] tableSymbols = new Object[symbolLengthMap.keySet().size()];

        int index = 0;
        int bits = 0;

        while (symbolLengthMap.size() > 0) {
            if (bits != 0) {
                tableIndexes[bits - 1] = index;
            }

            final Iterator<Map.Entry<E, Integer>> it = symbolLengthMap.entrySet().iterator();
            ArrayList<E> level = new ArrayList<>();
            while (it.hasNext()) {
                final Map.Entry<E, Integer> entry = it.next();
                if (entry.getValue() == bits) {
                    level.add(entry.getKey());
                    it.remove();
                }
            }

            level.sort(comparator);
            for (E symbol : level) {
                tableSymbols[index++] = symbol;
            }

            bits++;
        }

        return new DefinedHuffmanTable<>(tableIndexes, tableSymbols);
    }

    /**
     * Build a {@link DefinedHuffmanTable} using the given symbol array as base.
     * <p>
     * This method builds a map of frequencies counting all the symbols found and
     * call {@link #withFrequencies(Map, Comparator)} in order to build the map.
     *
     * @param symbols Array of symbols from where the map of frequencies will be extracted.
     *                Thus, this map should contain a good sample of the kind of data to be
     *                compressed in an optimal way, or the whole data if it can fit in memory.
     * @param comparator Comparator for the symbols. After finding the number
     *                   of bits that each symbol should have, this will be
     *                   used to provide an order of symbols within the symbols
     *                   with the same number of bits. Ordering the symbols
     *                   properly may optimize the way the table will be
     *                   written in a stream.
     * @param <E> Type of the symbol to encode
     * @return A new {@link DefinedHuffmanTable} instance.
     */
    public static <E> DefinedHuffmanTable<E> from(Iterable<E> symbols, Comparator<? super E> comparator) {
        final Map<E, Integer> frequency = new HashMap<>();
        for (E symbol : symbols) {
            final Integer mapValue = frequency.get(symbol);
            final int newValue = 1 + ((mapValue != null)? mapValue : 0);
            frequency.put(symbol, newValue);
        }

        return withFrequencies(frequency, comparator);
    }
}
