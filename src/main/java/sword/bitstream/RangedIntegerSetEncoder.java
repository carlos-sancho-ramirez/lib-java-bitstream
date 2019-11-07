package sword.bitstream;

import java.io.IOException;
import java.util.Comparator;

import sword.bitstream.huffman.HuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;

/**
 * Encode into the stream a set of integer values.
 * This is assuming a given range of possible integer values and a concrete Huffman table to encode the length of the set.
 *
 * As this class is expected to encode a set, it is expected that all elements
 * will be given in ascending order and none will be repeated.
 * This allow minimizing the data into the stream.
 *
 * This implementation does not allow having null values.
 *
 * @see RangedIntegerSetDecoder
 */
public class RangedIntegerSetEncoder implements Comparator<Integer>, CollectionLengthEncoder, ProcedureWithIOException<Integer>, Procedure2WithIOException<Integer> {

    private final OutputHuffmanStream _stream;
    private final HuffmanTable<Integer> _lengthTable;
    private final int _min;
    private final int _max;
    private int _length;
    private int _lastIndex;

    public RangedIntegerSetEncoder(OutputHuffmanStream stream, HuffmanTable<Integer> lengthTable, int min, int max) {
        if (max < min) {
            throw new IllegalArgumentException("minimum should be lower or equal than maximum");
        }

        _stream = stream;
        _lengthTable = lengthTable;
        _min = min;
        _max = max;
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

    @Override
    public void encodeLength(int length) throws IOException {
        if (length < 0) {
            throw new IllegalArgumentException("length should not be a negative number");
        }

        if (length > _max - _min + 1) {
            throw new IllegalArgumentException("length should not be bigger than the amount of possible values within the range");
        }

        _length = length;
        _stream.writeHuffmanSymbol(_lengthTable, length);
    }
}
