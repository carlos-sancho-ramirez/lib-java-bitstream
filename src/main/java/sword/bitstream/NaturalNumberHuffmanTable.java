package sword.bitstream;

import java.util.*;

/**
 * Huffman table that allow encoding natural numbers.
 * Negative numbers are not part of the this table.
 *
 * This table assign less bits to the smaller values and more bits to bigger ones.
 * Thus, zero is always the most probable one and then the one that takes less bits.
 *
 * This Huffman table assign always amount of bits that are multiple of the given
 * bit align. Trying to fit inside the lower values and adding more bits for bigger values.
 *
 * E.g. if bitAlign is 4 the resulting table will assign symbols from 0 to 7 to
 * the unique symbols with 4 bits once included, leaving the first bit as a switch
 * to extend the number of bits.
 * <br>0000 -&gt; 0
 * <br>0001 -&gt; 1
 * <br>0010 -&gt; 2
 * <br>0011 -&gt; 3
 * <br>0100 -&gt; 4
 * <br>0101 -&gt; 5
 * <br>0110 -&gt; 6
 * <br>0111 -&gt; 7
 *
 * Note that all encoded symbols start with '0'. In reality the amount of '1' before
 * this zero reflects the number of bits for this symbol. When the zero is the first
 * one, the amount of bit for the symbol is understood to match the bit align value.
 * When there are one '1' in front the zero ("10") then it will be the bit align
 * value multiplied by 2. Thus "110" will be "bitAlign * 3", "1110" will be
 * "bitAlign * 4" and so on.
 *
 * <br>10000000 -&gt; 8
 * <br>10000001 -&gt; 9
 * <br>...
 * <br>10111111 -&gt; 71
 * <br>110000000000 -&gt; 72
 * <br>110000000001 -&gt; 73
 * <br>...
 *
 * This table can theoretically include any number, even if it is really big.
 * Technically it is currently limited to the long bounds (64-bit integer).
 * As it can include any number and numbers are infinite, this table is
 * infinite as well and its iterable will not converge.
 */
public class NaturalNumberHuffmanTable implements HuffmanTable<Long> {

    private final int _bitAlign;

    /**
     * Create a new instance with the given bit alignment.
     * @param bitAlign Number of bits that the most probable symbols will have.
     *                 Check {@link NaturalNumberHuffmanTable} for more information.
     */
    public NaturalNumberHuffmanTable(int bitAlign) {
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
        return level > 0 && (((level + 1) % _bitAlign) == 0);
    }

    private int getSymbolsAtLevel(int level) {
        return 1 << (((level + 1) / _bitAlign) * (_bitAlign - 1));
    }

    @Override
    public int symbolsWithBits(int bits) {
        final int level = bits - 1;
        return isValidLevel(level)? getSymbolsAtLevel(level) : 0;
    }

    private long getBaseFromLevel(int level) {
        long base = 0;
        int exp = level / _bitAlign;
        while (exp > 0) {
            base += 1 << (exp * (_bitAlign - 1));
            exp--;
        }

        return base;
    }

    @Override
    public Long getSymbol(int bits, int index) {
        final int level = bits - 1;
        if (!isValidLevel(level)) {
            throw new IllegalArgumentException();
        }

        return getBaseFromLevel(level) + index;
    }

    private static class LevelIterator implements Iterator<Long> {

        private final long _lastLevelSymbol;

        private long _next;

        LevelIterator(long base, long lastLevelSymbol) {
            _lastLevelSymbol = lastLevelSymbol;
            _next = base;
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

    private class LevelIterable implements Iterable<Long> {

        private final int _level;

        LevelIterable(int level) {
            _level = level;
        }

        @Override
        public Iterator<Long> iterator() {
            final long base = getBaseFromLevel(_level);
            final long lastLevelSymbol = base + getSymbolsAtLevel(_level) - 1;
            return new LevelIterator(base, lastLevelSymbol);
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

    /**
     * Build a new instance based on the given map of frequencies.
     * Check {@link DefinedHuffmanTable#withFrequencies(Map)} for more detail.
     *
     * @param frequency Map of frequencies.
     * @return A new instance create.
     * @see DefinedHuffmanTable#withFrequencies(Map)
     */
    public static NaturalNumberHuffmanTable withFrequencies(Map<Long, Integer> frequency) {

        long maxValue = Long.MIN_VALUE;
        for (long symbol : frequency.keySet()) {
            if (symbol < 0) {
                throw new IllegalArgumentException("Found a negative number");
            }

            if (symbol > maxValue) {
                maxValue = symbol;
            }
        }

        if (maxValue < 0) {
            throw new IllegalArgumentException("map should not be empty");
        }

        int requiredBits = 0;
        long possibilities = 1;
        while (maxValue > possibilities) {
            possibilities <<= 1;
            requiredBits++;
        }

        final int minValidBitAlign = 2;

        // Any maxCheckedBitAlign bigger than requiredBits + 1 will always increase
        // for sure the number of required bits. That's why the limit is set here.
        final int maxCheckedBitAlign = requiredBits + 1;

        long minSize = Long.MAX_VALUE;
        int bestBitAlign = 0;

        for (int bitAlign = minValidBitAlign; bitAlign <= maxCheckedBitAlign; bitAlign++) {
            long length = 0;
            for (Map.Entry<Long, Integer> entry : frequency.entrySet()) {
                final long symbol = entry.getKey();
                int packs = 1;
                long nextBase = 1 << (bitAlign - 1);
                while (symbol >= nextBase) {
                    packs++;
                    nextBase += 1 << ((bitAlign - 1) * packs);
                }

                length += bitAlign * packs * entry.getValue();
                if (length > minSize) {
                    break;
                }
            }

            if (length < minSize) {
                minSize = length;
                bestBitAlign = bitAlign;
            }
        }

        return new NaturalNumberHuffmanTable(bestBitAlign);
    }
}
