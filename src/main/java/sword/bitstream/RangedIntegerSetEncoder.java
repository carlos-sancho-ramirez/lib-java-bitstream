package sword.bitstream;

import java.io.IOException;
import java.util.Comparator;

import sword.bitstream.huffman.RangedIntegerHuffmanTable;

/**
 * Encode into the stream all integer values from a set.
 * This is assuming a given range and a specific number of elements within the set.
 *
 * As this class is expected to encode a set, it is expected that all elements
 * will be given in ascending order and none will be repeated.
 * This allow minimizing the data into the stream.
 *
 * This implementation does not allow having null values.
 */
class RangedIntegerSetEncoder implements Comparator<Integer>, ProcedureWithIOException<Integer>, Procedure2WithIOException<Integer> {

    private final OutputBitStream _stream;
    private final int _min;
    private final int _max;
    private final int _length;
    private int _lastIndex;

    RangedIntegerSetEncoder(OutputBitStream stream, int min, int max, int length) {
        if (max < min) {
            throw new IllegalArgumentException("minimum should be lower or equal than maximum");
        }

        if (length >= 0 && length > max - min + 1) {
            throw new IllegalArgumentException("length should not be bigger than the amount of possible values within the range");
        }

        _stream = stream;
        _min = min;
        _max = max;
        _length = length;
    }

    @Override
    public int compare(Integer a, Integer b) {
        return a - b;
    }

    @Override
    public void apply(Integer element) throws IOException {
        RangedIntegerHuffmanTable table = new RangedIntegerHuffmanTable(_min, _max - _length + 1);
        _stream.writeHuffmanSymbol(table, element);
        _lastIndex = 0;
    }

    @Override
    public void apply(Integer previous, Integer element) throws IOException {
        ++_lastIndex;
        RangedIntegerHuffmanTable table = new RangedIntegerHuffmanTable(previous + 1, _max - _length + _lastIndex + 1);
        _stream.writeHuffmanSymbol(table, element);
    }
}
