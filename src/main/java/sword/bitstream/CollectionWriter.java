package sword.bitstream;

import java.io.IOException;
import java.util.Collection;

public final class CollectionWriter<T> implements ProcedureWithIOException<Collection<T>> {

    private final CollectionLengthEncoder _lengthEncoder;
    private final ProcedureWithIOException<T> _symbolWriter;

    public CollectionWriter(CollectionLengthEncoder lengthEncoder, ProcedureWithIOException<T> symbolWriter) {
        _lengthEncoder = lengthEncoder;
        _symbolWriter = symbolWriter;
    }

    @Override
    public void apply(Collection<T> collection) throws IOException {
        final int length = collection.size();
        _lengthEncoder.encodeLength(length);

        for (T symbol : collection) {
            _symbolWriter.apply(symbol);
        }
    }
}
