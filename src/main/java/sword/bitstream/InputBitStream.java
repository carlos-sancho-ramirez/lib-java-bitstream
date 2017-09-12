package sword.bitstream;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sword.bitstream.huffman.DefinedHuffmanTable;
import sword.bitstream.huffman.HuffmanTable;
import sword.bitstream.huffman.IntegerNumberHuffmanTable;
import sword.bitstream.huffman.LongIntegerNumberHuffmanTable;
import sword.bitstream.huffman.LongNaturalNumberHuffmanTable;
import sword.bitstream.huffman.NaturalNumberHuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;

import static sword.bitstream.OutputBitStream.INTEGER_NUMBER_BIT_ALIGNMENT;
import static sword.bitstream.OutputBitStream.NATURAL_NUMBER_BIT_ALIGNMENT;

/**
 * Wrapper for a Java InputStream that adds functionality to read serialiazed content.
 * <p>
 * This is a complementary class for {@link OutputBitStream}. Thus, this class
 * provides lot of methods to read what the complementary class has written in
 * to the output stream.
 */
public class InputBitStream implements Closeable {

    private final NaturalNumberHuffmanTable naturalNumberHuffmanTable =
            new NaturalNumberHuffmanTable(NATURAL_NUMBER_BIT_ALIGNMENT);
    private final IntegerNumberHuffmanTable integerNumberHuffmanTable =
            new IntegerNumberHuffmanTable(INTEGER_NUMBER_BIT_ALIGNMENT);

    private final LongNaturalNumberHuffmanTable longNaturalNumberHuffmanTable =
            new LongNaturalNumberHuffmanTable(NATURAL_NUMBER_BIT_ALIGNMENT);
    private final LongIntegerNumberHuffmanTable longIntegerNumberHuffmanTable =
            new LongIntegerNumberHuffmanTable(INTEGER_NUMBER_BIT_ALIGNMENT);

    private final SupplierWithIOException<Object> nullSupplier = new SupplierWithIOException<Object>() {

        @Override
        public Object apply() throws IOException {
            return this;
        }
    };

    private final InputStream _is;
    private int _buffer;
    private int _bitsOnBuffer;
    private boolean _closed;

    /**
     * Create a new instance wrapping the given InputStream.
     * @param is InputStream used to read.
     */
    public InputBitStream(InputStream is) {
        if (is == null) {
            throw new IllegalArgumentException();
        }

        _is = is;
    }

    /**
     * Close this stream and the wrapped one.
     * @throws IOException if it is not possible to close it.
     *                     This is usually because it is already closed.
     */
    @Override
    public void close() throws IOException {
        _is.close();
        _closed = true;
    }

    private void assertNotClosed() {
        if (_closed) {
            throw new IllegalArgumentException("Stream already closed");
        }
    }

    /**
     * Read a single boolean from the stream.
     * <p>
     * A byte has 8 bits and a boolean can be represented with a single bits.
     * Thus, this method will only call {@link InputStream#read()} in the
     * wrapped stream once every 8 calls to this method, until reading all
     * bits from the previous read byte.
     * <p>
     * This is a key method and all other more complex methods within the class
     * depends on this.
     *
     * @return true or false depending on next bit value.
     * @throws IOException if it is unable to read from the wrapped stream or
     *                     this stream has been closed.
     */
    public boolean readBoolean() throws IOException {
        assertNotClosed();

        if (_bitsOnBuffer == 0) {
            _buffer = _is.read();
            if (_buffer < 0) {
                throw new IOException("Stream end already reached");
            }

            _bitsOnBuffer = 8;
        }

        boolean value = (_buffer & 1) != 0;
        _buffer >>>= 1;
        --_bitsOnBuffer;

        return value;
    }

    /**
     * Read a symbol from the stream according to the given Huffman table.
     * @param table Huffman table used to decode the symbol.
     * @param <E> Type of the symbol to decode.
     * @return The symbol found in the stream according too the Huffman table.
     * @throws IOException if it is not possible to read from the stream.
     */
    public <E> E readHuffmanSymbol(HuffmanTable<E> table) throws IOException {
        if (table == null) {
            throw new IllegalArgumentException();
        }

        if (table.symbolsWithBits(0) > 0) {
            return table.getSymbol(0, 0);
        }

        int value = 0;
        int base = 0;
        int bits = 1;

        while (true) {
            value = (value << 1) + (readBoolean() ? 1 : 0);
            base <<= 1;
            final int levelLength = table.symbolsWithBits(bits);
            final int levelIndex = value - base;
            if (levelIndex < levelLength) {
                return table.getSymbol(bits, levelIndex);
            }

            base += levelLength;
            bits++;
        }
    }

