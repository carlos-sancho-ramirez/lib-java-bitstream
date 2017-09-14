package sword.bitstream;

import java.io.IOException;
import java.util.Comparator;

import sword.bitstream.huffman.IntegerNumberHuffmanTable;
import sword.bitstream.huffman.NaturalNumberHuffmanTable;

/**
 * Encode Integer values into the stream. This implementation allow having null values.
 */
public class NullableIntegerEncoder implements Comparator<Integer>, ProcedureWithIOException<Integer>, Procedure2WithIOException<Integer> {

    private static final int BIT_ALIGNMENT = 8;
    static final IntegerNumberHuffmanTable integerTable = new IntegerNumberHuffmanTable(BIT_ALIGNMENT);
    static final NaturalNumberHuffmanTable naturalTable = new NaturalNumberHuffmanTable(BIT_ALIGNMENT);

    private final OutputBitStream _stream;

    public NullableIntegerEncoder(OutputBitStream stream) {
        _stream = stream;
    }

    @Override
    public int compare(Integer a, Integer b) {
        return (a == null || b != null && (a < b))? -1 : 1;
    }

    @Override
    public void apply(Integer element) throws IOException {
        if (element == null) {
            _stream.writeBoolean(false);
        }
        else {
            _stream.writeBoolean(true);
            _stream.writeHuffmanSymbol(integerTable, element);
        }
    }

    @Override
    public void apply(Integer previous, Integer element) throws IOException {
        if (previous == null) {
            _stream.writeHuffmanSymbol(integerTable, element);
        }
        else {
            _stream.writeHuffmanSymbol(naturalTable, element - previous - 1);
        }
    }
}
