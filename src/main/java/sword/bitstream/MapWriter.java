package sword.bitstream;

import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

public final class MapWriter<K, V> implements ProcedureWithIOException<Map<K, V>> {

    private final CollectionLengthEncoder _lengthEncoder;
    private final ProcedureWithIOException<K> _keyWriter;
    private final Procedure2WithIOException<K> _diffKeyWriter;
    private final Comparator<? super K> _keyComparator;
    private final ProcedureWithIOException<V> _valueWriter;

    public MapWriter(CollectionLengthEncoder lengthEncoder,
                     ProcedureWithIOException<K> keyWriter,
                     Procedure2WithIOException<K> diffKeyWriter,
                     Comparator<? super K> keyComparator,
                     ProcedureWithIOException<V> valueWriter) {
        _lengthEncoder = lengthEncoder;
        _keyWriter = keyWriter;
        _diffKeyWriter = diffKeyWriter;
        _keyComparator = keyComparator;
        _valueWriter = valueWriter;
    }

    @Override
    public void apply(Map<K, V> map) throws IOException {
        final int mapSize = map.size();
        final Object[] keys = new Object[mapSize];
        int index = 0;
        for (K key : map.keySet()) {
            keys[index++] = key;
        }

        Arrays.sort(keys, 0, mapSize, (Comparator<? super Object>) _keyComparator);
        _lengthEncoder.encodeLength(mapSize);

        boolean first = true;
        K previous = null;
        for (K key : (K[]) keys) {
            if (_diffKeyWriter == null || first) {
                _keyWriter.apply(key);
                first = false;
            }
            else {
                _diffKeyWriter.apply(previous, key);
            }
            previous = key;

            _valueWriter.apply(map.get(key));
        }
    }
}
