package sword.bitstream.huffman;

import java.util.Iterator;

abstract class AbstractIterator<T> implements Iterator<T> {

    @Override
    public void remove() {
        throw new UnsupportedOperationException("Unable to remove items");
    }
}
