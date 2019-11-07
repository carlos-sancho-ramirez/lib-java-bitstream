package sword.bitstream;

import java.io.IOException;

import sword.bitstream.huffman.HuffmanTable;

/**
 * Encode symbols using a provided HuffmanTable
 */
public class HuffmanTableLengthEncoder implements CollectionLengthEncoder {

    private final OutputHuffmanStream _stream;
    private final HuffmanTable<Integer> _table;

    public HuffmanTableLengthEncoder(OutputHuffmanStream stream, HuffmanTable<Integer> table) {
        _stream = stream;
        _table = table;
    }

    @Override
    public void encodeLength(int length) throws IOException {
        _stream.writeHuffmanSymbol(_table, length);
    }
}
