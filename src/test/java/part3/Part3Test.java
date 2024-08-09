package test.java.part3;

import main.java.part3.Part3;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Tests for the Part2 encryption and decryption.
 * Author: Hamish Burke
 */
public class Part3Test {

    private static final Path INPUT_FILE = Path.of("test/resources/text-files/plaintext.txt");
    private static final Path ENCRYPTED_FILE = Path.of("test/resources/text-files/ciphertext.enc");
    private static final Path DECRYPTED_FILE = Path.of("test/resources/text-files/decrypted.txt");
    private static final Path KEY_FILE = Path.of("key.base64");
    private static final Path IV_FILE = Path.of("iv.base64");

    @BeforeEach
    public void setup() throws Exception {
        // Ensure any existing encrypted/decrypted files are deleted
        Files.deleteIfExists(ENCRYPTED_FILE);
        Files.deleteIfExists(DECRYPTED_FILE);
        Files.deleteIfExists(KEY_FILE);
        Files.deleteIfExists(IV_FILE);
    }

    @Test
    public void encryptionCBCTest() throws Exception {
        System.out.println("Running encryptionTest...");
        // Encrypt the file
        Part3.main(new String[]{"enc", "-i", INPUT_FILE.toString(), "-o", ENCRYPTED_FILE.toString(), "-c", "CBC"});

        // Verify the encrypted file exists
        Assertions.assertTrue(Files.exists(ENCRYPTED_FILE), "Encrypted file should exist.");
    }

    @Test
    public void decryptionCBCTest() throws Exception {
        System.out.println("Running decryptionTest...");
        // Ensure the file is encrypted first
        Part3.main(new String[]{"enc", "-i", INPUT_FILE.toString(), "-o", ENCRYPTED_FILE.toString(), "-c", "CBC"});

        // Decrypt the file using the same key and IV
        Part3.main(new String[]{"dec", "-k", KEY_FILE.toString(), "-iv", IV_FILE.toString(), "-i", ENCRYPTED_FILE.toString(), "-o", DECRYPTED_FILE.toString(), "-c","CBC"});

        // Verify the decrypted file exists
        Assertions.assertTrue(Files.exists(DECRYPTED_FILE), "Decrypted file should exist.");

        // Verify the decrypted content matches the original content
        String originalContent = Files.readString(INPUT_FILE);
        String decryptedContent = Files.readString(DECRYPTED_FILE);
        Assertions.assertEquals(originalContent, decryptedContent, "Decrypted content should match the original content.");
    }

    @Test
    public void encryptionECBTest() throws Exception {
        System.out.println("Running encryptionTest...");
        // Encrypt the file
        Part3.main(new String[]{"enc", "-i", INPUT_FILE.toString(), "-o", ENCRYPTED_FILE.toString(), "-c", "ECB"});

        // Verify the encrypted file exists
        Assertions.assertTrue(Files.exists(ENCRYPTED_FILE), "Encrypted file should exist.");
    }

    @Test
    public void decryptionECBTest() throws Exception {
        System.out.println("Running decryptionTest...");
        // Ensure the file is encrypted first
        Part3.main(new String[]{"enc", "-i", INPUT_FILE.toString(), "-o", ENCRYPTED_FILE.toString(), "-c", "ECB"});

        // Decrypt the file using the same key and IV
        Part3.main(new String[]{"dec", "-k", KEY_FILE.toString(), "-iv", IV_FILE.toString(), "-i", ENCRYPTED_FILE.toString(), "-o", DECRYPTED_FILE.toString(), "-c", "ECB"});

        // Verify the decrypted file exists
        Assertions.assertTrue(Files.exists(DECRYPTED_FILE), "Decrypted file should exist.");

        // Verify the decrypted content matches the original content
        String originalContent = Files.readString(INPUT_FILE);
        String decryptedContent = Files.readString(DECRYPTED_FILE);
        Assertions.assertEquals(originalContent, decryptedContent, "Decrypted content should match the original content.");
    }

    @Test
    public void testPerformance() throws Exception {
        System.out.println("Testing Performance...");

        List<String> keySizes = List.of("16","24","32");
        List<String> modes = List.of("CBC","ECB");
        List<Integer> fileSize = List.of(10 * 1024, 100 * 1024, 1000 * 1024); // file sizes in KB (adjust as needed)
        int repeatAmount = 10;
        Map<String,Long> encTimes = new HashMap<>(); // key = mode + keySize + fileSize, value = time taken
        Map<String,Long> decTimes = new HashMap<>();


        // Measure performance
        for (String mode : modes) {
            for (Integer size : fileSize) {
                for (String keySize : keySizes) {
                    long encTime = 0;
                    long decTime = 0;
                    for(int i = 0 ; i< repeatAmount ; i++) {
                        // Create a file of specified size
                        createFile(INPUT_FILE, size);

                        long encryptionTime = Part3.main(new String[]{"enc", "-i", INPUT_FILE.toString(), "-o", ENCRYPTED_FILE.toString(), "-c", mode, "-ks", keySize});
                        long decryptionTime = Part3.main(new String[]{"dec", "-k", KEY_FILE.toString(), "-iv", IV_FILE.toString(), "-i", ENCRYPTED_FILE.toString(), "-o", DECRYPTED_FILE.toString(), "-c", mode, "-ks", keySize});

                        // Clean up before the next test
                        Files.deleteIfExists(ENCRYPTED_FILE);
                        Files.deleteIfExists(DECRYPTED_FILE);

                        encTime += encryptionTime;
                        decTime += decryptionTime;
                    }

                    encTimes.put(mode + "-" + keySize + "-" + size, encTime/repeatAmount);
                    decTimes.put(mode + "-" + keySize + "-" + size, decTime/repeatAmount);
                }
            }
        }

        // Print the results
        System.out.println("Encryption times:");
        for (Map.Entry<String, Long> entry : encTimes.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue() + "ns");
        }

        System.out.println("Decryption times:");
        for (Map.Entry<String, Long> entry : decTimes.entrySet()) {
            System.out.println(entry.getKey() + " : " + entry.getValue() + "ns");
        }

        System.out.println("Mean of " + repeatAmount + " runs");
        System.out.println("Performance comparison completed.");
    }

    private void createFile(Path path, Integer size) throws IOException {
        byte[] data = new byte[(int) size];  // Create a byte array with the desired size
        Files.write(path, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        // truncate existing means if the file already exists, it will be truncated to 0 bytes
    }
}
