package sword.bitstream;

import java.io.IOException;

import static sword.bitstream.IntegerEncoder.integerTable;
import static sword.bitstream.IntegerEncoder.naturalTable;

/**
 * Decode natural values from the stream.
 *
 * Implementation within this is the counterpart of the implementation within {@link NaturalEncoder}.
 * That means that this class will decode any value encoded by {@link NaturalEncoder}.
 *
 * @see NaturalEncoder
 */
public final class NaturalDecoder implements SupplierWithIOException<Integer>, FunctionWithIOException<Integer, Integer> {

    private final InputHuffmanStream _stream;

    public NaturalDecoder(InputHuffmanStream stream) {
        _stream = stream;
    }

    @Override
    public Integer apply() throws IOException {
        return _stream.readHuffmanSymbol(naturalTable);
    }

    @Override
    public Integer apply(Integer previous) throws IOException {
        return _stream.readHuffmanSymbol(naturalTable) + previous + 1;
    }
}
