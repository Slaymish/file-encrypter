package test.java.part2;

import main.java.part2.Part2;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Logger;

/**
 * Tests for the Part2 encryption and decryption.
 * Author: Hamish Burke
 */
public class Part2Test {

    private static final String PASSWORD = "my_password";
    private static final Path INPUT_FILE = Path.of("test/resources/text-files/plaintext.txt");
    private static final Path ENCRYPTED_FILE = Path.of("test/resources/text-files/ciphertext.enc");
    private static final Path DECRYPTED_FILE = Path.of("test/resources/text-files/decrypted.txt");
    private static final Path INVALID_FILE = Path.of("test/resources/text-files/invalid.txt");

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

    @Test
    public void encryptionWithInvalidPathTest() {
        System.out.println("Running encryptionWithInvalidPathTest...");
        // Attempt to encrypt with an invalid input file path
        Exception exception = Assertions.assertThrows(IOException.class, () -> {
            Part2.main(new String[]{"enc", "-p", PASSWORD, "-i", INVALID_FILE.toString(), "-o", ENCRYPTED_FILE.toString()});
        });

        // Verify that the appropriate exception is thrown
        Assertions.assertTrue(exception.getMessage().contains("No such file or directory"));
    }

    @Test
    public void decryptionWithIncorrectPasswordTest() throws Exception {
        System.out.println("Running decryptionWithIncorrectPasswordTest...");
        // Encrypt the file
        Part2.main(new String[]{"enc", "-p", PASSWORD, "-i", INPUT_FILE.toString(), "-o", ENCRYPTED_FILE.toString()});

        // Attempt to decrypt with an incorrect password
        Exception exception = Assertions.assertThrows(IOException.class, () -> {
            Part2.main(new String[]{"dec", "-p", "wrong_password", "-i", ENCRYPTED_FILE.toString(), "-o", DECRYPTED_FILE.toString()});
        });

        // Verify that the appropriate exception is thrown or the content doesn't match
        String originalContent = Files.readString(INPUT_FILE);
        String decryptedContent = Files.readString(DECRYPTED_FILE);
        Assertions.assertNotEquals(originalContent, decryptedContent, "Decrypted content should not match the original content when using an incorrect password.");
    }

    @Test
    public void encryptionAndDecryptionWithLargeFileTest() throws Exception {
        System.out.println("Running encryptionAndDecryptionWithLargeFileTest...");
        Path largeFile = Path.of("test/resources/text-files/largefile.txt");

        // Generate a large file for testing
        Files.writeString(largeFile, "A".repeat(10_000_000)); // 10 MB file

        // Encrypt the large file
        Path encryptedLargeFile = Path.of("test/resources/text-files/largefile.enc");
        Part2.main(new String[]{"enc", "-p", PASSWORD, "-i", largeFile.toString(), "-o", encryptedLargeFile.toString()});
        Assertions.assertTrue(Files.exists(encryptedLargeFile), "Encrypted large file should exist.");

        // Decrypt the large file
        Path decryptedLargeFile = Path.of("test/resources/text-files/decrypted_largefile.txt");
        Part2.main(new String[]{"dec", "-p", PASSWORD, "-i", encryptedLargeFile.toString(), "-o", decryptedLargeFile.toString()});
        Assertions.assertTrue(Files.exists(decryptedLargeFile), "Decrypted large file should exist.");

        // Verify the decrypted content matches the original content
        String originalContent = Files.readString(largeFile);
        String decryptedContent = Files.readString(decryptedLargeFile);
        Assertions.assertEquals(originalContent, decryptedContent, "Decrypted large file content should match the original content.");
    }
}
