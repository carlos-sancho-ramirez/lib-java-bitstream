package sword.bitstream;

import java.io.IOException;

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
        return _stream.readIntegerNumber();
    }

    @Override
    public Integer apply(Integer previous) throws IOException {
        return _stream.readNaturalNumber() + previous + 1;
    }
}
