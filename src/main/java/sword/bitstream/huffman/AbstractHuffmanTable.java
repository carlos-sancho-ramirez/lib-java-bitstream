package sword.bitstream.huffman;

import java.util.Iterator;

abstract class AbstractHuffmanTable<T> implements HuffmanTable<T> {

    private static final Iterator _emptyIterator = new AbstractIterator() {

        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            throw new UnsupportedOperationException("Unable to retrieve elements from an empty iterator");
        }
    };

    private static final Iterable _emptyIterable = new Iterable() {

        @Override
        public Iterator iterator() {
            return _emptyIterator;
        }
    };

    private final class BitLevelIterator extends AbstractIterator<T> {

        private final int _bits;
        private final int _symbolCount;
        private int _index;

        BitLevelIterator(int bits) {
            _bits = bits;
            _symbolCount = symbolsWithBits(bits);
        }

        @Override
        public boolean hasNext() {
            return _index < _symbolCount;
        }

        @Override
        public T next() {
            return getSymbol(_bits, _index++);
        }
    }

    private final class BitLevelIterable implements Iterable<T> {

        private final int _bits;

        BitLevelIterable(int bits) {
            _bits = bits;
        }

        @Override
        public Iterator<T> iterator() {
            return new BitLevelIterator(_bits);
        }
    }

    private final class TableIterator extends AbstractIterator<Iterable<T>> {

        private int _bits;
        private int _remaining = 1;

        @Override
        public boolean hasNext() {
            return _remaining > 0;
        }

        @Override
        public Iterable<T> next() {
            final int symbolCount = symbolsWithBits(_bits);
            _remaining = (_remaining - symbolCount) * 2;

            if (symbolCount == 0) {
                ++_bits;
                return _emptyIterable;
            }

            return new BitLevelIterable(_bits++);
        }
    }

    @Override
    public Iterator<Iterable<T>> iterator() {
        return new TableIterator();
    }
}
