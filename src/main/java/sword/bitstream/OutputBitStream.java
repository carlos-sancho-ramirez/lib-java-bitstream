package sword.bitstream;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

public class OutputBitStream implements Closeable {

    private final OutputStream _os;
    private int _buffer;
    private int _bitsOnBuffer;
    private boolean _closed;

    public OutputBitStream(OutputStream os) {
        if (os == null) {
            throw new IllegalArgumentException();
        }

        _os = os;
    }

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

    public void writeBoolean(boolean value) throws IOException {
        assertNotClosed();

        if (value) {
            _buffer |= 1 << _bitsOnBuffer;
        }

        _bitsOnBuffer++;
        flushByte();
    }

    /**
     * Writes a natural number (zero or positive integer) into the stream.
     * The number will be encoded with less bit if it is closer to zero and
     * increasing in number of bits if further.
     *
     * Ideally there is no upper limit for this number.
     * In reality it is currently limited by the 'long' boundaries.
     */
    public void writeNaturalNumber(long number) throws IOException {
        long base = 0;
        long nextBase = 128; // 1 << 7
        int bytes = 1;

        while (number >= nextBase) {
            base = nextBase;
            bytes++;
            nextBase += 1 << (7 * bytes);
        }

        for (int i = 1; i < bytes; i++) {
            writeBoolean(true);
        }
        writeBoolean(false);

        long encNumber = number - base;
        for (int i = 0; i < bytes * 7; i++) {
            writeBoolean((encNumber & 1) != 0);
            encNumber >>>= 1;
        }
    }

    /**
     * Write a single char into the stream
     */
    public void writeChar(char character) throws IOException {
        writeNaturalNumber((int) character);
    }

    /**
     * Write a string of characters into the stream.
     * This method allows empty strings but not null ones.
     */
    public void writeString(String str) throws IOException {
        final int length = str.length();
        writeNaturalNumber(length);
        for (int i = 0; i < length; i++) {
            writeChar(str.charAt(i));
        }
    }

    /**
     * Write a symbol into the stream using the given Huffman table.
     * @param table Huffman table that specifies how to encode the symbol
     * @param symbol Symbol to encode. It must be present in the Huffman table.
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
     * Write a string of characters into the stream assuming that
     * the given sorted set of chars are the only possibilities
     * that can be found.
     *
     * This method allows empty strings but not null ones.
     * @param charSet Array of char containing all possible
     *                characters that the string may contain.
     * @param str String to be codified, serialised and included
     *            in the stream.
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
     * Write a Huffman table into the stream.
     *
     * As the symbol has a generic type, it is required that the caller of this
     * function provide the proper procedure to write each symbol.
     *
     * @param table Huffman table to encode.
     * @param proc Procedure to write a single symbol.
     * @param <E> Type of the symbol to encode.
     */
    public <E> void writeHuffmanTable(HuffmanTable<E> table, ProcedureWithIOException<E> proc) throws IOException {
        int levelIndex = 0;
        int max = 1;
        while (max > 0) {
            max <<= 1;
            final int levelLength = table.symbolsAtLevel(levelIndex++);
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
     * Write a char-typed Huffman table into the stream.
     * @param table table to be encoded
     */
    public void writeHuffmanCharTable(HuffmanTable<Character> table) throws IOException {
        writeHuffmanTable(table, this::writeChar);
    }
}
