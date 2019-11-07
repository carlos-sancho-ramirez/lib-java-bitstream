package sword.bitstream;

import java.io.IOException;
import java.util.*;

public interface InputCollectionStream {
    /**
     * Read an arbitrary map into the stream.
     *
     * @param lengthDecoder Callback used once to read the number of elements within the map.
     * @param keySupplier Decode a key from the stream.
     * @param diffKeySupplier Optional supplier that decode a key based on the previous one.
     *                      When given a proper comparator in writing time, it may offer some optimizations.
     *                      This method can be null. In case of being null, keySupplier will
     *                      be called instead for all elements.
     * @param valueSupplier Decode a value from the stream.
     * @param <K> Type for the Key of the map.
     * @param <V> Type for the value of the map.
     * @return A map read from the stream.
     * @throws IOException Thrown as soon as any of the given suppliers throw an IOException.
     */
    default <K, V> Map<K, V> readMap(
            CollectionLengthDecoder lengthDecoder,
            SupplierWithIOException<K> keySupplier,
            FunctionWithIOException<K, K> diffKeySupplier,
            SupplierWithIOException<V> valueSupplier) throws IOException {

        final int length = lengthDecoder.decodeLength();
        final HashMap<K, V> map = new HashMap<>(length);

        K key = null;
        for (int i = 0; i < length; i++) {
            if (i == 0 || diffKeySupplier == null) {
                key = keySupplier.apply();
            }
            else {
                key = diffKeySupplier.apply(key);
            }

            map.put(key, valueSupplier.apply());
        }

        return map;
    }

    /**
     * Read an arbitrary set from the stream.
     *
     * @param lengthDecoder Callback used once to read the number of elements within the set.
     * @param supplier Decode an element from the stream.
     * @param diffSupplier Optional supplier that decode an element based on the previous one.
     *                     When given a proper comparator in writing time, it may offer some optimizations.
     *                     This method can be null. In case of being null, <code>supplier</code>
     *                     will be called instead for all elements.
     * @param <E> Type for the elements within the set.
     * @return A set read from the stream.
     * @throws IOException Thrown as soon as any of the given suppliers throw an IOException.
     */
    default <E> Set<E> readSet(
            CollectionLengthDecoder lengthDecoder,
            SupplierWithIOException<E> supplier,
            FunctionWithIOException<E, E> diffSupplier) throws IOException {
        final SupplierWithIOException<Object> nullSupplier = new SupplierWithIOException<Object>() {

            @Override
            public Object apply() throws IOException {
                return this;
            }
        };
        return readMap(lengthDecoder, supplier, diffSupplier, nullSupplier).keySet();
    }

    /**
     * Read an arbitrary list from the stream.
     * <p>
     * This is the complementary method of {@link OutputCollectionStream#writeList(CollectionLengthEncoder, ProcedureWithIOException, List)}.
     * Thus, assumes that the length is encoded first, and then all symbols are given in order after that.
     *
     * @param lengthDecoder Callback used once to read the number of symbols within the list.
     * @param supplier Decode a single symbol of the list.
     * @param <E> Type for the symbols within the list.
     * @return A list read from the stream.
     * @throws IOException Thrown only if any of the given callback throws it.
     */
    default <E> List<E> readList(
            CollectionLengthDecoder lengthDecoder,
            SupplierWithIOException<E> supplier) throws IOException {
        final int length = lengthDecoder.decodeLength();
        final ArrayList<E> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            result.add(supplier.apply());
        }

        return result;
    }
}
