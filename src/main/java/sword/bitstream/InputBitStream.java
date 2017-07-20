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
}
