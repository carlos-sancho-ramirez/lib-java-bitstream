package sword.bitstream;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Set;

/**
 * Wrapper for an {@link java.io.OutputStream} that provides optimal serialization
 * to compact and encode data into the stream.
 */
public class OutputBitStream implements Closeable {

    static final int NATURAL_NUMBER_BIT_ALIGNMENT = 8;
    static final int INTEGER_NUMBER_BIT_ALIGNMENT = NATURAL_NUMBER_BIT_ALIGNMENT;

    private final NaturalNumberHuffmanTable naturalNumberHuffmanTable =
            new NaturalNumberHuffmanTable(NATURAL_NUMBER_BIT_ALIGNMENT);
    private final IntegerNumberHuffmanTable integerNumberHuffmanTable =
            new IntegerNumberHuffmanTable(INTEGER_NUMBER_BIT_ALIGNMENT);

    private final LongNaturalNumberHuffmanTable longNaturalNumberHuffmanTable =
            new LongNaturalNumberHuffmanTable(NATURAL_NUMBER_BIT_ALIGNMENT);
    private final LongIntegerNumberHuffmanTable longIntegerNumberHuffmanTable =
            new LongIntegerNumberHuffmanTable(INTEGER_NUMBER_BIT_ALIGNMENT);

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
     * Write the given value assuming that the value can only
     * be inside a range of values.
     * @param min Minimum number allowed in the range (inclusive)
     * @param max Maximum number allowed in the range (inclusive)
     * @param value Value to codify
     * @throws IOException if it is unable to write into the stream.
     */
    public void writeRangedNumber(int min, int max, int value) throws IOException {
        final int normMax = max - min;
        if (normMax < 0) {
            throw new IllegalArgumentException("minimum should be lower or equal than maximum");
        }

        final int normValue = value - min;
        if (normValue < 0 || normValue > normMax) {
            throw new IllegalArgumentException("value should be within the range");
        }

        final int possibilities = max - min + 1;
        int maxBits = 0;
        while (possibilities > (1 << maxBits)) {
            maxBits++;
        }

        final int limit = (1 << maxBits) - possibilities;

        if (normValue < limit) {
            for (int i = maxBits - 2; i >= 0; i--) {
                writeBoolean((normValue & (1 << i)) != 0);
            }
        }
        else {
            final int encValue = normValue + limit;
            for (int i = maxBits - 1; i >= 0; i--) {
                writeBoolean((encValue & (1 << i)) != 0);
            }
        }
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
            writeRangedNumber(0, max, levelLength);
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
     * Writes a natural number (zero or positive integer) into the stream.
     * The number will be encoded with less bits if it is closer to zero and
     * increasing in number of bits if further.
     * <p>
     * Ideally there is no upper limit for this number.
     * In reality it is currently limited by the 'long' boundaries.
     * @param number Value to write into the stream.
     *               This must be zero or positive, never negative.
     * @throws IOException if it is unable to write into the stream.
     */
    public void writeNaturalNumber(int number) throws IOException {
        if (number < 0) {
            throw new IllegalArgumentException("Negative numbers are not allowed");
        }

        writeHuffmanSymbol(naturalNumberHuffmanTable, number);
    }

    /**
     * Writes a natural number (zero or positive integer) into the stream.
     * The number will be encoded with less bits if it is closer to zero and
     * increasing in number of bits if further.
     * <p>
     * Ideally there is no upper limit for this number.
     * In reality it is currently limited by the 'long' boundaries.
     * @param number Value to write into the stream.
     *               This must be zero or positive, never negative.
     * @throws IOException if it is unable to write into the stream.
     */
    public void writeLongNaturalNumber(long number) throws IOException {
        if (number < 0) {
            throw new IllegalArgumentException("Negative numbers are not allowed");
        }

        writeHuffmanSymbol(longNaturalNumberHuffmanTable, number);
    }

    /**
     * Writes an integer number into the stream.
     * The number will be encoded with less bits if it is closer to zero and
     * increasing in number of bits if further.
     * <p>
     * Ideally there is no limit for this number.
     * In reality it is currently limited by the 'int' boundaries.
     * @param number Value to write into the stream.
     * @throws IOException if it is unable to write into the stream.
     */
    public void writeIntegerNumber(int number) throws IOException {
        writeHuffmanSymbol(integerNumberHuffmanTable, number);
    }

    /**
     * Writes an integer number into the stream.
     * The number will be encoded with less bits if it is closer to zero and
     * increasing in number of bits if further.
     * <p>
     * Ideally there is no limit for this number.
     * In reality it is currently limited by the 'long' boundaries.
     * @param number Value to write into the stream.
     * @throws IOException if it is unable to write into the stream.
     */
    public void writeLongIntegerNumber(long number) throws IOException {
        writeHuffmanSymbol(longIntegerNumberHuffmanTable, number);
    }

    /**
     * Write a single char into the stream.
     * @param character Value to be written into the stream.
     * @throws IOException if it is unable to write into the stream.
     */
    public void writeChar(char character) throws IOException {
        writeNaturalNumber((int) character);
    }

    /**
     * Write a string of characters into the stream.
     * This method allows empty strings but not null ones.
     * @param str String to be written into the stream.
     * @throws IOException if it is unable to write into the stream.
     */
    public void writeString(String str) throws IOException {
        final int length = str.length();
        writeNaturalNumber(length);
        for (int i = 0; i < length; i++) {
            writeChar(str.charAt(i));
        }
    }

    /**
     * Write a string of characters into the stream assuming that
     * the given sorted set of chars are the only possibilities
     * that can be found.
     * <p>
     * This method allows empty strings but not null ones.
     * @param charSet Array of char containing all possible
     *                characters that the string may contain.
     * @param str String to be codified, serialised and included
     *            in the stream.
     * @throws IOException if it is unable to write into the stream.
     */
    public void writeString(char[] charSet, String str) throws IOException {
        final int max = charSet.length - 1;
        final int length = str.length();
        writeNaturalNumber(length);
        for (int i = 0; i < length; i++) {
            final char thisChar = str.charAt(i);
            int charValue = 0;
            while (charSet[charValue] != thisChar) {
                ++charValue;

                if (charValue > max) {
                    throw new IllegalArgumentException("Found char within the string that was not included in the given charSet");
                }
            }

            writeRangedNumber(0, max, charValue);
        }
    }

    ProcedureWithIOException<Character> _charWriter = new ProcedureWithIOException<Character>() {

        @Override
        public void apply(Character element) throws IOException {
            writeChar(element);
        }
    };

    /**
     * Write a char-typed Huffman table into the stream.
     * @param table table to be encoded.
     * @throws IOException if it is unable to write into the stream.
     */
    public void writeHuffmanCharTable(DefinedHuffmanTable<Character> table) throws IOException {
        writeHuffmanTable(table, _charWriter, null);
    }

    /**
     * Write a set of range numbers into the stream.
     *
     * @param lengthTable HuffmanTable used to write the size of the set.
     * @param min Minimum value expected for any of the values included in the set.
     * @param max Maximum value expected for any of the values included in the set.
     * @param valueSet Set of values to be written into the stream.
     *                 All its values must be between min and max inclusive.
     *                 It can be empty, but never null.
     * @throws IOException if it is unable to write into the stream.
     */
    public void writeRangedNumberSet(HuffmanTable<Integer> lengthTable, int min, int max, Set<Integer> valueSet) throws IOException {
        if (max < min) {
            throw new IllegalArgumentException("minimum should be lower or equal than maximum");
        }

        final int length = valueSet.size();
        final Iterator<Integer> it = valueSet.iterator();
        final int[] values = new int[length];
        for (int i = 0; i < length; i++) {
            values[i] = it.next();
        }
        Arrays.sort(values);

        if (length > 0 && (values[0] < min || values[length - 1] > max)) {
            throw new IllegalArgumentException("set values should be within the range");
        }

        writeHuffmanSymbol(lengthTable, length);
        int newMin = min;
        for (int i = 0; i < length; i++) {
            int newValue = values[i];
            writeRangedNumber(newMin, max - (length - i - 1), newValue);
            newMin = newValue + 1;
        }
    }
}
