package sword.bitstream;

import java.io.IOException;
import java.util.Comparator;

/**
 * Encode Integer values into the stream assuming a given range. This implementation does not allow having null values.
 */
public class RangedIntegerEncoder implements Comparator<Integer>, ProcedureWithIOException<Integer>, Procedure2WithIOException<Integer> {

    private final OutputBitStream _stream;
    private final int _min;
    private final int _max;

    public RangedIntegerEncoder(OutputBitStream stream, int min, int max) {
        if (max < min) {
            throw new IllegalArgumentException("minimum should be lower or equal than maximum");
        }

        _stream = stream;
        _min = min;
        _max = max;
    }

    @Override
    public int compare(Integer a, Integer b) {
        return a - b;
    }

    @Override
    public void apply(Integer element) throws IOException {
        _stream.writeRangedNumber(_min, _max, element);
    }

    @Override
    public void apply(Integer previous, Integer element) throws IOException {
        _stream.writeRangedNumber(previous + 1, _max, element);
    }
}
