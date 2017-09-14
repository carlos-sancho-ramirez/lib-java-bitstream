package sword.bitstream;

import java.io.IOException;
import java.util.Comparator;

import sword.bitstream.huffman.IntegerNumberHuffmanTable;
import sword.bitstream.huffman.NaturalNumberHuffmanTable;

/**
 * Encode Integer values into the stream. This implementation does not allow having null values.
 */
public class IntegerEncoder implements Comparator<Integer>, ProcedureWithIOException<Integer>, Procedure2WithIOException<Integer> {

    private static final int BIT_ALIGNMENT = 8;
    static final IntegerNumberHuffmanTable integerTable = new IntegerNumberHuffmanTable(BIT_ALIGNMENT);
    static final NaturalNumberHuffmanTable naturalTable = new NaturalNumberHuffmanTable(BIT_ALIGNMENT);

    private final OutputBitStream _stream;

    public IntegerEncoder(OutputBitStream stream) {
        _stream = stream;
    }

    @Override
    public int compare(Integer a, Integer b) {
        return a - b;
    }

    @Override
    public void apply(Integer element) throws IOException {
        _stream.writeHuffmanSymbol(integerTable, element);
    }

    @Override
    public void apply(Integer previous, Integer element) throws IOException {
        _stream.writeHuffmanSymbol(naturalTable, element - previous - 1);
    }
}
