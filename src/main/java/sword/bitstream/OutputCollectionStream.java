package sword.bitstream;

import java.io.IOException;
import java.util.*;

public interface OutputCollectionStream {

    /**
     * Write an arbitrary map into the stream.
     *
     * @param lengthEncoder Callback used once to store the number of elements within the map.
     * @param keyWriter Encode a key into the stream.
     * @param diffKeyWriter Optional procedure that encode a key based on the previous one.
     *                      When given a proper comparator it may offer some optimizations.
     *                      This method can be null. In case of being null, keyWriter will
     *                      be called instead for all elements.
     * @param keyComparator Comparator for the key elements.
     *                      This will ensure that the map is always encoded in the same order.
     * @param valueWriter Encode a value into the stream.
     * @param map Map to be encoded.
     * @param <K> Type for the Key of the map.
     * @param <V> Type for the value of the map.
     * @throws IOException Thrown as soon as any of the given writers throws an IOException.
     */
    default <K, V> void writeMap(
            CollectionLengthEncoder lengthEncoder,
            ProcedureWithIOException<K> keyWriter,
            Procedure2WithIOException<K> diffKeyWriter,
            Comparator<? super K> keyComparator,
            ProcedureWithIOException<V> valueWriter,
            Map<K, V> map) throws IOException {

        final int mapSize = map.size();
        final Object[] keys = new Object[mapSize];
        int index = 0;
        for (K key : map.keySet()) {
            keys[index++] = key;
        }

        Arrays.sort(keys, 0, mapSize, (Comparator<? super Object>) keyComparator);
        lengthEncoder.encodeLength(mapSize);

        boolean first = true;
        K previous = null;
        for (K key : (K[]) keys) {
            if (diffKeyWriter == null || first) {
                keyWriter.apply(key);
                first = false;
            }
            else {
                diffKeyWriter.apply(previous, key);
            }
            previous = key;

            valueWriter.apply(map.get(key));
        }
    }

    /**
     * Write a set of arbitrary type into the stream
     * @param lengthEncoder Callback used once to store the number of elements within the set.
     * @param writer Encode an element from the set into the stream.
     * @param diffWriter Optional procedure that encode an element based on the previous one.
     *                   When given a proper comparator it may offer some optimizations.
     *                   This method can be null. In case of being null, writer will
     *                   be called instead for all elements.
     * @param comparator Comparator for the elements.
     *                   This will ensure that the set is always encoded in the same order.
     * @param set Set to be encoded.
     * @param <E> Type for the elements within the set.
     * @throws IOException Thrown only if any of the callbacks provided throws it.
     */
    default <E> void writeSet(
            CollectionLengthEncoder lengthEncoder,
            ProcedureWithIOException<E> writer,
            Procedure2WithIOException<E> diffWriter,
            Comparator<? super E> comparator,
            Set<E> set) throws IOException {

        final Object dummy = new Object();
        final HashMap<E, Object> map = new HashMap<>(set.size());
        for (E element : set) {
            map.put(element, dummy);
        }

        final ProcedureWithIOException<Object> nullWriter = new ProcedureWithIOException<Object>() {
            @Override
            public void apply(Object element) throws IOException {
                // Nothing to do
            }
        };
        writeMap(lengthEncoder, writer, diffWriter, comparator, nullWriter, map);
    }

    /**
     * Write a list of arbitrary type into the stream.
     * <p>
     * This method encode the list by encoding the number of elements first, and sending all symbols in the given order.
     *
     * @param lengthEncoder Callback used once to store the number of elements within the list.
     * @param writer Encode a single symbol from the list into the stream.
     * @param list List of elements to be encoded.
     * @param <E> Type for the symbols within the list.
     * @throws IOException Thrown only if any of the callbacks provided throws it.
     */
    default <E> void writeList(
            CollectionLengthEncoder lengthEncoder,
            ProcedureWithIOException<E> writer,
            List<E> list) throws IOException {
        new CollectionWriter<>(lengthEncoder, writer).apply(list);
    }
}
