package sword.bitstream;

import java.io.IOException;

import sword.bitstream.huffman.HuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;

/**
 * Decode from the stream a set of integer values.
 * This is assuming a given range of integer values and a concrete Huffman table to encode the length of the set.
 *
 * This is a counterpart of {@link RangedIntegerSetEncoder} and it is assumed that this is used to decode sets encoded by it.
 */
public class RangedIntegerSetDecoder implements CollectionLengthDecoder, SupplierWithIOException<Integer>, FunctionWithIOException<Integer, Integer> {

    private final InputBitStream _stream;
    private final HuffmanTable<Integer> _lengthTable;
    private final int _min;
    private final int _max;
    private int _length;
    private int _lastIndex;

    public RangedIntegerSetDecoder(InputBitStream stream, HuffmanTable<Integer> lengthTable, int min, int max) {
        if (max < min) {
            throw new IllegalArgumentException("minimum should be lower or equal than maximum");
        }

        _stream = stream;
        _lengthTable = lengthTable;
        _min = min;
        _max = max;
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

    @Override
    public int decodeLength() throws IOException {
        final int length = _stream.readHuffmanSymbol(_lengthTable);

        if (length < 0) {
            throw new IllegalArgumentException("length should not be a negative number");
        }

        if (length > _max - _min + 1) {
            throw new IllegalArgumentException("length should not be bigger than the amount of possible values within the range");
        }

        _length = length;
        return length;
    }
}
