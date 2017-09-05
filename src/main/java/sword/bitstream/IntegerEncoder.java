package sword.bitstream;

import java.io.IOException;
import java.util.Comparator;

/**
 * Encode Integer values into the stream. This implementation does not allow having null values.
 */
public class IntegerEncoder implements Comparator<Integer>, ProcedureWithIOException<Integer>, Procedure2WithIOException<Integer> {

    private final OutputBitStream _stream;

    public IntegerEncoder(OutputBitStream stream) {
        _stream = stream;
    }

    @Override
    public int compare(Integer a, Integer b) {
        return a - b;
    }

    @Override
    public void apply(Integer element) throws IOException {
        _stream.writeIntegerNumber(element);
    }

    @Override
    public void apply(Integer previous, Integer element) throws IOException {
        _stream.writeNaturalNumber(element - previous - 1);
    }
}
