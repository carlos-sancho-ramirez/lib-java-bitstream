package sword.bitstream;

import java.io.IOException;
import java.util.Comparator;

import sword.bitstream.huffman.IntegerNumberHuffmanTable;
import sword.bitstream.huffman.NaturalNumberHuffmanTable;

/**
 * Encode integer values into the stream.
 *
 * This class is suitable to encode a serie of non-repeated integer numbers, sorted in ascending order.
 * The first value is expected to be the minimum of the serie and it can be positive, negative or zero.
 * The following values are encoded based on the difference from the previous value.
 * As they are expected to be sorted in ascending order and not repeated, the following value
 * can be encoded as a new natural number resulting of the difference of values minus one.
 * This implementation does not allow having null values.
 *
 * @see IntegerDecoder
 */
public final class IntegerEncoder implements Comparator<Integer>, ProcedureWithIOException<Integer>, Procedure2WithIOException<Integer> {

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
