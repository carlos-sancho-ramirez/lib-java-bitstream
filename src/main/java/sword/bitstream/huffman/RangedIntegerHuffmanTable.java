package sword.bitstream.huffman;

/**
 * Huffman table to encode a range of integers with uniform probability.
 */
public class RangedIntegerHuffmanTable extends AbstractHuffmanTable<Integer> {

    private final int _min;
    private final int _max;

    private final int _maxBits;
    private final int _limit;

    /**
     * Create a new instance for the given range of integers.
     *
     * @param min mininum expected value (inclusive)
     * @param max maximum expected value (inclusive)
     */
    public RangedIntegerHuffmanTable(int min, int max) {
        if (max < min) {
            throw new IllegalArgumentException("Invalid range");
        }

        _min = min;
        _max = max;

        final int possibilities = max - min + 1;
        int maxBits = 0;
        while (possibilities > (1 << maxBits)) {
            maxBits++;
        }

        _maxBits = maxBits;
        _limit = (1 << maxBits) - possibilities;
    }

    @Override
    public int symbolsWithBits(int bits) {
        if (bits == _maxBits) {
            return _max - _min + 1 - _limit;
        }
        else if (bits == _maxBits - 1) {
            return _limit;
        }

        return 0;
    }

    @Override
    public Integer getSymbol(int bits, int index) {
        if (bits == _maxBits) {
            return index + _limit + _min;
        }
        else if (bits == _maxBits - 1) {
            return index + _min;
        }

        throw new IllegalArgumentException("Invalid number of bits");
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + '(' + _min + ',' + _max + ')';
    }
}
