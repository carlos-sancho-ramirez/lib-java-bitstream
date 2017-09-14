package sword.bitstream;

import java.io.IOException;

import static sword.bitstream.IntegerEncoder.integerTable;
import static sword.bitstream.IntegerEncoder.naturalTable;

/**
 * Decode Integer values from the stream. This implementation does not allow having null values.
 */
public class IntegerDecoder implements SupplierWithIOException<Integer>, FunctionWithIOException<Integer, Integer> {

    private final InputBitStream _stream;

    public IntegerDecoder(InputBitStream stream) {
        _stream = stream;
    }

    @Override
    public Integer apply() throws IOException {
        return _stream.readHuffmanSymbol(integerTable);
    }

    @Override
    public Integer apply(Integer previous) throws IOException {
        return _stream.readHuffmanSymbol(naturalTable) + previous + 1;
    }
}
