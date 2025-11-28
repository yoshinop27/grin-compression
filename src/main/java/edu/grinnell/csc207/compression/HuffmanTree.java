package edu.grinnell.csc207.compression;
import java.util.PriorityQueue;
import java.util.Map;
import java.util.Optional;
import java.util.HashMap;

/**
 * A HuffmanTree derives a space-efficient coding of a collection of byte
 * values.
 *
 * The huffman tree encodes values in the range 0--255 which would normally
 * take 8 bits.  However, we also need to encode a special EOF character to
 * denote the end of a .grin file.  Thus, we need 9 bits to store each
 * byte value.  This is fine for file writing (modulo the need to write in
 * byte chunks to the file), but Java does not have a 9-bit data type.
 * Instead, we use the next larger primitive integral type, short, to store
 * our byte values.
 */
public class HuffmanTree {
    public Node root;

    /**
     * Constructs a new HuffmanTree from a frequency map.
     * @param freqs a map from 9-bit values to frequencies.
     */
    public HuffmanTree (Map<Short, Integer> freqs) {
        // Create priority queue
        PriorityQueue<Node> pq = new PriorityQueue<>();

        // Add EOF node
        Node eof = new Node(1, (short) 0b100000000);
        pq.add(eof);
        
        // Create a pair for each entry in freqs, add to priority queue
        for (Map.Entry<Short, Integer> e: freqs.entrySet()){
            Node p = new Node(e.getValue(), e.getKey()); 
            pq.add(p);
        }

        // Build Huffman Tree
        while (pq.size() >= 2){
            Node left = pq.poll();
            Node right = pq.poll();
            Node newNode = new Node(left.freq + right.freq, left, right);
            pq.add(newNode);
        }

        // Assign final node to root
        this.root = pq.poll();
    }

    /**
     * Constructs a new HuffmanTree from the given file.
     * @param in the input file (as a BitInputStream)
     */
    public HuffmanTree (BitInputStream in) {
        this.root = HuffmanTreeH(in);
    }

    /**
     * Recursively build huffman tree from serialized version
     * @param in filereader
     * @return a huffman tree
     */
    public Node HuffmanTreeH(BitInputStream in) {
        int tag;
        // Read the file
        tag = in.readBit();
        if (tag == 1){
            // Internal Node
            Node left = HuffmanTreeH(in);
            Node right = HuffmanTreeH(in);
            return new Node(0, left, right);
        } else {
            // Leaf Node
            int ch = in.readBits(9);
            return new Node(0, (short) ch);
        }
    }
    
    /**
     * Writes this HuffmanTree to the given file as a stream of bits in a
     * serialized format.
     * @param out the output file as a BitOutputStream
     */
    public void serialize (BitOutputStream out) {
        serializeH(out, this.root);
    }

    /**
     * Write huffman tree to file 
     * @param out file to write to
     * @param node root node to start on
     */
    public void serializeH (BitOutputStream out, Node node){
        // Check if null
        if (node == null) return;
        // Check if internal node, write to file
        if (node.value.isPresent()){
            out.writeBit(0);
            out.writeBits(node.value.get(), 9);
        } else {
            out.writeBit(1);
            // Recursively call on left and right
            serializeH(out, node.left);
            serializeH(out, node.right);
        }
    }
   
    /**
     * Encodes the file given as a stream of bits into a compressed format
     * using this Huffman tree. The encoded values are written, bit-by-bit
     * to the given BitOuputStream.
     * @param in the file to compress.
     * @param out the file to write the compressed output to.
     */
    public void encode (BitInputStream in, BitOutputStream out) {

        // DFS on tree and add every char to a map
        Map<Short, String> map = new HashMap<>();
        findCodes(map, this.root, "");

        // Read in whole file
        int bits;
        while ((bits = in.readBits(8)) != -1) {
            // pull from map
            String compressed = map.get((short) bits);
            // Loop through compressed code and write
            for (char c : compressed.toCharArray()){
                out.writeBit(c - '0'); // normalize from char to bit
            }
        }
        // Write eof
        String eof = map.get((short) 0b100000000);
        for (char c : eof.toCharArray()){
            out.writeBit(c - '0');
        }
    }

    /**
     * Generate a map of characters and bits
     * @param map create code map
     * @param node current node to evaluate
     * @param path bit path
     */
    public void findCodes (Map<Short, String> map, Node node, String path) {
        // Null check
        if (node == null) return;
        // Keep traversing and building path
        if (node.value.isEmpty()){
            findCodes(map, node.left, path + "0");
            findCodes(map, node.right, path + "1");
        } else {
            // Add to map
            map.put(node.value.get(), path);
        }
    }

    /**
     * Decodes a stream of huffman codes from a file given as a stream of
     * bits into their uncompressed form, saving the results to the given
     * output stream. Note that the EOF character is not written to out
     * because it is not a valid 8-bit chunk (it is 9 bits).
     * @param in the file to decompress.
     * @param out the file to write the decompressed output to.
     */
    public void decode (BitInputStream in, BitOutputStream out) {

        // Huffman tree for decoding
        Node cur = this.root;

        // Read the file
        int bit;
        while((bit = in.readBit()) != -1){
            // Traverse huffman tree
            if (bit == 0){
                cur = cur.left;
            } else {
                cur = cur.right;
            }
            // Check for value
            if (cur.value.isPresent()){
                short val = cur.value.get();
                if (val == (short) 0b100000000) {
                    break; // stop on EOF
                }
                out.writeBits((int) val, 8);
                cur = this.root;
            }
        }
    }

    /**
     * Node class to act as internal nodes for huffman tree
     */
    class Node implements Comparable<Node>{
        // Fields
        int freq;
        Optional<Short> value;
        Node left;
        Node right;

        /**
         * Constructor for merging two leaves together
         * @param freq of character
         * @param value 9 bit binary value
         */
        public Node (int freq, Node left, Node right){
            this.freq = freq;
            this.value = Optional.empty();
            this.left = left;
            this.right = right;
        }

        /**
         * two parameter constructor to create a leaf
         * @param freq of character
         * @param value 9 bit binary value
         */
        public Node (int freq, short value){
            this.freq = freq;
            this.value = Optional.of(value);
            this.left = null;
            this.right = null;
        }

        @Override
        /**
         * Implementing compare to function to enable comparing between nodes
         * @param other node object to compare to
         * @return 1 if object called on is greater, -1 if less, 0 if equal
         */
        public int compareTo(Node other) {
            // Compare by frequencies
            if (this.freq > other.freq) return 1;
            else if (this.freq < other.freq) return -1;
            else return 0; 
        }
    }
}
