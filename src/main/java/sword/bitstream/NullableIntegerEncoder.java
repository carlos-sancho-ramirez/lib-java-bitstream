package sword.bitstream;

import java.io.IOException;
import java.util.Comparator;

/**
 * Encode Integer values into the stream. This implementation allow having null values.
 */
public class NullableIntegerEncoder implements Comparator<Integer>, ProcedureWithIOException<Integer>, Procedure2WithIOException<Integer> {

    private final OutputBitStream _stream;

    public NullableIntegerEncoder(OutputBitStream stream) {
        _stream = stream;
    }

    @Override
    public int compare(Integer a, Integer b) {
        return (a == null || b != null && (a < b))? -1 : 1;
    }

    @Override
    public void apply(Integer element) throws IOException {
        if (element == null) {
            _stream.writeBoolean(false);
        }
        else {
            _stream.writeBoolean(true);
            _stream.writeIntegerNumber(element);
        }
    }

    @Override
    public void apply(Integer previous, Integer element) throws IOException {
        if (previous == null) {
            _stream.writeIntegerNumber(element);
        }
        else {
            _stream.writeNaturalNumber(element - previous - 1);
        }
    }
}
