package sword.bitstream;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;

public class InputBitStream implements Closeable {

    private final InputStream _is;
    private int _buffer;
    private int _bitsOnBuffer;
    private boolean _closed;

    public InputBitStream(InputStream is) {
        if (is == null) {
            throw new IllegalArgumentException();
        }

        _is = is;
    }

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
     * Reads a natural number (zero or positive integer) from the stream
     * in the same format {@link OutputBitStream#writeNaturalNumber(long)} writes it.
     *
     * Ideally there is no upper limit for this number.
     * In reality it is currently limited by the 'long' boundaries.
     */
    public long readNaturalNumber() throws IOException {
        long base = 0;
        int bytes = 1;
        while (readBoolean()) {
            base += 1 << (7 * bytes);
            bytes++;
        }

        long value = 0;
        for (int i = 0; i < 7 * bytes; i++) {
            if (readBoolean()) {
                value |= 1 << i;
            }
        }

        return value + base;
    }

    /**
     * Read a single char from the stream
     */
    public char readChar() throws IOException {
        return (char) readNaturalNumber();
    }

    /**
     * Read a string of characters from the stream.
     * This method is complementary of {@link OutputBitStream#writeString(String)}.
     * Thus the original value given to that method should be returned here.
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
     * Read a value assuming that the value can only
     * be inside a range of values.
     * @param min Minimum number allowed in the range (inclusive)
     * @param max Maximum number allowed in the range (inclusive)
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

        if (result >= limit) {
            result <<= 1;
            if (readBoolean()) {
                result += 1;
            }
            result -= limit;
        }

        return result + min;
    }

    /**
     * Read a string of characters from the stream assuming that
     * the given sorted set of chars are the only possibilities
     * that can be found.
     *
     * @param charSet Array of char containing all possible
     *                characters that the string may contain
     *                in the same order it was given when encoded.
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
}
