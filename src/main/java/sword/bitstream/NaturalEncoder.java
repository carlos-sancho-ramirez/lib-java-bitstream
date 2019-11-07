package sword.bitstream;

import sword.bitstream.huffman.IntegerNumberHuffmanTable;
import sword.bitstream.huffman.NaturalNumberHuffmanTable;

import java.io.IOException;
import java.util.Comparator;

import static sword.bitstream.IntegerEncoder.naturalTable;

/**
 * Encode natural values into the stream.
 *
 * This class is suitable to encode a serie of non-repeated natural numbers, sorted in ascending order.
 * The first value is expected to be the minimum of the serie and never a negative number (zero is ok).
 * The following values are encoded based on the difference from the previous value.
 * As they are expected to be sorted in ascending order and not repeated, the following value
 * can be encoded as a new natural number resulting of the difference of values minus one.
 * This implementation does not allow having null values.
 *
 * @see NaturalDecoder
 */
public final class NaturalEncoder implements Comparator<Integer>, ProcedureWithIOException<Integer>, Procedure2WithIOException<Integer> {

    private final OutputHuffmanStream _stream;

    public NaturalEncoder(OutputHuffmanStream stream) {
        _stream = stream;
    }

    @Override
    public int compare(Integer a, Integer b) {
        return a - b;
    }

    @Override
    public void apply(Integer element) throws IOException {
        _stream.writeHuffmanSymbol(naturalTable, element);
    }

    @Override
    public void apply(Integer previous, Integer element) throws IOException {
        _stream.writeHuffmanSymbol(naturalTable, element - previous - 1);
    }
}
