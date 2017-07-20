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
}
