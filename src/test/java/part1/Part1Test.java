package test.java.part1;

import main.java.part1.Part1;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.logging.Logger;

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

   private static final Logger LOG = Logger.getLogger(Part1Test.class.getName());

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
      // Encrypt the file first
      Part1.main(new String[]{"enc", "-i", INPUT_FILE.toString(), "-o", ENCRYPTED_FILE.toString()});

      // Decrypt the file
      Part1.main(new String[]{"dec", "-i", ENCRYPTED_FILE.toString(), "-o", DECRYPTED_FILE.toString(), "-k", KEY_FILE.toString(), "-iv", IV_FILE.toString()});

      // Verify the decrypted file exists
      Assertions.assertTrue(Files.exists(DECRYPTED_FILE), "Decrypted file should exist.");

      // Verify the content is the same as the original plaintext file
      byte[] originalContent = Files.readAllBytes(INPUT_FILE);
      byte[] decryptedContent = Files.readAllBytes(DECRYPTED_FILE);
      Assertions.assertArrayEquals(originalContent, decryptedContent, "Decrypted content should match the original content.");
   }

   @Test
   public void decryptionWithGeneratedKeyAndIvTest() throws Exception {
      System.out.println("Running decryptionWithGeneratedKeyAndIvTest...");
      // Encrypt the file without specifying key and IV, they should be generated
      Part1.main(new String[]{"enc", "-i", INPUT_FILE.toString(), "-o", ENCRYPTED_FILE.toString()});

      // Decrypt the file using the generated key and IV
      Part1.main(new String[]{"dec", "-i", ENCRYPTED_FILE.toString(), "-o", DECRYPTED_FILE.toString(), "-k", KEY_FILE.toString(), "-iv", IV_FILE.toString()});

      // Verify the decrypted file exists
      Assertions.assertTrue(Files.exists(DECRYPTED_FILE), "Decrypted file should exist.");

      // Verify the content is the same as the original plaintext file
      byte[] originalContent = Files.readAllBytes(INPUT_FILE);
      byte[] decryptedContent = Files.readAllBytes(DECRYPTED_FILE);
      Assertions.assertArrayEquals(originalContent, decryptedContent, "Decrypted content should match the original content.");
   }

   @Test
   public void invalidKeyFileTest() {
      System.out.println("Running invalidKeyFileTest...");
      Path invalidKeyFile = Path.of("invalid.key.base64");

      Assertions.assertThrows(IOException.class, () -> {
         Part1.main(new String[]{"dec", "-i", ENCRYPTED_FILE.toString(), "-o", DECRYPTED_FILE.toString(), "-k", invalidKeyFile.toString(), "-iv", IV_FILE.toString()});
      }, "Decryption with an invalid key file should throw an IOException.");
   }

   @Test
   public void invalidIvFileTest() {
      System.out.println("Running invalidIvFileTest...");
      Path invalidIvFile = Path.of("invalid.iv.base64");

      Assertions.assertThrows(IOException.class, () -> {
         Part1.main(new String[]{"dec", "-i", ENCRYPTED_FILE.toString(), "-o", DECRYPTED_FILE.toString(), "-k", KEY_FILE.toString(), "-iv", invalidIvFile.toString()});
      }, "Decryption with an invalid IV file should throw an IOException.");
   }

   @Test
   public void missingKeyFileForDecryptionTest() {
      System.out.println("Running missingKeyFileForDecryptionTest...");

      Assertions.assertThrows(IllegalArgumentException.class, () -> {
         Part1.main(new String[]{"dec", "-i", ENCRYPTED_FILE.toString(), "-o", DECRYPTED_FILE.toString(), "-iv", IV_FILE.toString()});
      }, "Decryption without a key file should throw an IllegalArgumentException.");
   }

   @Test
   public void missingIvFileForDecryptionTest() {
      System.out.println("Running missingIvFileForDecryptionTest...");

      Assertions.assertThrows(IllegalArgumentException.class, () -> {
         Part1.main(new String[]{"dec", "-i", ENCRYPTED_FILE.toString(), "-o", DECRYPTED_FILE.toString(), "-k", KEY_FILE.toString()});
      }, "Decryption without an IV file should throw an IllegalArgumentException.");
   }

   @Test
   public void testWithDifferentModes() throws Exception {
      System.out.println("Running testWithDifferentModes...");
      String[] modes = {"CBC", "ECB", "CTR", "OFB", "CFB", "GCM"};

      for (String mode : modes) {
         setup(); // Reset files for each mode
         Part1.main(new String[]{"enc", "-i", INPUT_FILE.toString(), "-o", ENCRYPTED_FILE.toString(), "-m", mode});
         Assertions.assertTrue(Files.exists(ENCRYPTED_FILE), "Encrypted file should exist for mode: " + mode);

         Part1.main(new String[]{"dec", "-i", ENCRYPTED_FILE.toString(), "-o", DECRYPTED_FILE.toString(), "-k", KEY_FILE.toString(), "-iv", IV_FILE.toString(), "-m", mode});
         Assertions.assertTrue(Files.exists(DECRYPTED_FILE), "Decrypted file should exist for mode: " + mode);

         byte[] originalContent = Files.readAllBytes(INPUT_FILE);
         byte[] decryptedContent = Files.readAllBytes(DECRYPTED_FILE);
         Assertions.assertArrayEquals(originalContent, decryptedContent, "Decrypted content should match original for mode: " + mode);
      }
   }
}
