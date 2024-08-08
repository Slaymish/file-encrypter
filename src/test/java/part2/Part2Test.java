package test.java.part2;

import main.java.part2.Part2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for the Part2 encryption and decryption.
 * Author: Hamish Burke
 */
public class Part2Test {

    private static final String PASSWORD = "my_password";
    private static final Path INPUT_FILE = Path.of("test/resources/text-files/plaintext.txt");
    private static final Path ENCRYPTED_FILE = Path.of("test/resources/text-files/ciphertext.enc");
    private static final Path DECRYPTED_FILE = Path.of("test/resources/text-files/decrypted.txt");

    @BeforeEach
    public void setup() throws Exception {
        // Ensure any existing encrypted/decrypted files are deleted
        Files.deleteIfExists(ENCRYPTED_FILE);
        Files.deleteIfExists(DECRYPTED_FILE);
    }

    @Test
    public void encryptionTest() throws Exception {
        System.out.println("Running encryptionTest...");
        // Encrypt the file
        Part2.main(new String[]{"enc", "-p", PASSWORD, "-i", INPUT_FILE.toString(), "-o", ENCRYPTED_FILE.toString()});

        // Verify the encrypted file exists
        Assertions.assertTrue(Files.exists(ENCRYPTED_FILE), "Encrypted file should exist.");
    }

    @Test
    public void decryptionTest() throws Exception {
        System.out.println("Running decryptionTest...");
        // Ensure the file is encrypted first
        Part2.main(new String[]{"enc", "-p", PASSWORD, "-i", INPUT_FILE.toString(), "-o", ENCRYPTED_FILE.toString()});

        // Decrypt the file
        Part2.main(new String[]{"dec", "-p", PASSWORD, "-i", ENCRYPTED_FILE.toString(), "-o", DECRYPTED_FILE.toString()});

        // Verify the decrypted file exists
        Assertions.assertTrue(Files.exists(DECRYPTED_FILE), "Decrypted file should exist.");

        // Verify the decrypted content matches the original content
        String originalContent = Files.readString(INPUT_FILE);
        String decryptedContent = Files.readString(DECRYPTED_FILE);
        Assertions.assertEquals(originalContent, decryptedContent, "Decrypted content should match the original content.");
    }
}
