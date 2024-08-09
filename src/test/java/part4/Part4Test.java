package test.java.part4;

import main.java.part2.Part2;
import main.java.part4.Part4;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for the Part2 encryption and decryption.
 * Author: Hamish Burke
 */
public class Part4Test {
    private static final Path INPUT_FILE = Path.of("test/resources/text-files/plaintext.txt");
    private static final Path ENCRYPTED_FILE = Path.of("test/resources/text-files/ciphertext.enc");

    @Test
    public void testMain() throws Exception {
        Part4.main(new String[]{ENCRYPTED_FILE.toString(), "-t", "1"});
    }

    @Test
    public void testMain2() throws Exception {
        Part4.main(new String[]{ENCRYPTED_FILE.toString(), "-t", "2"});
    }

    @Test
    public void testMain3() throws Exception {
        Part4.main(new String[]{ENCRYPTED_FILE.toString(), "-t", "3"});
    }

    @Test
    public void testPrintsCorrectPassword() throws Exception {
        // Redirect System.out to capture output
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        try {
            Path tempFile = Files.createTempFile("temp", ".txt");
            System.setOut(new PrintStream(tempFile.toFile()));
            Part4.main(new String[]{ENCRYPTED_FILE.toString(), "-t", "1"});
            String output = Files.readString(tempFile);
            Assertions.assertTrue(output.contains("apple"));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }

    @Test
    public void testPrintsCorrectPassword2() throws Exception {
        // Redirect System.out to capture output
        PrintStream originalOut = System.out;
        PrintStream originalErr = System.err;
        try {
            Path tempFile = Files.createTempFile("temp", ".txt");
            System.setOut(new PrintStream(tempFile.toFile()));
            Part4.main(new String[]{ENCRYPTED_FILE.toString(), "-t", "2"});
            String output = Files.readString(tempFile);
            Assertions.assertTrue(output.contains("dog"));
        } finally {
            System.setOut(originalOut);
            System.setErr(originalErr);
        }
    }
}
