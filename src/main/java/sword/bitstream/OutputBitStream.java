package sword.bitstream;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sword.bitstream.huffman.DefinedHuffmanTable;
import sword.bitstream.huffman.HuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;

/**
 * Wrapper for an {@link java.io.OutputStream} that provides optimal serialization
 * to compact and encode data into the stream.
 */
public class OutputBitStream implements Closeable {

    private final ProcedureWithIOException<Object> nullWriter = new ProcedureWithIOException<Object>() {
        @Override
        public void apply(Object element) throws IOException {
            // Nothing to do
        }
    };

    private final OutputStream _os;
    private int _buffer;
    private int _bitsOnBuffer;
    private boolean _closed;

    /**
     * Create a new instance wrapping the given {@link java.io.OutputStream}
     * @param os {@link java.io.OutputStream} to use to write the encoded data.
     */
    public OutputBitStream(OutputStream os) {
        if (os == null) {
            throw new IllegalArgumentException();
        }

        _os = os;
    }

    /**
     * Close this stream and the wrapped one.
     * @throws IOException if it is unable to write into the stream or it is already close.
     */
    @Override
    public void close() throws IOException {
        if (_bitsOnBuffer > 0) {
            _os.write(_buffer);
        }

        _os.close();
        _closed = true;
    }

    private void assertNotClosed() {
        if (_closed) {
            throw new IllegalArgumentException("Stream already closed");
        }
    }

    private void flushByte() throws IOException {
        while (_bitsOnBuffer >= 8) {
            _os.write(_buffer);
            _buffer >>>= 8;
            _bitsOnBuffer -= 8;
        }
    }

    /**
     * Write a boolean into the stream.
     * <p>
     * This method assumes that a byte has 8 bits and that a boolean can be
     * represented with a single bit. In other words, its possible to include
     * 8 booleans in each byte. With this assumption, this method will only
     * write into the wrapped stream once every 8 calls, once it has the values
     * for each of the booleans composing a byte.
     * <p>
     * Note that calling {@link #close()} in this class is required in order to
     * store all buffered booleans before closing the stream.
     * <p>
     * This is a key method within the class and all other methods depends on it.
     *
     * @param value boolean to be encoded and written into the stream.
     * @throws IOException if it is unable to write into the stream.
     */
    public void writeBoolean(boolean value) throws IOException {
        assertNotClosed();

        if (value) {
            _buffer |= 1 << _bitsOnBuffer;
        }

        _bitsOnBuffer++;
        flushByte();
    }

    /**
     * Write a symbol into the stream using the given Huffman table.
     * @param table Huffman table that specifies how to encode the symbol
     * @param symbol Symbol to encode. It must be present in the Huffman table.
     * @param <E> Type for the symbol to encode.
     * @throws IOException if it is unable to write into the stream.
     */
    public <E> void writeHuffmanSymbol(HuffmanTable<E> table, E symbol) throws IOException {
        int bits = 0;
        int acc = 0;
        for (Iterable<E> level : table) {
            for (E element : level) {
                if (symbol == null && element == null || symbol != null && symbol.equals(element)) {
                    if (bits > 0) {
                        for (int i = bits - 1; i >= 0; i--) {
                            writeBoolean((acc & (1 << i)) != 0);
                        }
                    }
                    return;
                }
                acc++;
            }
            acc <<= 1;
            bits++;
        }

        final String symbolString = (symbol != null)? symbol.toString() : "null";
        throw new IllegalArgumentException("Symbol <" + symbolString + "> is not included in the given Huffman table");
    }

    /**
     * Write a Huffman table into the stream.
     * <p>
     * As the symbol has a generic type, it is required that the caller of this
     * function provide the proper procedures to write each symbol.
     *
     * @param table Huffman table to encode.
     * @param proc Procedure to write a single symbol. This may not be called
     *             for all symbols if diffProc is different from null in order
     *             to reduce the amount of data to write.
     * @param diffProc Optional procedure to write a symbol based in a previous one.
     *                 This may compress in a better degree the table if their symbols are sortered.
     *                 In case this is null, the function given in proc will be called instead.
     * @param <E> Type of the symbol to encode.
     * @throws IOException if it is unable to write into the stream.
     */
    public <E> void writeHuffmanTable(DefinedHuffmanTable<E> table,
            ProcedureWithIOException<E> proc, Procedure2WithIOException<E> diffProc) throws IOException {
        int bits = 0;
        int max = 1;
        while (max > 0) {
            final int levelLength = table.symbolsWithBits(bits++);
            writeHuffmanSymbol(new RangedIntegerHuffmanTable(0, max), levelLength);
            max -= levelLength;
            max <<= 1;
        }

        for (Iterable<E> level : table) {
            Iterator<E> it = level.iterator();
            E previous = null;
            if (it.hasNext()) {
                previous = it.next();
                proc.apply(previous);
            }

            while (it.hasNext()) {
                E element = it.next();
                if (diffProc != null) {
                    diffProc.apply(previous, element);
                    previous = element;
                }
                else {
                    proc.apply(element);
                }
            }
        }
    }

