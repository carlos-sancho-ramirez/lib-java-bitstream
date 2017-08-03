package sword.bitstream;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.*;

import static junit.framework.TestCase.fail;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

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

        final HuffmanTable<String> huffmanTable = new DefinedHuffmanTable<>( new String[][] {
                new String[0],
                new String[]{null},
                new String[0],
                new String[]{"a", "b", "c"},
                new String[]{"", "abc"}
        });

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

    @Test
    public void evaluateReadAndWriteHuffmanEncodedLoremIpsum() throws IOException {
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

        final DefinedHuffmanTable<Character> huffmanTable = DefinedHuffmanTable.from(loremIpsumList);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final OutputBitStream obs = new OutputBitStream(baos);

        obs.writeHuffmanCharTable(huffmanTable);
        obs.writeNaturalNumber(loremIpsumLength);
        for (int i = 0; i < loremIpsumLength; i++) {
            obs.writeHuffmanSymbol(huffmanTable, loremIpsum.charAt(i));
        }

        obs.close();

        final byte[] array = baos.toByteArray();
        final ByteArrayInputStream bais = new ByteArrayInputStream(array);
        final InputBitStream ibs = new InputBitStream(bais);

        assertEquals(huffmanTable, ibs.readHuffmanCharTable());
        assertEquals(loremIpsumLength, ibs.readNaturalNumber());
        for (int i = 0; i < loremIpsumLength; i++) {
            assertEquals(loremIpsum.charAt(i), ibs.readHuffmanSymbol(huffmanTable).charValue());
        }
        ibs.close();
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
        final int givenBitAlign1 = NaturalNumberHuffmanTable.withFrequencies(map).getBitAlign();
        assertEquals(expectedBitAlign1, givenBitAlign1);

        map.put(3L, 70);
        final int expectedBitAlign2 = 2;
        final int givenBitAlign2 = NaturalNumberHuffmanTable.withFrequencies(map).getBitAlign();
        assertEquals(expectedBitAlign2, givenBitAlign2);
    }

    @Test
    public void evaluateReadAndWriteHuffmanTableOfUniqueSymbol() throws IOException {
        final Map<Integer, Integer> frequencyMap = new HashMap<>();
        frequencyMap.put(1, 5);

        final DefinedHuffmanTable<Integer> huffmanTable = DefinedHuffmanTable.withFrequencies(frequencyMap);

        final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final OutputBitStream obs = new OutputBitStream(baos);

        obs.writeHuffmanTable(huffmanTable, value -> obs.writeNaturalNumber(value));
        obs.close();

        final byte[] array = baos.toByteArray();
        final ByteArrayInputStream bais = new ByteArrayInputStream(array);
        final InputBitStream ibs = new InputBitStream(bais);

        final DefinedHuffmanTable<Integer> givenHuffmanTable = ibs.readHuffmanTable(() -> (int) ibs.readNaturalNumber());
        ibs.close();

        assertEquals(huffmanTable, givenHuffmanTable);
    }
}
