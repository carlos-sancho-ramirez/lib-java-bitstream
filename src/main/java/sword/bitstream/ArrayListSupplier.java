package sword.bitstream;

import java.io.IOException;
import java.util.ArrayList;

public final class ArrayListSupplier<T> implements SupplierWithIOException<ArrayList<T>> {

    private final CollectionLengthDecoder _lengthDecoder;
    private final SupplierWithIOException<T> _symbolSupplier;

    public ArrayListSupplier(CollectionLengthDecoder lengthDecoder, SupplierWithIOException<T> symbolSupplier) {
        _lengthDecoder = lengthDecoder;
        _symbolSupplier = symbolSupplier;
    }

    @Override
    public ArrayList<T> apply() throws IOException {
        final int length = _lengthDecoder.decodeLength();
        final ArrayList<T> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            result.add(_symbolSupplier.apply());
        }

        return result;
    }
}
