package sword.bitstream;

import java.io.IOException;

import sword.bitstream.huffman.HuffmanTable;

/**
 * Decode symbols using a provided HuffmanTable
 */
public class HuffmanTableLengthDecoder implements CollectionLengthDecoder {

    private final InputBitStream _stream;
    private final HuffmanTable<Integer> _table;

    HuffmanTableLengthDecoder(InputBitStream stream, HuffmanTable<Integer> table) {
        _stream = stream;
        _table = table;
    }

    @Override
    public int decodeLength() throws IOException {
        return _stream.readHuffmanSymbol(_table);
    }
}
