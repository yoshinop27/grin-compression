package edu.grinnell.csc207.compression;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * The driver for the Grin compression program.
 */
public class Grin {

    // magic number
    private static final int MAGIC = 0x736;

    /**
     * Decodes the .grin file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * @param infile the file to decode
     * @param outfile the file to ouptut to
     */
    public static void decode (String infile, String outfile) throws IOException, IllegalArgumentException{
        // create bit stream
        BitInputStream in = new BitInputStream(infile);
        BitOutputStream out = new BitOutputStream(outfile);

        // check magic number
        int magic = in.readBits(32);
        if (MAGIC != magic) {
            throw new IllegalArgumentException();
        }

        // deserialize tree
        HuffmanTree ht = new HuffmanTree(in);

        // decode file using new tree
        ht.decode(in, out);
    }

    /**
     * Creates a mapping from 8-bit sequences to number-of-occurrences of
     * those sequences in the given file. To do this, read the file using a
     * BitInputStream, consuming 8 bits at a time.
     * @param file the file to read
     * @return a freqency map for the given file
     * @throws IOexception
     */
    public static Map<Short, Integer> createFrequencyMap (String file) throws IOException {
        // create map
        Map<Short, Integer> freqs = new HashMap<>();
        // input stream
        BitInputStream in = new BitInputStream(file);
        int bits;
        // read file 8 bits at a time
        while ((bits = in.readBits(8)) != -1) {
            short key = (short) bits;
            Integer count = freqs.get(key);
            if (count == null) {
                freqs.put(key, 1);
            } else {
                freqs.put(key, count + 1);
            }
        }
        in.close();
        return freqs;
    }

    /**
     * Encodes the given file denoted by infile and writes the output to the
     * .grin file denoted by outfile.
     * @param infile the file to encode.
     * @param outfile the file to write the output to.
     */
    public static void encode(String infile, String outfile) throws IOException {
        // create frequency map
        Map<Short, Integer> freqs = createFrequencyMap(infile);
        // create a huffman tree
        HuffmanTree ht = new HuffmanTree(freqs);

        // Streams
        BitInputStream in = new BitInputStream(infile);
        BitOutputStream out = new BitOutputStream(outfile);

        // write magic number
        out.writeBits(MAGIC, 32);
        // write serialized tree
        ht.serialize(out);
        // write content
        ht.encode(in, out);

        // clean
        in.close();
        out.close();
    }

    /**
     * The entry point to the program.
     * @param args the command-line arguments.
     */
    public static void main(String[] args) throws IllegalArgumentException, IOException {
        // prompt user
        System.out.println("Usage: java Grin <encode|decode> <infile> <outfile>");

        // Check args
        if (args.length != 3) throw new IllegalArgumentException();
        // run program
        if (args[0].toLowerCase().equals("encode")) {
            encode(args[1], args[2]);
        } else if (args[0].toLowerCase().equals("decode")){
            decode(args[1], args[2]);
        } else {
            throw new IllegalArgumentException();
        }
    }
}
