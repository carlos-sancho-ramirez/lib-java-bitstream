package sword.bitstream;

import java.io.IOException;

/**
 * Decode Integer values from the stream. This implementation allow having null values.
 */
public class NullableIntegerDecoder implements SupplierWithIOException<Integer>, FunctionWithIOException<Integer, Integer> {

    private final InputBitStream _stream;

    public NullableIntegerDecoder(InputBitStream stream) {
        _stream = stream;
    }

    @Override
    public Integer apply() throws IOException {
        final boolean isValid = _stream.readBoolean();
        return isValid? _stream.readIntegerNumber() : null;
    }

    @Override
    public Integer apply(Integer previous) throws IOException {
        if (previous == null) {
            return _stream.readIntegerNumber();
        }

        return _stream.readNaturalNumber() + previous + 1;
    }
}
