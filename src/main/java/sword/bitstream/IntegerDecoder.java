package sword.bitstream;

import java.io.IOException;

import static sword.bitstream.IntegerEncoder.integerTable;
import static sword.bitstream.IntegerEncoder.naturalTable;

/**
 * Decode integer values from the stream.
 *
 * Implementation within this is the counterpart of the implementation within {@link IntegerEncoder}.
 * That means that this class will decode any value encoded by {@link IntegerEncoder}.
 *
 * @see IntegerEncoder
 */
public final class IntegerDecoder implements SupplierWithIOException<Integer>, FunctionWithIOException<Integer, Integer> {

    private final InputHuffmanStream _stream;

    public IntegerDecoder(InputHuffmanStream stream) {
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
