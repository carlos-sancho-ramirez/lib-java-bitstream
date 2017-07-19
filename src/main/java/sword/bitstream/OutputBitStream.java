package sword.bitstream;

import java.io.Closeable;
import java.io.IOException;
import java.io.OutputStream;

public class OutputBitStream implements Closeable {

    private final OutputStream _os;
    private int _buffer;
    private int _bitsOnBuffer;

    public OutputBitStream(OutputStream os) {
        if (os == null) {
            throw new IllegalArgumentException();
        }

        _os = os;
    }

    @Override
    public void close() throws IOException {
        _os.close();
    }

    private void flushByte() throws IOException {
        while (_bitsOnBuffer >= 8) {
            _os.write(_buffer);
            _buffer >>>= 8;
            _bitsOnBuffer -= 8;
        }
    }

    public void writeBoolean(boolean value) throws IOException {
        if (value) {
            _buffer |= 1 << _bitsOnBuffer;
        }

        _bitsOnBuffer++;
        flushByte();
    }
}
