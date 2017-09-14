package sword.bitstream;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import sword.bitstream.huffman.CharHuffmanTable;
import sword.bitstream.huffman.DefinedHuffmanTable;
import sword.bitstream.huffman.HuffmanTable;
import sword.bitstream.huffman.IntegerNumberHuffmanTable;
import sword.bitstream.huffman.LongIntegerNumberHuffmanTable;
import sword.bitstream.huffman.LongNaturalNumberHuffmanTable;
import sword.bitstream.huffman.NaturalNumberHuffmanTable;
import sword.bitstream.huffman.RangedIntegerHuffmanTable;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

public class BitStreamTest {

    private static final int BIT_ALIGNMENT = 8;
    private static final NaturalNumberHuffmanTable naturalTable = new NaturalNumberHuffmanTable(BIT_ALIGNMENT);
    private static final IntegerNumberHuffmanTable integerTable = new IntegerNumberHuffmanTable(BIT_ALIGNMENT);
    private static final LongNaturalNumberHuffmanTable longNaturalTable = new LongNaturalNumberHuffmanTable(BIT_ALIGNMENT);
    private static final LongIntegerNumberHuffmanTable longIntegerTable = new LongIntegerNumberHuffmanTable(BIT_ALIGNMENT);

    private final Comparator<Character> charComparator = new Comparator<Character>() {
        @Override
        public int compare(Character a, Character b) {
            return Character.compare(a, b);
        }
    };

    private final Comparator<Integer> intComparator = new Comparator<Integer>() {
        @Override
        public int compare(Integer a, Integer b) {
            return Integer.compare(a, b);
        }
    };

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
        final int[] values = new int[] {
                0, 1, 5, 127, 128, 145, 16511, 16512, 2113662, 2113663, 2113664
        };

        for (int value : values) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final OutputBitStream obs = new OutputBitStream(baos);

            obs.writeHuffmanSymbol(naturalTable, value);
            obs.close();

            final byte[] array = baos.toByteArray();
            final ByteArrayInputStream bais = new ByteArrayInputStream(array);
            final InputBitStream ibs = new InputBitStream(bais);

            final int readValue = ibs.readHuffmanSymbol(naturalTable);
            ibs.close();

