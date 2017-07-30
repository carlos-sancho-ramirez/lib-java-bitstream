package sword.bitstream;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;

import static sword.bitstream.OutputBitStream.NATURAL_NUMBER_BIT_ALIGNMENT;

/**
 * Wrapper for a Java InputStream that adds functionality to read serialiazed content.
 *
 * This is a complementary class for {@link OutputBitStream}. Thus, this class
 * provides lot of methods to read what the complementary class has written in
 * to the output stream.
 */
public class InputBitStream implements Closeable {

    private final NaturalNumberHuffmanTable naturalNumberHuffmanTable =
            new NaturalNumberHuffmanTable(NATURAL_NUMBER_BIT_ALIGNMENT);

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
     *
     * A byte has 8 bits and a boolean can be represented with a single bits.
     * Thus, this method will only call {@link InputStream#read()} in the
     * wrapped stream once every 8 calls to this method, until reading all
     * bits from the previous read byte.
     *
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
     * Read a value assuming that the value can only
     * be inside a range of values.
     * This is a complementary method for {@link OutputBitStream#writeRangedNumber(int, int, int)}.
     *
     * @param min Minimum number allowed in the range (inclusive)
     * @param max Maximum number allowed in the range (inclusive)
     * @return the decoded number read from the stream according to the provided range.
     * @throws IOException if it is unable to read from the wrapped stream.
     */
    public int readRangedNumber(int min, int max) throws IOException {
        final int normMax = max - min;
        if (normMax < 0) {
            throw new IllegalArgumentException("minimum should be lower or equal than maximum");
        }

        final int possibilities = max - min + 1;
        int maxBits = 0;
        while (possibilities > (1 << maxBits)) {
            maxBits++;
        }

        final int limit = (1 << maxBits) - possibilities;
        final int minBits = (limit == 0)? maxBits : maxBits - 1;

        int result = 0;
        for (int i = minBits - 1; i >= 0; i--) {
            if (readBoolean()) {
                result |= 1 << i;
            }
        }

        if (maxBits > minBits && result >= limit) {
            result <<= 1;
            if (readBoolean()) {
                result += 1;
            }
            result -= limit;
        }

        return result + min;
    }

    /**
     * Read a Huffman table from the stream.
     * @param supplier Used to read each of the symbols from the stream.
     * @param <E> Type of the decoded symbol expected in the Huffman table.
     * @return The HuffmanTable resulting of reading the stream.
     * @throws IOException if it is unable to read from the wrapped stream.
     */
    public <E> DefinedHuffmanTable<E> readHuffmanTable(SupplierWithIOException<E> supplier) throws IOException {
        final ArrayList<Integer> levelLengths = new ArrayList<>();
        int max = 1;
        while (max > 0) {
            final int levelLength = readRangedNumber(0, max);
            levelLengths.add(levelLength);
            max -= levelLength;
            max <<= 1;
        }

        final ArrayList<Iterable<E>> symbols = new ArrayList<>(levelLengths.size());
        for (int levelLength : levelLengths) {
            final ArrayList<E> level = new ArrayList<>();
            for (int i = 0; i < levelLength; i++) {
                level.add(supplier.apply());
            }
            symbols.add(level);
        }

        return new DefinedHuffmanTable<>(symbols);
    }

    /**
     * Read a natural number (zero or positive integer) from the stream
     * in the same format {@link OutputBitStream#writeNaturalNumber(long)} writes it.
     *
     * Ideally there is no upper limit for this number.
     * In reality it is currently limited by the 'long' boundaries.
     * @return The read number
     * @throws IOException if it is unable to read from the wrapped stream.
     */
    public long readNaturalNumber() throws IOException {
        return readHuffmanSymbol(naturalNumberHuffmanTable);
    }

    /**
     * Read a single char from the stream
     * @return the char read from the stream
     * @throws IOException if it is unable to read from the wrapped stream.
     */
    public char readChar() throws IOException {
        return (char) readNaturalNumber();
    }

    /**
     * Read a string of characters from the stream.
     * This method is complementary of {@link OutputBitStream#writeString(String)}.
     * Thus the original value given to that method should be returned here.
     * @return the string of characters read from the stream.
     * @throws IOException if it is unable to read from the wrapped stream.
     */
    public String readString() throws IOException {
        final int length = (int) readNaturalNumber();
        final StringBuilder str = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            str.append(readChar());
        }

        return str.toString();
    }

    /**
     * Read a string of characters from the stream assuming that
     * the given sorted set of chars are the only possibilities
     * that can be found and that it is the same probability for
     * each of the possibilities.
     *
     * @param charSet Array of char containing all possible
     *                characters that the string may contain
     *                in the same order it was given when encoded.
     * @return The decoded string according to the charSet provided.
     * @throws IOException if it is unable to read from the wrapped stream.
     */
    public String readString(char[] charSet) throws IOException {
        final int max = charSet.length - 1;
        final int length = (int) readNaturalNumber();
        final StringBuilder str = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            final int index = readRangedNumber(0, max);
            str.append(charSet[index]);
        }

        return str.toString();
    }

    /**
     * Read a char-types Huffman table.
     * @return The resulting Huffman table on reading and decoding from the stream.
     * @throws IOException if it is unable to read from the wrapped stream.
     */
    public HuffmanTable<Character> readHuffmanCharTable() throws IOException {
        return readHuffmanTable(this::readChar);
    }
}
