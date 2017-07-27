package sword.bitstream;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

/**
 * Wrapper for an {@link java.io.OutputStream} that provides optimal serialization
 * to compact and encode data into the stream.
 */
public class OutputBitStream implements Closeable {

    static final int NATURAL_NUMBER_BIT_ALIGNMENT = 8;
    private final NaturalNumberHuffmanTable naturalNumberHuffmanTable =
            new NaturalNumberHuffmanTable(NATURAL_NUMBER_BIT_ALIGNMENT);

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
     *
     * This method assumes that a byte has 8 bits and that a boolean can be
     * represented with a single bit. In other words, its possible to include
     * 8 booleans in each byte. With this assumption, this method will only
     * write into the wrapped stream once every 8 calls, once it has the values
     * for each of the booleans composing a byte.
     *
     * Note that calling {@link #close()} in this class is required in order to
     * store all buffered booleans before closing the stream.
     *
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
            bits++;
            for (E element : level) {
                if (symbol == null && element == null || symbol != null && symbol.equals(element)) {
                    for (int i = bits - 1; i >= 0; i--) {
                        writeBoolean((acc & (1 << i)) != 0);
                    }
                    return;
                }
                acc++;
            }
            acc <<= 1;
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
     *
     * As the symbol has a generic type, it is required that the caller of this
     * function provide the proper procedure to write each symbol.
     *
     * @param table Huffman table to encode.
     * @param proc Procedure to write a single symbol.
     * @param <E> Type of the symbol to encode.
     * @throws IOException if it is unable to write into the stream.
     */
    public <E> void writeHuffmanTable(DefinedHuffmanTable<E> table, ProcedureWithIOException<E> proc) throws IOException {
        int bits = 1;
        int max = 1;
        while (max > 0) {
            max <<= 1;
            final int levelLength = table.symbolsWithBits(bits++);
            writeRangedNumber(0, max, levelLength);
            max -= levelLength;
        }

        for (Iterable<E> level : table) {
            for (E element : level) {
                proc.apply(element);
            }
        }
    }

    /**
     * Writes a natural number (zero or positive integer) into the stream.
     * The number will be encoded with less bits if it is closer to zero and
     * increasing in number of bits if further.
     *
     * Ideally there is no upper limit for this number.
     * In reality it is currently limited by the 'long' boundaries.
     * @param number Value to write into the stream.
     * @throws IOException if it is unable to write into the stream.
     */
    public void writeNaturalNumber(long number) throws IOException {
        writeHuffmanSymbol(naturalNumberHuffmanTable, number);
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
     *
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

    /**
     * Write a char-typed Huffman table into the stream.
     * @param table table to be encoded.
     * @throws IOException if it is unable to write into the stream.
     */
    public void writeHuffmanCharTable(DefinedHuffmanTable<Character> table) throws IOException {
        writeHuffmanTable(table, this::writeChar);
    }
}
