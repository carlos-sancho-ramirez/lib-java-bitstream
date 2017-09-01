package sword.bitstream;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class BitStreamTest {

    private String dump(byte[] array) {
        final StringBuilder str = new StringBuilder("[");
        final int length = array.length;

        for (int i = 0; i < length; i++) {
            str.append("" + array[i] + ((i == length - 1)? "]" : ","));
        }

        return str.toString();
    }

    @Test
    public void evaluateReadAndWriteForNaturalNumbers() throws IOException {
        final long[] values = new long[] {
                0L, 1L, 5L, 127L, 128L, 145L, 16511L, 16512L, 2113662L, 2113663L, 2113664L
        };

        for (long value : values) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final OutputBitStream obs = new OutputBitStream(baos);

            obs.writeNaturalNumber(value);
            obs.close();

            final byte[] array = baos.toByteArray();
            final ByteArrayInputStream bais = new ByteArrayInputStream(array);
            final InputBitStream ibs = new InputBitStream(bais);

            final long readValue = ibs.readNaturalNumber();
            ibs.close();

            assertEquals("Array is " + dump(array), value, readValue);
        }
    }

    @Test
    public void evaluateReadAndWriteForIntegerNumbers() throws IOException {
        final long[] values = new long[] {
                0L, 1L, 5L, 62L, 63L, 64L, 8255L, 8256L, 8257L,
                -1L, -2L, -63L, -64L, -65L, -8256L, -8257L
        };

        for (long value : values) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final OutputBitStream obs = new OutputBitStream(baos);

            obs.writeIntegerNumber(value);
            obs.close();

            final byte[] array = baos.toByteArray();
            final ByteArrayInputStream bais = new ByteArrayInputStream(array);
            final InputBitStream ibs = new InputBitStream(bais);

            final long readValue = ibs.readIntegerNumber();
            ibs.close();

            assertEquals("Array is " + dump(array), value, readValue);
        }
    }

    @Test
    public void evaluateReadAndWriteAString() throws IOException {
        final String[] values = new String[] {
                "", "a", "A", "78", "いえ", "家"
        };

        for (String value : values) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final OutputBitStream obs = new OutputBitStream(baos);

            obs.writeString(value);
            obs.close();

            final byte[] array = baos.toByteArray();
            final ByteArrayInputStream bais = new ByteArrayInputStream(array);
            final InputBitStream ibs = new InputBitStream(bais);

            final String readValue = ibs.readString();
            ibs.close();

            assertEquals("Array is " + dump(array), value, readValue);
        }
    }

    private void checkReadAndWriteRangedNumbers(int start, int end, int[] values) throws IOException {
        for (int value : values) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final OutputBitStream obs = new OutputBitStream(baos);

            obs.writeRangedNumber(start, end, value);
            obs.close();

            final byte[] array = baos.toByteArray();
            final ByteArrayInputStream bais = new ByteArrayInputStream(array);
            final InputBitStream ibs = new InputBitStream(bais);

            final int readValue = ibs.readRangedNumber(start, end);
            ibs.close();

            assertEquals("Array is " + dump(array), value, readValue);
        }
    }

    @Test
    public void evaluateReadAndWriteRangedNumbers() throws IOException {
        checkReadAndWriteRangedNumbers(48, 57, new int[] {
                48, 49, 50, 53, 54, 57
        });

        checkReadAndWriteRangedNumbers(0, 3, new int[] {
                0, 1, 2, 3
        });
    }

    private void checkReadAndWriteAString(char[] charSet, String[] values) throws IOException {
        for (String value : values) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final OutputBitStream obs = new OutputBitStream(baos);

            obs.writeString(charSet, value);
            obs.close();

            final byte[] array = baos.toByteArray();
            final ByteArrayInputStream bais = new ByteArrayInputStream(array);
            final InputBitStream ibs = new InputBitStream(bais);

            final String readValue = ibs.readString(charSet);
            ibs.close();

            assertEquals("Array is " + dump(array), value, readValue);
        }
    }

    @Test
    public void evaluateReadAndWriteAStringWithNumericCharSet() throws IOException {
        final char[] charSet = new char[] {
                '0', '1', '2', '3', '4', '5', '6', '7', '8', '9'
        };

        final String[] values = new String[] {
                "", "0", "1", "4", "0133", "001900"
        };

        checkReadAndWriteAString(charSet, values);
    }

    @Test
    public void evaluateReadAndWriteAStringWithKanaCharSet() throws IOException {
        final char[] charSet = new char[] {
                'あ', 'い', 'う', 'え', 'お',
                'か', 'き', 'く', 'け', 'こ',
                'さ', 'し', 'す', 'せ', 'そ'
        };

        final String[] values = new String[] {
                "", "あ", "ああ", "いえ", "こい"
        };

        checkReadAndWriteAString(charSet, values);
    }

    @Test
    public void evaluateReadAndWriteHuffmanSymbol() throws IOException {
        final String[] symbols = new String[] {
                "a", "b", "c", null, "", "abc"
        };

        final String[] tableSymbols = { null, "a", "b", "c", "", "abc"};
        final int[] tableIndexes = new int[] { 0, 1, 1, 4};

        final HuffmanTable<String> huffmanTable = new DefinedHuffmanTable<>(tableIndexes, tableSymbols);

        for (String symbol : symbols) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final OutputBitStream obs = new OutputBitStream(baos);

            obs.writeHuffmanSymbol(huffmanTable, symbol);
            obs.close();

            final byte[] array = baos.toByteArray();
            final ByteArrayInputStream bais = new ByteArrayInputStream(array);
            final InputBitStream ibs = new InputBitStream(bais);

            final String readValue = ibs.readHuffmanSymbol(huffmanTable);
            ibs.close();

            assertEquals("Array is " + dump(array), symbol, readValue);
        }
    }

    private byte[] checkReadAndWriteHuffmanEncodingLoremIpsum(boolean withDiff) throws IOException {

        final String loremIpsum = "Lorem ipsum dolor sit amet, consectetur adipiscing elit." +
                " Suspendisse ornare elit nec iaculis facilisis. Donec vitae faucibus nisl," +
                " nec porta odio. Duis a quam quis turpis sodales ultricies. Nulla et diam " +
                "urna. Aenean porta ipsum ac elit tempus maximus. Nullam quis libero id odio" +
                " euismod tempor. Nam sed vehicula enim.";

        final int loremIpsumLength = loremIpsum.length();
        final ArrayList<Character> loremIpsumList = new ArrayList<>(loremIpsumLength);
        for (int i = 0; i < loremIpsumLength; i++) {
            loremIpsumList.add(loremIpsum.charAt(i));
        }

        final DefinedHuffmanTable<Character> huffmanTable = DefinedHuffmanTable.from(loremIpsumList, Character::compare);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final OutputBitStream obs = new OutputBitStream(baos);

        final HuffmanTable<Long> diffTable = new LongNaturalNumberHuffmanTable(4);
        final Procedure2WithIOException<Character> diffProc = (!withDiff)? null : (prev, elem) -> {
            long diff = elem - prev;
            assertTrue(diff > 0);
            obs.writeHuffmanSymbol(diffTable, diff);
        };

        obs.writeHuffmanTable(huffmanTable, obs::writeChar, diffProc);
        obs.writeNaturalNumber(loremIpsumLength);
        for (int i = 0; i < loremIpsumLength; i++) {
            obs.writeHuffmanSymbol(huffmanTable, loremIpsum.charAt(i));
        }

        obs.close();

        final byte[] array = baos.toByteArray();
        final ByteArrayInputStream bais = new ByteArrayInputStream(array);
        final InputBitStream ibs = new InputBitStream(bais);

        final FunctionWithIOException<Character, Character> diffSupplier = (!withDiff)? null : prev -> {
            long diff = ibs.readHuffmanSymbol(diffTable);
            return (char) (prev + diff);
        };

        assertEquals(huffmanTable, ibs.readHuffmanTable(ibs::readChar, diffSupplier));
        assertEquals(loremIpsumLength, ibs.readNaturalNumber());
        for (int i = 0; i < loremIpsumLength; i++) {
            assertEquals(loremIpsum.charAt(i), ibs.readHuffmanSymbol(huffmanTable).charValue());
        }
        ibs.close();

        return array;
    }

    @Test
    public void evaluateReadAndWriteHuffmanEncodedLoremIpsum() throws IOException {
        final byte[] result1 = checkReadAndWriteHuffmanEncodingLoremIpsum(false);
        final byte[] result2 = checkReadAndWriteHuffmanEncodingLoremIpsum(true);

        assertTrue(result1.length >= result2.length);
    }

    @Test
    public void evaluateObtainingMostSuitableNaturalNumberHuffmanTable() {
        final Map<Long, Integer> map = new HashMap<>();
        map.put(1L, 9);
        map.put(2L, 64);
        map.put(3L, 68);
        map.put(4L, 21);
        map.put(5L, 47);
        map.put(6L, 62);
        map.put(7L, 38);
        map.put(8L, 97);
        map.put(9L, 31);

        final int expectedBitAlign1 = 5;
        final int givenBitAlign1 = LongNaturalNumberHuffmanTable.withFrequencies(map).getBitAlign();
        assertEquals(expectedBitAlign1, givenBitAlign1);

        map.put(3L, 70);
        final int expectedBitAlign2 = 2;
        final int givenBitAlign2 = LongNaturalNumberHuffmanTable.withFrequencies(map).getBitAlign();
        assertEquals(expectedBitAlign2, givenBitAlign2);
    }

    @Test
    public void evaluateReadAndWriteHuffmanTableOfUniqueSymbol() throws IOException {
        final Map<Integer, Integer> frequencyMap = new HashMap<>();
        frequencyMap.put(1, 5);

        final DefinedHuffmanTable<Integer> huffmanTable = DefinedHuffmanTable.withFrequencies(frequencyMap, Integer::compare);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final OutputBitStream obs = new OutputBitStream(baos);

        obs.writeHuffmanTable(huffmanTable, value -> obs.writeNaturalNumber(value), null);
        obs.close();

        final byte[] array = baos.toByteArray();
        final ByteArrayInputStream bais = new ByteArrayInputStream(array);
        final InputBitStream ibs = new InputBitStream(bais);

        final DefinedHuffmanTable<Integer> givenHuffmanTable = ibs.readHuffmanTable(() -> (int) ibs.readNaturalNumber(), null);
        ibs.close();

        assertEquals(huffmanTable, givenHuffmanTable);
    }

    @Test
    public void evaluateReadAndWriteRangedNumberSet() throws IOException {
        final int[] intValues = new int[] {
                -49, -48, -47, -46, -3, -1, 0, 1, 2, 12, 13, 14, 15
        };

        final List<Integer> possibleLengths = Arrays.asList(0, 1, 2, 3);
        final DefinedHuffmanTable<Integer> lengthTable = DefinedHuffmanTable.from(possibleLengths, Integer::compare);

        for (int min : intValues) for (int max : intValues) if (min <= max) {
            for (int a : intValues) for (int b : intValues) for (int c : intValues) {
                final Set<Integer> set = new HashSet<>();
                if (a >= min && a <= max) set.add(a);
                if (b >= min && b <= max) set.add(b);
                if (c >= min && c <= max) set.add(c);

                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final OutputBitStream obs = new OutputBitStream(baos);

                obs.writeRangedNumberSet(lengthTable, min, max, set);
                obs.close();

                final byte[] array = baos.toByteArray();
                final ByteArrayInputStream bais = new ByteArrayInputStream(array);
                final InputBitStream ibs = new InputBitStream(bais);

                final Set<Integer> givenSet = ibs.readRangedNumberSet(lengthTable, min, max);
                ibs.close();

                assertEquals(set, givenSet);
            }
        }
    }

    private static final class MapEntry<K, V> implements Map.Entry<K, V> {

        private final K _key;
        private final V _value;

        MapEntry(K key, V value) {
            _key = key;
            _value = value;
        }

        @Override
        public K getKey() {
            return _key;
        }

        @Override
        public V getValue() {
            return _value;
        }

        @Override
        public V setValue(V value) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class OrderedSetIterator<K, V> implements Iterator<Map.Entry<K, V>> {

        private final List<Map.Entry<K, V>> _entries;
        private final int _firstIndex;
        private int _index;

        OrderedSetIterator(List<Map.Entry<K, V>> entries, int firstIndex) {
            if (firstIndex < 0 || firstIndex >= entries.size()) {
                throw new IllegalArgumentException();
            }

            _entries = entries;
            _firstIndex = firstIndex;
            _index = firstIndex;
        }

        @Override
        public boolean hasNext() {
            return _index >= 0;
        }

        @Override
        public Map.Entry<K, V> next() {
            if (_index < 0) {
                throw new UnsupportedOperationException();
            }

            final Map.Entry<K, V> result = _entries.get(_index++);
            if (_index >= _entries.size()) {
                _index = 0;
            }

            if (_index == _firstIndex) {
                _index = -1;
            }

            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    private static final class OrderedSet<K, V> extends AbstractSet<Map.Entry<K, V>> {

        private final List<Map.Entry<K, V>> _entries;
        private int _firstIteratorIndex;

        OrderedSet(List<Map.Entry<K, V>> entries) {
            _entries = entries;
        }

        @Override
        public Iterator<Map.Entry<K, V>> iterator() {
            final OrderedSetIterator<K, V> it = new OrderedSetIterator<>(_entries, _firstIteratorIndex++);

            if (_firstIteratorIndex >= _entries.size()) {
                _firstIteratorIndex = 0;
            }

            return it;
        }

        @Override
        public int size() {
            return _entries.size();
        }
    }

    private static final class OrderedMap<K, V> extends AbstractMap<K, V> {

        private final OrderedSet<K, V> _set;

        OrderedMap(List<Map.Entry<K, V>> entries) {
            _set = new OrderedSet<>(entries);
        }

        @Override
        public Set<Entry<K, V>> entrySet() {
            return _set;
        }
    }

    @Test
    public void evaluateAlwaysSameHuffmanTableForSameFrequencyValues() throws IOException {
        final List<Map.Entry<Character, Integer>> entries = new ArrayList<>();
        entries.add(new MapEntry<>('a', 5));
        entries.add(new MapEntry<>('b', 5));
        entries.add(new MapEntry<>('c', 5));
        entries.add(new MapEntry<>('d', 7));
        entries.add(new MapEntry<>('e', 7));
        entries.add(new MapEntry<>('f', 5));
        entries.add(new MapEntry<>('g', 7));
        entries.add(new MapEntry<>('h', 8));
        entries.add(new MapEntry<>('i', 5));

        final OrderedMap<Character, Integer> map = new OrderedMap<>(entries);
        assertEquals(entries.iterator().next().getKey(), entries.iterator().next().getKey());
        assertNotEquals(map.entrySet().iterator().next().getKey(), map.entrySet().iterator().next().getKey());

        DefinedHuffmanTable<Character> table = DefinedHuffmanTable.withFrequencies(map, Character::compare);

        final int entryCount = entries.size();
        for (int i = 0; i < entryCount; i++) {
            DefinedHuffmanTable<Character> newTable = DefinedHuffmanTable.withFrequencies(map, Character::compare);
            assertEquals("Failed on iteration " + i, table, newTable);
        }
    }
}
