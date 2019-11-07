package sword.bitstream;

import java.io.IOException;

import sword.bitstream.huffman.RangedIntegerHuffmanTable;

/**
 * Decode Integer values from the stream assuming a given range. This implementation does not allow having null values.
 */
public class RangedIntegerDecoder implements SupplierWithIOException<Integer>, FunctionWithIOException<Integer, Integer> {

    private final InputHuffmanStream _stream;
    private final RangedIntegerHuffmanTable _table;
    private final int _max;

    public RangedIntegerDecoder(InputHuffmanStream stream, int min, int max) {
        if (max < min) {
            throw new IllegalArgumentException("minimum should be lower or equal than maximum");
        }

        _stream = stream;
        _max = max;
        _table = new RangedIntegerHuffmanTable(min, max);
    }

    @Override
    public Integer apply() throws IOException {
        return _stream.readHuffmanSymbol(_table);
    }

    @Override
    public Integer apply(Integer previous) throws IOException {
        return _stream.readHuffmanSymbol(new RangedIntegerHuffmanTable(previous + 1, _max));
    }
}
