package sword.bitstream;

import java.io.IOException;

import sword.bitstream.huffman.RangedIntegerHuffmanTable;

/**
 * Decode from the stream all integer values of a set.
 * This is assuming a given range and a specific number of elements within the set.
 *
 * As this class is expected to decode a set, it is expected that all elements
 * will be given in ascending order and none will be repeated.
 */
class RangedIntegerSetDecoder implements SupplierWithIOException<Integer>, FunctionWithIOException<Integer, Integer> {

    private final InputBitStream _stream;
    private final int _min;
    private final int _max;
    private final int _length;
    private int _lastIndex;

    RangedIntegerSetDecoder(InputBitStream stream, int min, int max, int length) {
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
    public Integer apply() throws IOException {
        RangedIntegerHuffmanTable table = new RangedIntegerHuffmanTable(_min, _max - _length + 1);
        _lastIndex = 0;
        return _stream.readHuffmanSymbol(table);
    }

    @Override
    public Integer apply(Integer previous) throws IOException {
        ++_lastIndex;
        RangedIntegerHuffmanTable table = new RangedIntegerHuffmanTable(previous + 1, _max - _length + _lastIndex + 1);
        return _stream.readHuffmanSymbol(table);
    }
}
