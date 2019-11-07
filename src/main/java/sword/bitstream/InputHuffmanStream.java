package sword.bitstream;

import sword.bitstream.huffman.DefinedHuffmanTable;
import sword.bitstream.huffman.HuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;

import java.io.IOException;
import java.util.*;

public interface InputHuffmanStream extends InputBitStream {

    /**
     * Read a symbol from the stream according to the given Huffman table.
     * @param table Huffman table used to decode the symbol.
     * @param <E> Type of the symbol to decode.
     * @return The symbol found in the stream according too the Huffman table.
     * @throws IOException if it is not possible to read from the stream.
     */
    default <E> E readHuffmanSymbol(HuffmanTable<E> table) throws IOException {
        if (table == null) {
            throw new IllegalArgumentException();
        }

        if (table.symbolsWithBits(0) > 0) {
            return table.getSymbol(0, 0);
        }

        int value = 0;
        int base = 0;
        int bits = 1;

        while (true) {
            value = (value << 1) + (readBoolean() ? 1 : 0);
            base <<= 1;
            final int levelLength = table.symbolsWithBits(bits);
            final int levelIndex = value - base;
            if (levelIndex < levelLength) {
                return table.getSymbol(bits, levelIndex);
            }

            base += levelLength;
            bits++;
        }
    }

    /**
     * Read a Huffman table from the stream.
     * <p>
     * This is the complementary method of {@link OutputHuffmanStream#writeHuffmanTable(DefinedHuffmanTable, ProcedureWithIOException, Procedure2WithIOException)}
     *
     * @param supplier Used to read each of the symbols from the stream.
     *                 This may not be called for all symbols if diffSupplier method is present.
     * @param diffSupplier Optional function used to write a symbol based on the previous one.
     * @param <E> Type of the decoded symbol expected in the Huffman table.
     * @return The HuffmanTable resulting of reading the stream.
     * @throws IOException if it is unable to read from the wrapped stream.
     *
     * @see OutputHuffmanStream#writeHuffmanTable(DefinedHuffmanTable, ProcedureWithIOException, Procedure2WithIOException)
     */
    default <E> DefinedHuffmanTable<E> readHuffmanTable(
            SupplierWithIOException<E> supplier,
            FunctionWithIOException<E, E> diffSupplier) throws IOException {
        final ArrayList<Integer> levelLengths = new ArrayList<>();
        int max = 1;
        while (max > 0) {
            final int levelLength = readHuffmanSymbol(new RangedIntegerHuffmanTable(0, max));
            levelLengths.add(levelLength);
            max -= levelLength;
            max <<= 1;
        }

        final ArrayList<Iterable<E>> symbols = new ArrayList<>(levelLengths.size());
        for (int levelLength : levelLengths) {
            final ArrayList<E> level = new ArrayList<>();
            E element = null;
            if (levelLength > 0) {
                element = supplier.apply();
                level.add(element);
            }

            for (int i = 1; i < levelLength; i++) {
                if (diffSupplier != null) {
                    element = diffSupplier.apply(element);
                }
                else {
                    element = supplier.apply();
                }
                level.add(element);
            }

            symbols.add(level);
        }

        return DefinedHuffmanTable.fromIterable(symbols);
    }
}
