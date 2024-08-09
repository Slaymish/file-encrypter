package test.java.part1;

import main.java.part1.Part1;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.security.SecureRandom;

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

   @Test
   public void emptyFileTest() throws Exception {
      System.out.println("Running emptyFileTest...");
      Path emptyFile = Path.of("test/resources/text-files/empty.txt");
      Files.createFile(emptyFile);

      // Encrypt the empty file
      Part1.main(new String[]{"enc", "-i", emptyFile.toString(), "-o", ENCRYPTED_FILE.toString()});

      // Decrypt the encrypted file
      Part1.main(new String[]{"dec", "-k", KEY_FILE.toString(), "-iv", IV_FILE.toString(), "-i", ENCRYPTED_FILE.toString(), "-o", DECRYPTED_FILE.toString()});

      // Verify the decrypted file exists and is empty
      Assertions.assertTrue(Files.exists(DECRYPTED_FILE), "Decrypted file should exist.");
      Assertions.assertEquals(0, Files.size(DECRYPTED_FILE), "Decrypted file should be empty.");

      Files.delete(emptyFile); // Cleanup
   }

   @Test
   public void largeFileTest() throws Exception {
      System.out.println("Running largeFileTest...");
      Path largeFile = Path.of("test/resources/text-files/largefile.txt");
      byte[] largeContent = new byte[10 * 1024 * 1024]; // 10MB file
      Files.write(largeFile, largeContent);

      // Encrypt the large file
      Part1.main(new String[]{"enc", "-i", largeFile.toString(), "-o", ENCRYPTED_FILE.toString()});

      // Decrypt the encrypted file
      Part1.main(new String[]{"dec", "-k", KEY_FILE.toString(), "-iv", IV_FILE.toString(), "-i", ENCRYPTED_FILE.toString(), "-o", DECRYPTED_FILE.toString()});

      // Verify the decrypted file exists and matches the original content
      Assertions.assertTrue(Files.exists(DECRYPTED_FILE), "Decrypted file should exist.");
      byte[] decryptedContent = Files.readAllBytes(DECRYPTED_FILE);
      Assertions.assertArrayEquals(largeContent, decryptedContent, "Decrypted content should match the original content.");

      Files.delete(largeFile); // Cleanup
   }

   @Test
   public void incorrectKeyTest() throws Exception {
      System.out.println("Running incorrectKeyTest...");
      // Encrypt the file with a correct key
      Part1.main(new String[]{"enc", "-i", INPUT_FILE.toString(), "-o", ENCRYPTED_FILE.toString()});

      // Generate an incorrect key
      byte[] incorrectKey = new byte[16];
      SecureRandom sr = new SecureRandom();
      sr.nextBytes(incorrectKey);
      Path incorrectKeyFile = Path.of("incorrect_key.base64");
      Files.write(incorrectKeyFile, incorrectKey);

      // Attempt to decrypt the file with the incorrect key
      IOException exception = Assertions.assertThrows(IOException.class, () -> {
         Part1.main(new String[]{"dec", "-k", incorrectKeyFile.toString(), "-iv", IV_FILE.toString(), "-i", ENCRYPTED_FILE.toString(), "-o", DECRYPTED_FILE.toString()});
      });

      // Check that the IOException was due to a bad padding (which indicates a wrong key)
      Assertions.assertTrue(exception.getMessage().contains("BadPaddingException"), "Decryption should fail with an incorrect key.");

      Files.delete(incorrectKeyFile); // Cleanup
   }

   @Test
   public void incorrectIVTest() throws Exception {
      System.out.println("Running incorrectIVTest...");
      // Encrypt the file with the correct IV
      Part1.main(new String[]{"enc", "-i", INPUT_FILE.toString(), "-o", ENCRYPTED_FILE.toString()});

      // Generate an incorrect IV
      byte[] incorrectIV = new byte[16];
      SecureRandom sr = new SecureRandom();
      sr.nextBytes(incorrectIV);
      Path incorrectIVFile = Path.of("incorrect_iv.base64");
      Files.write(incorrectIVFile, incorrectIV);

      // Attempt to decrypt the file with the incorrect IV
      IOException exception = Assertions.assertThrows(IOException.class, () -> {
         Part1.main(new String[]{"dec", "-k", KEY_FILE.toString(), "-iv", incorrectIVFile.toString(), "-i", ENCRYPTED_FILE.toString(), "-o", DECRYPTED_FILE.toString()});
      });

      // Alternatively, compare decrypted content to the original content
      String decryptedContent = Files.readString(DECRYPTED_FILE);
      String originalContent = Files.readString(INPUT_FILE);
      Assertions.assertNotEquals(originalContent, decryptedContent, "Decryption with an incorrect IV should produce incorrect content.");

      Files.delete(incorrectIVFile); // Cleanup
   }

   @Test
   public void missingFileTest() {
      // Try to encrypt a file that does not exist and expect a NoSuchFileException
      NoSuchFileException exception = Assertions.assertThrows(NoSuchFileException.class, () -> {
         Part1.main(new String[]{"enc", "-i", "test/resources/text-files/missingfile.txt", "-o", "test/resources/text-files/ciphertext.enc"});
      });

      // Additional assertion to ensure the exception message is correct (optional)
      Assertions.assertTrue(exception.getMessage().contains("missingfile.txt"));
   }

}
