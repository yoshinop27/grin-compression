package edu.grinnell.csc207.compression;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public class Tests {
    /**
     * test constructor frequency map to huffman tree
     */
    @Test
    public void testConstructor1() {
        // Mock Frequency Map
        Map<Short, Integer> testMap = new HashMap<>();
        testMap.put((short) 0, 1);
        testMap.put((short) 1, 2);

         // test constructor1
        HuffmanTree test = new HuffmanTree(testMap);
        assertEquals(4, test.root.freq); // account for EOF
        assertEquals((short) 1, test.root.left.value.get());
    }
    
    /**
     * test constructor input to huffman tree
     * @throws IOException
     * Resource for creating temporary testing files
     * Cite: https://junit.org/junit5/docs/current/user-guide/#writing-tests-built-in-extensions-TempDirectory
     */
    @Test
    void testConstructor2(@TempDir Path tmp) throws IOException {
        // mock data 
        Path file = tmp.resolve("tree.bin");
        BitOutputStream out = new BitOutputStream(file.toString());
        out.writeBit(0);        
        out.writeBits(5, 9);    
        out.close();

        // test constructor 2
        BitInputStream in = new BitInputStream(file.toString()); 
        HuffmanTree tree = new HuffmanTree(in);
        in.close();

        // test root is leaf with value 5
        assertEquals(0, tree.root.freq);
        assertTrue(tree.root.value.isPresent());
        assertEquals((short) 5, tree.root.value.get());
    }

    /**
     * Testing encode functionality
     * @param tmp
     * @throws IOException
     */
    @Test
    void encodeDecode(@TempDir Path tmp) throws IOException {
        // mock data (highly redundant so compression is effective)
        byte[] data = "aaaaaaaaaaaaaaaa".getBytes(StandardCharsets.US_ASCII);

        // setup tree
        Map<Short, Integer> freqs = new HashMap<>();
        for (byte b : data) {
            short key = (short) (b); 
            freqs.put(key, freqs.getOrDefault(key, 0) + 1); // cite: https://www.w3schools.com/java/ref_hashmap_getordefault.asp
        }
        HuffmanTree tree = new HuffmanTree(freqs);

        // write input file
        Path input = tmp.resolve("input.bin");
        Files.write(input, data);
        // file we are going to write encoded data to
        Path compressed = tmp.resolve("compressed.bin");

        // encode
        BitInputStream encodeIn = new BitInputStream(input.toString());
        BitOutputStream encodeOut = new BitOutputStream(compressed.toString());
        tree.encode(encodeIn, encodeOut);
        encodeIn.close();
        encodeOut.close();

        // ensure compressed file is smaller than input
        long inputSize = Files.size(input);
        long compressedSize = Files.size(compressed);
        assertTrue(compressedSize < inputSize);

        // writing original data back to
        Path output = tmp.resolve("output.bin");

        // decode
        BitInputStream decodeIn = new BitInputStream(compressed.toString());
        BitOutputStream decodeOut = new BitOutputStream(output.toString());
        tree.decode(decodeIn, decodeOut);
        decodeIn.close();
        decodeOut.close();

        // preserving data test
        byte[] decoded = Files.readAllBytes(output);
        assertArrayEquals(data, decoded);
    }

    /**
     * Testing method to serialize huffman trees to a output file
     * @param tmp
     * @throws IOException
     */
    @Test
    void serializeHtest (@TempDir Path tmp) throws IOException{
        // Mock Frequency Map and huffman tree
        Map<Short, Integer> testMap = new HashMap<>();
        testMap.put((short) 0, 1);
        testMap.put((short) 1, 2);
        HuffmanTree test = new HuffmanTree(testMap);

        // Mock output file
        Path file = tmp.resolve("tree.bin");
        BitOutputStream out = new BitOutputStream(file.toString());
        // Write to file
        test.serialize(out);
        out.close();

        // Read in
        BitInputStream in = new BitInputStream(file.toString());
        HuffmanTree tree = new HuffmanTree(in);
        in.close();

        // test
        assertEquals(0, tree.root.freq);
        assertEquals(Optional.empty(), tree.root.value);
        assertTrue(tree.root.left.value.isPresent());
    }    
}