    /**
     * Write an arbitrary map into the stream.
     *
     * @param lengthEncoder Callback used once to store the number of elements within the map.
     * @param map Map to be encoded.
     * @param keyComparator Comparator for the key elements.
     *                      This will ensure that the map is always encoded in the same order.
     * @param keyWriter Encode a key into the stream.
     * @param diffKeyWriter Optional procedure that encode a key based on the previous one.
     *                      When given a proper comparator it may offer some optimizations.
     *                      This method can be null. In case of being null, keyWriter will
     *                      be called instead for all elements.
     * @param valueWriter Encode a value into the stream.
     * @param <K> Type for the Key of the map.
     * @param <V> Type for the value of the map.
     * @throws IOException Thrown as soon as any of the given writers throws an IOException.
     */
    public <K, V> void writeMap(
            CollectionLengthEncoder lengthEncoder,
            Map<K, V> map,
            Comparator<? super K> keyComparator,
            ProcedureWithIOException<K> keyWriter,
            Procedure2WithIOException<K> diffKeyWriter,
            ProcedureWithIOException<V> valueWriter) throws IOException {

        final ArrayList<K> keyList = new ArrayList<>(map.keySet());
        keyList.sort(keyComparator);

        lengthEncoder.encodeLength(keyList.size());

        boolean first = true;
        K previous = null;
        for (K key : keyList) {
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
     * @param set Set to be encoded.
     * @param comparator Comparator for the elements.
     *                   This will ensure that the set is always encoded in the same order.
     * @param writer Encode an element from the set into the stream.
     * @param diffWriter Optional procedure that encode an element based on the previous one.
     *                   When given a proper comparator it may offer some optimizations.
     *                   This method can be null. In case of being null, writer will
     *                   be called instead for all elements.
     * @param <E> Type for the elements within the set.
     * @throws IOException Thrown only if any of the callbacks provided throws it.
     */
    public <E> void writeSet(
            CollectionLengthEncoder lengthEncoder, Set<E> set, Comparator<? super E> comparator,
            ProcedureWithIOException<E> writer, Procedure2WithIOException<E> diffWriter) throws IOException {

        final Object dummy = new Object();
        final HashMap<E, Object> map = new HashMap<>(set.size());
        for (E element : set) {
            map.put(element, dummy);
        }

        writeMap(lengthEncoder, map, comparator, writer, diffWriter, nullWriter);
    }

    /**
     * Write a list of arbitrary type into the stream.
     * <p>
     * This method encode the list by encoding the number of elements first, and sending all symbols in the given order.
     *
     * @param lengthEncoder Callback used once to store the number of elements within the list.
     * @param list List of elements to be encoded.
     * @param writer Encode a single symbol from the list into the stream.
     * @param <E> Type for the symbols within the list.
     * @throws IOException Thrown only if any of the callbacks provided throws it.
     */
    public <E> void writeList(
            CollectionLengthEncoder lengthEncoder, List<E> list, ProcedureWithIOException<E> writer) throws IOException {
        final int length = list.size();
        lengthEncoder.encodeLength(length);

        for (E symbol : list) {
            writer.apply(symbol);
        }
    }

    /**
     * Write a set of range numbers into the stream.
     *
     * @param lengthEncoder Encoder used to write the size of the set.
     * @param min Minimum value expected for any of the values included in the set.
     * @param max Maximum value expected for any of the values included in the set.
     * @param set Set of values to be written into the stream.
     *            All its values must be between min and max inclusive.
     *            It can be empty, but never null.
     * @throws IOException if it is unable to write into the stream.
     */
    public void writeRangedNumberSet(CollectionLengthEncoder lengthEncoder, int min, int max, Set<Integer> set) throws IOException {
        final RangedIntegerSetEncoder encoder = new RangedIntegerSetEncoder(this, min, max, set.size());
        writeSet(lengthEncoder, set, encoder, encoder, encoder);
    }
}
