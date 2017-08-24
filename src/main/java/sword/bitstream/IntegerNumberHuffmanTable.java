package sword.bitstream;

import java.util.Iterator;

/**
 * Huffman table that allow encoding integer numbers.
 * This means that all zero, positive and negative numbers are allowed without any decimal.
 * <p>
 * This table assign less bits to the values closer to 0 and more bits to ones further.
 * Thus, zero is always the most probable one and then the one that takes less bits.
 * <p>
 * This Huffman table assign always amount of bits that are multiple of the given
 * bit align. Trying to fit inside the closer values and adding more bits for further values.
 * <p>
 * E.g. if bitAlign is 4 the resulting table will assign symbols from -4 to 3 to
 * the unique symbols with 4 bits once included, leaving the first bit as a switch
 * to extend the number of bits.
 * <code>
 * <br>&nbsp;&nbsp;0000 &rArr; 0
 * <br>&nbsp;&nbsp;0001 &rArr; 1
 * <br>&nbsp;&nbsp;0010 &rArr; 2
 * <br>&nbsp;&nbsp;0011 &rArr; 3
 * <br>&nbsp;&nbsp;0100 &rArr; -4
 * <br>&nbsp;&nbsp;0101 &rArr; -3
 * <br>&nbsp;&nbsp;0110 &rArr; -2
 * <br>&nbsp;&nbsp;0111 &rArr; -1
 * <br></code>
 * <p>
 * Note that all encoded symbols start with <code>0</code>. In reality the amount of <code>1</code> before
 * this zero reflects the number of bits for this symbol. When the zero is the first
 * one, the amount of bits for the symbol is understood to match the bit align value.
 * When there are one <code>1</code> in front the zero (<code>10</code>) then it will be the bit align
 * value multiplied by 2. Thus <code>110</code> will be <code>bitAlign * 3</code>, <code>1110</code> will be
 * <code>bitAlign * 4</code> and so on.
 * <code>
 * <br>&nbsp;&nbsp;10000000 &rArr; 4
 * <br>&nbsp;&nbsp;10000001 &rArr; 5
 * <br>&nbsp;&nbsp;...
 * <br>&nbsp;&nbsp;10011111 &rArr; 35
 * <br>&nbsp;&nbsp;10100000 &rArr; -36
 * <br>&nbsp;&nbsp;...
 * <br>&nbsp;&nbsp;10111111 &rArr; -5
 * <br>&nbsp;&nbsp;110000000000 &rArr; 36
 * <br>&nbsp;&nbsp;110000000001 &rArr; 37
 * <br>&nbsp;&nbsp;...
 * <br></code>
 * <p>
 * This table can theoretically include any number, even if it is really big.
 * Technically it is currently limited to the long bounds (64-bit integer).
 * As it can include any number and numbers are infinite, this table is
 * infinite as well and its iterable will not converge.
 */
public class IntegerNumberHuffmanTable implements HuffmanTable<Long> {

    private final int _bitAlign;

    /**
     * Create a new instance with the given bit alignment.
     * @param bitAlign Number of bits that the most probable symbols will have.
     *                 Check {@link IntegerNumberHuffmanTable} for more information.
     */
    public IntegerNumberHuffmanTable(int bitAlign) {
        if (bitAlign < 2) {
            throw new IllegalArgumentException();
        }

        _bitAlign = bitAlign;
    }

    /**
     * Return the bit alignment provided in construction time.
     * This value can be used to encode this table, as it is the only relevant number.
     * @return The bit alignment of this Huffman table.
     */
    public int getBitAlign() {
        return _bitAlign;
    }

    private boolean isValidLevel(int level) {
        return level > 0 && ((level % _bitAlign) == 0);
    }

    private int getSymbolsAtLevel(int level) {
        return 1 << ((level / _bitAlign) * (_bitAlign - 1));
    }

    @Override
    public int symbolsWithBits(int bits) {
        return isValidLevel(bits)? getSymbolsAtLevel(bits) : 0;
    }

    private long getBaseFromLevel(int level) {
        long base = 0;
        int exp = ((level - 1) / _bitAlign) * (_bitAlign - 1) - 1;
        while (exp > 0) {
            base += 1 << exp;
            exp -= _bitAlign - 1;
        }

        return base;
    }

    private long getNegativeBaseFromLevel(int level) {
        long base = 0;
        int exp = (level / _bitAlign) * (_bitAlign - 1) - 1;
        while (exp > 0) {
            base -= 1 << exp;
            exp -= _bitAlign - 1;
        }

        return base;
    }

    @Override
    public Long getSymbol(int bits, int index) {
        if (!isValidLevel(bits)) {
            throw new IllegalArgumentException();
        }

        int symbolsPerSegment = getSymbolsAtLevel(bits) / 2;
        return (index < symbolsPerSegment)?
                getBaseFromLevel(bits) + index :
                getNegativeBaseFromLevel(bits) + (index - symbolsPerSegment);
    }

    private class LevelIterator implements Iterator<Long> {

        private final int _level;
        private int _index;

        LevelIterator(int level) {
            _level = level;
        }

        @Override
        public boolean hasNext() {
            return _index < symbolsWithBits(_level);
        }

        @Override
        public Long next() {
            return getSymbol(_level, _index++);
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private class LevelIterable implements Iterable<Long> {

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

    private class TableIterator implements Iterator<Iterable<Long>> {

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

    /**
     * Return an iterator to check symbols one by one in the given order.
     * Note that this iterator will never converge and this table is infinite.
     */
    @Override
    public Iterator<Iterable<Long>> iterator() {
        return new TableIterator();
    }
}