    /**
     * Read a Huffman table from the stream.
     * <p>
     * This is the complementary method of {@link OutputBitStream#writeHuffmanTable(DefinedHuffmanTable, ProcedureWithIOException, Procedure2WithIOException)}
     *
     * @param supplier Used to read each of the symbols from the stream.
     *                 This may not be called for all symbols if diffSupplier method is present.
     * @param diffSupplier Optional function used to write a symbol based on the previous one.
     * @param <E> Type of the decoded symbol expected in the Huffman table.
     * @return The HuffmanTable resulting of reading the stream.
     * @throws IOException if it is unable to read from the wrapped stream.
     *
     * @see OutputBitStream#writeHuffmanTable(DefinedHuffmanTable, ProcedureWithIOException, Procedure2WithIOException)
     */
    public <E> DefinedHuffmanTable<E> readHuffmanTable(
            SupplierWithIOException<E> supplier, FunctionWithIOException<E,E> diffSupplier) throws IOException {
        final ArrayList<Integer> levelLengths = new ArrayList<>();
        int max = 1;
        while (max > 0) {
            final int levelLength = readHuffmanSymbol(new RangedIntegerHuffmanTable(0, max));
            levelLengths.add(levelLength);
            max -= levelLength;
            max <<= 1;
        }

        final ArrayList<Iterable<E>> symbols = new ArrayList<>(levelLengths.size());
        for (int levelLength : levelLengths) {
            final ArrayList<E> level = new ArrayList<>();
            E element = null;
            if (levelLength > 0) {
                element = supplier.apply();
                level.add(element);
            }

            for (int i = 1; i < levelLength; i++) {
                if (diffSupplier != null) {
                    element = diffSupplier.apply(element);
                }
                else {
                    element = supplier.apply();
                }
                level.add(element);
            }

            symbols.add(level);
        }

        return DefinedHuffmanTable.fromIterable(symbols);
    }

    /**
     * Read a natural number (zero or positive integer) from the stream
     * in the same format {@link OutputBitStream#writeNaturalNumber(int)} writes it.
     * <p>
     * Ideally there is no upper limit for this number.
     * In reality it is currently limited by the 'int' boundaries.
     * @return The read number
     * @throws IOException if it is unable to read from the wrapped stream.
     */
    public int readNaturalNumber() throws IOException {
        return readHuffmanSymbol(naturalNumberHuffmanTable);
    }

    /**
     * Read a natural number (zero or positive integer) from the stream
     * in the same format {@link OutputBitStream#writeLongNaturalNumber(long)} writes it.
     * <p>
     * Ideally there is no upper limit for this number.
     * In reality it is currently limited by the 'long' boundaries.
     * @return The read number
     * @throws IOException if it is unable to read from the wrapped stream.
     */
    public long readLongNaturalNumber() throws IOException {
        return readHuffmanSymbol(longNaturalNumberHuffmanTable);
    }

    /**
     * Read an integer number from the stream in the same format
     * {@link OutputBitStream#writeIntegerNumber(int)} writes it.
     * <p>
     * Ideally there is no limits for this number.
     * In reality it is currently limited by the 'int' boundaries.
     * @return The read number
     * @throws IOException if it is unable to read from the wrapped stream.
     */
    public int readIntegerNumber() throws IOException {
        return readHuffmanSymbol(integerNumberHuffmanTable);
    }

    /**
     * Read an integer number from the stream in the same format
     * {@link OutputBitStream#writeLongIntegerNumber(long)} writes it.
     * <p>
     * Ideally there is no limits for this number.
     * In reality it is currently limited by the 'long' boundaries.
     * @return The read number
     * @throws IOException if it is unable to read from the wrapped stream.
     */
    public long readLongIntegerNumber() throws IOException {
        return readHuffmanSymbol(longIntegerNumberHuffmanTable);
    }

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
    public <K, V> Map<K, V> readMap(
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
    public <E> Set<E> readSet(
            CollectionLengthDecoder lengthDecoder,
            SupplierWithIOException<E> supplier,
            FunctionWithIOException<E, E> diffSupplier) throws IOException {

        return readMap(lengthDecoder, supplier, diffSupplier, nullSupplier).keySet();
    }

    /**
     * Read an arbitrary list from the stream.
     * <p>
     * This is the complemetary method of {@link sword.bitstream.OutputBitStream#writeList(CollectionLengthEncoder, java.util.List, ProcedureWithIOException)}.
     * Thus, assumes that the length is encoded first, and then all symbols are given in order after that.
     *
     * @param lengthDecoder Callback used once to read the number of symbols within the list.
     * @param supplier Decode a single symbol of the list.
     * @param <E> Type for the symbols within the list.
     * @return A list read from the stream.
     * @throws IOException Thrown only if any of the given callback throws it.
     */
    public <E> List<E> readList(
            CollectionLengthDecoder lengthDecoder,
            SupplierWithIOException<E> supplier) throws IOException {
        final int length = lengthDecoder.decodeLength();
        final ArrayList<E> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            result.add(supplier.apply());
        }

        return result;
    }

    /**
     * Read a set of range numbers from the stream.
     *
     * @param lengthDecoder Decoder used to read the size of the set.
     * @param min Minimum value expected for any of the values included in the set.
     * @param max Maximum value expected for any of the values included in the set.
     * @return A Set read from the stream. It can be empty, but never null.
     * @throws IOException if it is unable to write into the stream.
     */
    public Set<Integer> readRangedNumberSet(CollectionLengthDecoder lengthDecoder, int min, int max) throws IOException {
        final RangedIntegerDecoder decoder = new RangedIntegerDecoder(this, min, max);
        return readSet(lengthDecoder, decoder, decoder);
    }
}
