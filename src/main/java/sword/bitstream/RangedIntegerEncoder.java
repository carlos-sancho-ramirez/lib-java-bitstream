package sword.bitstream;

import java.io.IOException;
import java.util.Comparator;

import sword.bitstream.huffman.RangedIntegerHuffmanTable;

/**
 * Encode Integer values into the stream assuming a given range. This implementation does not allow having null values.
 */
public class RangedIntegerEncoder implements Comparator<Integer>, ProcedureWithIOException<Integer>, Procedure2WithIOException<Integer> {

    private final OutputBitStream _stream;
    private final RangedIntegerHuffmanTable _table;
    private final int _max;

    public RangedIntegerEncoder(OutputBitStream stream, int min, int max) {
        if (max < min) {
            throw new IllegalArgumentException("minimum should be lower or equal than maximum");
        }

        _stream = stream;
        _max = max;
        _table = new RangedIntegerHuffmanTable(min, max);
    }

    @Override
    public int compare(Integer a, Integer b) {
        return a - b;
    }

    @Override
    public void apply(Integer element) throws IOException {
        _stream.writeHuffmanSymbol(_table, element);
    }

    @Override
    public void apply(Integer previous, Integer element) throws IOException {
        _stream.writeHuffmanSymbol(new RangedIntegerHuffmanTable(previous + 1, _max), element);
    }
}
