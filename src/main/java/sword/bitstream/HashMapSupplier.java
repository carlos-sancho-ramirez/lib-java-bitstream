package sword.bitstream;

import java.io.IOException;
import java.util.HashMap;

public final class HashMapSupplier<K, V> implements SupplierWithIOException<HashMap<K, V>> {

    private final CollectionLengthDecoder _lengthDecoder;
    private final SupplierWithIOException<K> _keySupplier;
    private final FunctionWithIOException<K, K> _diffKeySupplier;
    private final SupplierWithIOException<V> _valueSupplier;

    public HashMapSupplier(CollectionLengthDecoder lengthDecoder,
                           SupplierWithIOException<K> keySupplier,
                           FunctionWithIOException<K, K> diffKeySupplier,
                           SupplierWithIOException<V> valueSupplier) {
        _lengthDecoder = lengthDecoder;
        _keySupplier = keySupplier;
        _diffKeySupplier = diffKeySupplier;
        _valueSupplier = valueSupplier;
    }

    @Override
    public HashMap<K, V> apply() throws IOException {
        final int length = _lengthDecoder.decodeLength();
        final HashMap<K, V> map = new HashMap<>(length);

        K key = null;
        for (int i = 0; i < length; i++) {
            key = (i == 0 || _diffKeySupplier == null)? _keySupplier.apply() : _diffKeySupplier.apply(key);
            map.put(key, _valueSupplier.apply());
        }

        return map;
    }
}
