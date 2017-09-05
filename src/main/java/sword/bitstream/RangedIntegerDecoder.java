package sword.bitstream;

import java.io.IOException;

/**
 * Decode Integer values from the stream assuming a given range. This implementation does not allow having null values.
 */
public class RangedIntegerDecoder implements SupplierWithIOException<Integer>, FunctionWithIOException<Integer, Integer> {

    private final InputBitStream _stream;
    private final int _min;
    private final int _max;

    public RangedIntegerDecoder(InputBitStream stream, int min, int max) {
        if (max < min) {
            throw new IllegalArgumentException("minimum should be lower or equal than maximum");
        }

        _stream = stream;
        _min = min;
        _max = max;
    }

    @Override
    public Integer apply() throws IOException {
        return _stream.readRangedNumber(_min, _max);
    }

    @Override
    public Integer apply(Integer previous) throws IOException {
        return _stream.readRangedNumber(previous + 1, _max);
    }
}
