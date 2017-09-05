package sword.bitstream;

import java.io.IOException;

/**
 * Encode symbols using a provided HuffmanTable
 */
public class HuffmanTableLengthEncoder implements CollectionLengthEncoder {

    private final OutputBitStream _stream;
    private final HuffmanTable<Integer> _table;

    HuffmanTableLengthEncoder(OutputBitStream stream, HuffmanTable<Integer> table) {
        _stream = stream;
        _table = table;
    }

    @Override
    public void encodeLength(int length) throws IOException {
        _stream.writeHuffmanSymbol(_table, length);
    }
}
