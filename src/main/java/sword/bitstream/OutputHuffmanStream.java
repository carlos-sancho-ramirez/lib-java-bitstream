package sword.bitstream;

import sword.bitstream.huffman.DefinedHuffmanTable;
import sword.bitstream.huffman.HuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;

import java.io.IOException;
import java.util.*;

public interface OutputHuffmanStream extends OutputBitStream {

    /**
     * Write a symbol into the stream using the given Huffman table.
     * @param table Huffman table that specifies how to encode the symbol
     * @param symbol Symbol to encode. It must be present in the Huffman table.
     * @param <E> Type for the symbol to encode.
     * @throws IOException if it is unable to write into the stream.
     */
    default <E> void writeHuffmanSymbol(HuffmanTable<E> table, E symbol) throws IOException {
        int bits = 0;
        int acc = 0;
        for (Iterable<E> level : table) {
            for (E element : level) {
                if (symbol == null && element == null || symbol != null && symbol.equals(element)) {
                    if (bits > 0) {
                        for (int i = bits - 1; i >= 0; i--) {
                            writeBoolean((acc & (1 << i)) != 0);
                        }
                    }
                    return;
                }
                acc++;
            }
            acc <<= 1;
            bits++;
        }

        final String symbolString = (symbol != null)? symbol.toString() : "null";
        throw new IllegalArgumentException("Symbol <" + symbolString + "> is not included in the given Huffman table");
    }

    /**
     * Write a Huffman table into the stream.
     * <p>
     * As the symbol has a generic type, it is required that the caller of this
     * function provide the proper procedures to write each symbol.
     *
     * @param table Huffman table to encode.
     * @param proc Procedure to write a single symbol. This may not be called
     *             for all symbols if diffProc is different from null in order
     *             to reduce the amount of data to write.
     * @param diffProc Optional procedure to write a symbol based in a previous one.
     *                 This may compress in a better degree the table if their symbols are sortered.
     *                 In case this is null, the function given in proc will be called instead.
     * @param <E> Type of the symbol to encode.
     * @throws IOException if it is unable to write into the stream.
     */
    default <E> void writeHuffmanTable(DefinedHuffmanTable<E> table,
            ProcedureWithIOException<E> proc, Procedure2WithIOException<E> diffProc) throws IOException {
        int bits = 0;
        int max = 1;
        while (max > 0) {
            final int levelLength = table.symbolsWithBits(bits++);
            writeHuffmanSymbol(new RangedIntegerHuffmanTable(0, max), levelLength);
            max -= levelLength;
            max <<= 1;
        }

        for (Iterable<E> level : table) {
            Iterator<E> it = level.iterator();
            E previous = null;
            if (it.hasNext()) {
                previous = it.next();
                proc.apply(previous);
            }

            while (it.hasNext()) {
                E element = it.next();
                if (diffProc != null) {
                    diffProc.apply(previous, element);
                    previous = element;
                }
                else {
                    proc.apply(element);
                }
            }
        }
    }
}
