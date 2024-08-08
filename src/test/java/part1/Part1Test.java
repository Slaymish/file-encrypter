package test.java.part1;

import main.java.part1.Part1;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for the Part1 encryption and decryption.
 * Author: Hamish Burke
 */
public class Part1Test {

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
   public void encryptionTest() throws Exception {
      System.out.println("Running encryptionTest...");
      // Encrypt the file
      Part1.main(new String[]{"enc", "-i", INPUT_FILE.toString(), "-o", ENCRYPTED_FILE.toString()});

      // Verify the encrypted file exists
      Assertions.assertTrue(Files.exists(ENCRYPTED_FILE), "Encrypted file should exist.");
   }

   @Test
   public void decryptionTest() throws Exception {
      System.out.println("Running decryptionTest...");
      // Ensure the file is encrypted first
      Part1.main(new String[]{"enc", "-i", INPUT_FILE.toString(), "-o", ENCRYPTED_FILE.toString()});

      // Decrypt the file using the same key and IV
      Part1.main(new String[]{"dec", "-k", KEY_FILE.toString(), "-iv", IV_FILE.toString(), "-i", ENCRYPTED_FILE.toString(), "-o", DECRYPTED_FILE.toString()});

      // Verify the decrypted file exists
      Assertions.assertTrue(Files.exists(DECRYPTED_FILE), "Decrypted file should exist.");

      // Verify the decrypted content matches the original content
      String originalContent = Files.readString(INPUT_FILE);
      String decryptedContent = Files.readString(DECRYPTED_FILE);
      Assertions.assertEquals(originalContent, decryptedContent, "Decrypted content should match the original content.");
   }
}
