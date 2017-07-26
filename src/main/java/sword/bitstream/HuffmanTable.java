package sword.bitstream;

public interface HuffmanTable<E> extends Iterable<Iterable<E>> {
    int symbolsAtLevel(int level);
    E getSymbol(int level, int index);
}
