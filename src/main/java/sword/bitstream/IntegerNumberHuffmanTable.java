package sword.bitstream;

import java.util.Iterator;
import java.util.Map;

/**
 * Huffman table that allow encoding integer numbers.
 * This means that all zero, positive and negative numbers are allowed without any decimal.
 *
 * This table assign less bits to the values closer to 0 and more bits to ones further.
 * Thus, zero is always the most probable one and then the one that takes less bits.
 *
 * This Huffman table assign always amount of bits that are multiple of the given
 * bit align. Trying to fit inside the closer values and adding more bits for further values.
 *
 * E.g. if bitAlign is 4 the resulting table will assign symbols from -4 to 3 to
 * the unique symbols with 4 bits once included, leaving the first bit as a switch
 * to extend the number of bits.
 * <br>0000 -&gt; 0
 * <br>0001 -&gt; 1
 * <br>0010 -&gt; 2
 * <br>0011 -&gt; 3
 * <br>0100 -&gt; -4
 * <br>0101 -&gt; -3
 * <br>0110 -&gt; -2
 * <br>0111 -&gt; -1
 *
 * Note that all encoded symbols start with '0'. In reality the amount of '1' before
 * this zero reflects the number of bits for this symbol. When the zero is the first
 * one, the amount of bits for the symbol is understood to match the bit align value.
 * When there are one '1' in front the zero ("10") then it will be the bit align
 * value multiplied by 2. Thus "110" will be "bitAlign * 3", "1110" will be
 * "bitAlign * 4" and so on.
 *
 * <br>10000000 -&gt; 4
 * <br>10000001 -&gt; 5
 * <br>...
 * <br>10011111 -&gt; 35
 * <br>10100000 -&gt; -36
 * <br>...
 * <br>10111111 -&gt; -5
 * <br>110000000000 -&gt; 36
 * <br>110000000001 -&gt; 37
 * <br>...
 *
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