            assertEquals("Array is " + dump(array), value, readValue);
        }
    }

    @Test
    public void evaluateReadAndWriteForLongNaturalNumbers() throws IOException {
        final long[] values = new long[] {
                0L, 1L, 5L, 127L, 128L, 145L, 16511L, 16512L, 2113662L, 2113663L, 2113664L
        };

        for (long value : values) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final OutputBitStream obs = new OutputBitStream(baos);

            obs.writeHuffmanSymbol(longNaturalTable, value);
            obs.close();

            final byte[] array = baos.toByteArray();
            final ByteArrayInputStream bais = new ByteArrayInputStream(array);
            final InputBitStream ibs = new InputBitStream(bais);

            final long readValue = ibs.readHuffmanSymbol(longNaturalTable);
            ibs.close();

            assertEquals("Array is " + dump(array), value, readValue);
        }
    }

    @Test
    public void evaluateReadAndWriteForIntegerNumbers() throws IOException {
        final int[] values = new int[] {
                0, 1, 5, 62, 63, 64, 8255, 8256, 8257,
                -1, -2, -63, -64, -65, -8256, -8257
        };

        for (int value : values) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final OutputBitStream obs = new OutputBitStream(baos);

            obs.writeHuffmanSymbol(integerTable, value);
            obs.close();

            final byte[] array = baos.toByteArray();
            final ByteArrayInputStream bais = new ByteArrayInputStream(array);
            final InputBitStream ibs = new InputBitStream(bais);

            final int readValue = ibs.readHuffmanSymbol(integerTable);
            ibs.close();

            assertEquals("Array is " + dump(array), value, readValue);
        }
    }

    @Test
    public void evaluateReadAndWriteForLongIntegerNumbers() throws IOException {
        final long[] values = new long[] {
                0L, 1L, 5L, 62L, 63L, 64L, 8255L, 8256L, 8257L,
                -1L, -2L, -63L, -64L, -65L, -8256L, -8257L
        };

        for (long value : values) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final OutputBitStream obs = new OutputBitStream(baos);

            obs.writeHuffmanSymbol(longIntegerTable, value);
            obs.close();

            final byte[] array = baos.toByteArray();
            final ByteArrayInputStream bais = new ByteArrayInputStream(array);
            final InputBitStream ibs = new InputBitStream(bais);

            final long readValue = ibs.readHuffmanSymbol(longIntegerTable);
            ibs.close();

            assertEquals("Array is " + dump(array), value, readValue);
        }
    }

    @Test
    public void evaluateReadAndWriteAString() throws IOException {
        final String[] values = new String[] {
                "", "a", "A", "78", "いえ", "家"
        };

        final HuffmanTable<Character> table = new CharHuffmanTable(8);

        for (String value : values) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final OutputBitStream obs = new OutputBitStream(baos);

            final ProcedureWithIOException<Character> writer = new ProcedureWithIOException<Character>() {
                @Override
                public void apply(Character element) throws IOException {
                    obs.writeHuffmanSymbol(table, element);
                }
            };

            obs.writeList(new LengthEncoder(obs), stringAsCharList(value), writer);
            obs.close();

            final byte[] array = baos.toByteArray();
            final ByteArrayInputStream bais = new ByteArrayInputStream(array);
            final InputBitStream ibs = new InputBitStream(bais);

            final SupplierWithIOException<Character> supplier = new SupplierWithIOException<Character>() {
                @Override
                public Character apply() throws IOException {
                    return ibs.readHuffmanSymbol(table);
                }
            };

            final String readValue = charListAsString(ibs.readList(new LengthDecoder(ibs), supplier));
            ibs.close();

            assertEquals("Array is " + dump(array), value, readValue);
        }
    }

    private void checkReadAndWriteRangedNumbers(int start, int end, int[] values) throws IOException {
        for (int value : values) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final OutputBitStream obs = new OutputBitStream(baos);

            final RangedIntegerHuffmanTable table = new RangedIntegerHuffmanTable(start, end);
            obs.writeHuffmanSymbol(table, value);
            obs.close();

            final byte[] array = baos.toByteArray();
            final ByteArrayInputStream bais = new ByteArrayInputStream(array);
            final InputBitStream ibs = new InputBitStream(bais);

            final int readValue = ibs.readHuffmanSymbol(table);
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

    private static List<Character> stringAsCharList(String value) {
        final int valueLength = value.length();
        final ArrayList<Character> valueAsList = new ArrayList<>(valueLength);
        for (int i = 0; i < valueLength; i++) {
            valueAsList.add(value.charAt(i));
        }

        return valueAsList;
    }

    private static String charListAsString(List<Character> list) {
        final StringBuilder sb = new StringBuilder();
        for (char v : list) {
            sb.append(v);
        }

        return sb.toString();
    }

    private void checkReadAndWriteAString(char[] charSet, String[] values) throws IOException {
        final int charSetLength = charSet.length;
        final ArrayList<Character> charList = new ArrayList<>(charSetLength);
        for (int i = 0; i < charSetLength; i++) {
            charList.add(charSet[i]);
        }

        final HuffmanTable<Character> table = DefinedHuffmanTable.from(charList, new Comparator<Character>() {

            @Override
            public int compare(Character a, Character b) {
                return Character.compare(a, b);
            }
        });

        for (String value : values) {
            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final OutputBitStream obs = new OutputBitStream(baos);

            final ProcedureWithIOException<Character> writer = new ProcedureWithIOException<Character>() {
                @Override
                public void apply(Character element) throws IOException {
                    obs.writeHuffmanSymbol(table, element);
                }
            };

            final int valueLength = value.length();
            final List<Character> valueAsList = stringAsCharList(value);
            obs.writeList(new LengthEncoder(obs), valueAsList, writer);
            obs.close();

            final byte[] array = baos.toByteArray();
            final ByteArrayInputStream bais = new ByteArrayInputStream(array);
            final InputBitStream ibs = new InputBitStream(bais);

            final SupplierWithIOException<Character> supplier = new SupplierWithIOException<Character>() {
                @Override
                public Character apply() throws IOException {
                    return ibs.readHuffmanSymbol(table);
                }
            };

            final String readValue = charListAsString(ibs.readList(new LengthDecoder(ibs), supplier));
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

        final DefinedHuffmanTable<Character> huffmanTable = DefinedHuffmanTable.from(loremIpsumList, charComparator);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final OutputBitStream obs = new OutputBitStream(baos);

        final HuffmanTable<Long> diffTable = new LongNaturalNumberHuffmanTable(4);

        final int charBitAlignment = 8;
        final ProcedureWithIOException<Character> proc = new ProcedureWithIOException<Character>() {

            private final CharHuffmanTable _table = new CharHuffmanTable(charBitAlignment);

            @Override
            public void apply(Character element) throws IOException {
                obs.writeHuffmanSymbol(_table, element);
            }
        };

        final Procedure2WithIOException<Character> diffProc = !withDiff? null : new Procedure2WithIOException<Character>() {
            @Override
            public void apply(Character previous, Character element) throws IOException {
                long diff = element - previous;
                assertTrue(diff > 0);
                obs.writeHuffmanSymbol(diffTable, diff);
            }
        };

        obs.writeHuffmanTable(huffmanTable, proc, diffProc);
        obs.writeHuffmanSymbol(naturalTable, loremIpsumLength);
        for (int i = 0; i < loremIpsumLength; i++) {
            obs.writeHuffmanSymbol(huffmanTable, loremIpsum.charAt(i));
        }

        obs.close();

        final byte[] array = baos.toByteArray();
        final ByteArrayInputStream bais = new ByteArrayInputStream(array);
        final InputBitStream ibs = new InputBitStream(bais);

        final SupplierWithIOException<Character> supplier = new SupplierWithIOException<Character>() {

            private final CharHuffmanTable _table = new CharHuffmanTable(charBitAlignment);

            @Override
            public Character apply() throws IOException {
                return ibs.readHuffmanSymbol(_table);
            }
        };

        final FunctionWithIOException<Character, Character> diffSupplier = (!withDiff)? null : new FunctionWithIOException<Character, Character>() {
            @Override
            public Character apply(Character previous) throws IOException {
                long diff = ibs.readHuffmanSymbol(diffTable);
                return (char) (previous + diff);
            }
        };

        assertEquals(huffmanTable, ibs.readHuffmanTable(supplier, diffSupplier));
        assertEquals(loremIpsumLength, ibs.readHuffmanSymbol(naturalTable).intValue());
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

        final DefinedHuffmanTable<Integer> huffmanTable = DefinedHuffmanTable.withFrequencies(frequencyMap, intComparator);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final OutputBitStream obs = new OutputBitStream(baos);

        final ProcedureWithIOException<Integer> proc = new ProcedureWithIOException<Integer>() {
            @Override
            public void apply(Integer element) throws IOException {
                obs.writeHuffmanSymbol(naturalTable, element);
            }
        };

        obs.writeHuffmanTable(huffmanTable, proc, null);
        obs.close();

        final byte[] array = baos.toByteArray();
        final ByteArrayInputStream bais = new ByteArrayInputStream(array);
        final InputBitStream ibs = new InputBitStream(bais);

        final SupplierWithIOException<Integer> supplier = new SupplierWithIOException<Integer>() {
            @Override
            public Integer apply() throws IOException {
                return ibs.readHuffmanSymbol(naturalTable);
            }
        };

        final DefinedHuffmanTable<Integer> givenHuffmanTable = ibs.readHuffmanTable(supplier, null);
        ibs.close();

        assertEquals(huffmanTable, givenHuffmanTable);
    }

    @Test
    public void evaluateReadAndWriteRangedNumberSet() throws IOException {
        final int[] intValues = new int[] {
                -49, -48, -47, -46, -3, -1, 0, 1, 2, 12, 13, 14, 15
        };

        final List<Integer> possibleLengths = Arrays.asList(0, 1, 2, 3);
        final DefinedHuffmanTable<Integer> lengthTable = DefinedHuffmanTable.from(possibleLengths, intComparator);

        for (int min : intValues) for (int max : intValues) if (min <= max) {
            for (int a : intValues) for (int b : intValues) for (int c : intValues) {
                final Set<Integer> set = new HashSet<>();
                if (a >= min && a <= max) set.add(a);
                if (b >= min && b <= max) set.add(b);
                if (c >= min && c <= max) set.add(c);

                final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                final OutputBitStream obs = new OutputBitStream(baos);

                obs.writeRangedNumberSet(new HuffmanTableLengthEncoder(obs, lengthTable), min, max, set);
                obs.close();

                final byte[] array = baos.toByteArray();
                final ByteArrayInputStream bais = new ByteArrayInputStream(array);
                final InputBitStream ibs = new InputBitStream(bais);

                final Set<Integer> givenSet = ibs.readRangedNumberSet(new HuffmanTableLengthDecoder(ibs, lengthTable), min, max);
                ibs.close();

                assertEquals(set, givenSet);
            }
        }
    }

    private static class LengthEncoder implements CollectionLengthEncoder {

        private final OutputBitStream _stream;

        LengthEncoder(OutputBitStream stream) {
            _stream = stream;
        }

        @Override
        public void encodeLength(int length) throws IOException {
            _stream.writeHuffmanSymbol(naturalTable, length);
        }
    }

    private static class LengthDecoder implements CollectionLengthDecoder {

        private final InputBitStream _stream;

        LengthDecoder(InputBitStream stream) {
            _stream = stream;
        }

        @Override
        public int decodeLength() throws IOException {
            return _stream.readHuffmanSymbol(naturalTable);
        }
    }

    private static class ValueEncoder implements ProcedureWithIOException<String> {

        private final OutputBitStream _stream;
        private final ProcedureWithIOException<Character> _writer;
        private final LengthEncoder _lengthEncoder;

        ValueEncoder(OutputBitStream stream) {
            _stream = stream;
            _writer = new ProcedureWithIOException<Character>() {

                private final HuffmanTable<Character> _table = new CharHuffmanTable(8);

                @Override
                public void apply(Character element) throws IOException {
                    _stream.writeHuffmanSymbol(_table, element);
                }
            };
            _lengthEncoder = new LengthEncoder(stream);
        }

        @Override
        public void apply(String element) throws IOException {
            _stream.writeList(_lengthEncoder, stringAsCharList(element), _writer);
        }
    }

    private static class ValueDecoder implements SupplierWithIOException<String> {

        private final InputBitStream _stream;
        private final SupplierWithIOException<Character> _supplier;
        private final LengthDecoder _lengthDecoder;

        ValueDecoder(InputBitStream stream) {
            _stream = stream;
            _supplier = new SupplierWithIOException<Character>() {

                private final HuffmanTable<Character> _table = new CharHuffmanTable(8);

                @Override
                public Character apply() throws IOException {
                    return _stream.readHuffmanSymbol(_table);
                }
            };
            _lengthDecoder = new LengthDecoder(stream);
        }

        @Override
        public String apply() throws IOException {
            return charListAsString(_stream.readList(_lengthDecoder, _supplier));
        }
    }

    private void checkReadAndWriteMaps(boolean useDiff) throws IOException {
        final Integer[] values = new Integer[] {
                -42, -5, -1, 0, null, 1, 2, 25
        };

        final int length = values.length;
        for (int indexA = 0; indexA <= length; indexA++) {
            for (int indexB = indexA; indexB <= length; indexB++) {
                for (int indexC = indexB; indexC <= length; indexC++) {
                    final HashMap<Integer, String> map = new HashMap<>();

                    if (indexA < length) {
                        map.put(values[indexA], Integer.toString(indexA));
                    }

                    if (indexB < length) {
                        map.put(values[indexB], Integer.toString(indexB));
                    }

                    if (indexC < length) {
                        map.put(values[indexC], Integer.toString(indexC));
                    }

                    final ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    final OutputBitStream obs = new OutputBitStream(baos);

                    final NullableIntegerEncoder keyEncoder = new NullableIntegerEncoder(obs);
                    final ValueEncoder valueEncoder = new ValueEncoder(obs);
                    obs.writeMap(new LengthEncoder(obs), map, keyEncoder, keyEncoder, useDiff? keyEncoder : null, valueEncoder);
                    obs.close();

                    final byte[] array = baos.toByteArray();
                    final ByteArrayInputStream bais = new ByteArrayInputStream(array);
                    final InputBitStream ibs = new InputBitStream(bais);

                    final NullableIntegerDecoder keyDecoder = new NullableIntegerDecoder(ibs);
                    final ValueDecoder valueDecoder = new ValueDecoder(ibs);
                    final Map<Integer, String> givenMap = ibs.readMap(new LengthDecoder(ibs), keyDecoder, useDiff? keyDecoder : null, valueDecoder);
                    ibs.close();

                    assertEquals(map, givenMap);
                }
            }
        }
    }

    @Test
    public void evaluateReadAndWriteMapsWithoutDiff() throws IOException {
        checkReadAndWriteMaps(false);
    }

    @Test
    public void evaluateReadAndWriteMapsWithDiff() throws IOException {
        checkReadAndWriteMaps(true);
    }

    @Test
    public void evaluateReadAndWriteEmptyList() throws IOException {
        final ProcedureWithIOException<Object> writer = new ProcedureWithIOException<Object>() {
            @Override
            public void apply(Object element) throws IOException {
                throw new AssertionError("Call not expected");
            }
        };
        List<Object> list = Collections.emptyList();

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final OutputBitStream obs = new OutputBitStream(baos);

        obs.writeList(new LengthEncoder(obs), list, writer);
        obs.close();

        final byte[] array = baos.toByteArray();
        final ByteArrayInputStream bais = new ByteArrayInputStream(array);
        final InputBitStream ibs = new InputBitStream(bais);

        final SupplierWithIOException<Object> supplier = new SupplierWithIOException<Object>() {
            @Override
            public Object apply() throws IOException {
                throw new AssertionError("This should not be called");
            }
        };

        final List<Object> givenList = ibs.readList(new LengthDecoder(ibs), supplier);
        ibs.close();

        assertTrue(givenList.isEmpty());
    }

    @Test
    public void evaluateReadAndWriteList() throws IOException {
        final String[] values = new String[] {
                null, "a", "b", "ab", "A", "1418528", ""
        };

        final RangedIntegerHuffmanTable table = new RangedIntegerHuffmanTable(0, values.length - 1);

        for (String a : values) for (String b : values) {
            ArrayList<String> list = new ArrayList<>(2);
            list.add(a);
            list.add(b);

            final ByteArrayOutputStream baos = new ByteArrayOutputStream();
            final OutputBitStream obs = new OutputBitStream(baos);

            final ProcedureWithIOException<String> writer = new ProcedureWithIOException<String>() {
                @Override
                public void apply(String element) throws IOException {
                    final int valuesLength = values.length;
                    for (int i = 0; i < valuesLength; i++) {
                        if (element == values[i]) {
                            obs.writeHuffmanSymbol(table, i);
                            return;
                        }
                    }

                    throw new AssertionError("Unexpected symbol");
                }
            };

            obs.writeList(new LengthEncoder(obs), list, writer);
            obs.close();

            final byte[] array = baos.toByteArray();
            final ByteArrayInputStream bais = new ByteArrayInputStream(array);
            final InputBitStream ibs = new InputBitStream(bais);

            final SupplierWithIOException<String> supplier = new SupplierWithIOException<String>() {
                @Override
                public String apply() throws IOException {
                    return values[ibs.readHuffmanSymbol(table)];
                }
            };

            final List<String> givenList = ibs.readList(new LengthDecoder(ibs), supplier);
            ibs.close();

            assertEquals(2, givenList.size());
            assertEquals(list.get(0), givenList.get(0));
            assertEquals(list.get(1), givenList.get(1));
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

        DefinedHuffmanTable<Character> table = DefinedHuffmanTable.withFrequencies(map, charComparator);

        final int entryCount = entries.size();
        for (int i = 0; i < entryCount; i++) {
            DefinedHuffmanTable<Character> newTable = DefinedHuffmanTable.withFrequencies(map, charComparator);
            assertEquals("Failed on iteration " + i, table, newTable);
        }
    }
}
