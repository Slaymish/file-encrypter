package test.java.part4;

import main.java.part4.Part4;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Tests for the Part4 brute-force attack.
 * Author: Hamish Burke
 */
public class Part4Test {
    private static final Path PLAINTEXT_FILE = Path.of("test/resources/text-files/plaintext.txt");
    private static final Path ENCRYPTED_FILE = Path.of("test/resources/text-files/ciphertext.enc");

    @BeforeEach
    public void setup() throws Exception {
        // Ensure any existing encrypted file is deleted
        Files.deleteIfExists(ENCRYPTED_FILE);
    }

    @Test
    public void testPrintsCorrectPasswordSimple() throws Exception {
        // Use a very simple password for this test
        String password = "a";
        Part4.encryptFile(PLAINTEXT_FILE, ENCRYPTED_FILE, password);

        // Redirect System.out to capture output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(outputStream));
            Part4.main(new String[]{ENCRYPTED_FILE.toString(), "-t", "0"});
            String output = outputStream.toString().trim();
            System.out.println("Expected: " + password + ", Actual: " + output); // Debugging output
            Assertions.assertEquals(password, output);
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testPrintsCorrectPasswordType0() throws Exception {
        // Encrypt the file with a known password that matches the expected output
        String password = "apple";
        Part4.encryptFile(PLAINTEXT_FILE, ENCRYPTED_FILE, password);

        // Redirect System.out to capture output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(outputStream));
            Part4.main(new String[]{ENCRYPTED_FILE.toString(), "-t", "0"});
            String output = outputStream.toString().trim();
            System.out.println("Expected: " + password + ", Actual: " + output); // Debugging output
            Assertions.assertEquals(password, output);
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testPrintsCorrectPasswordType1() throws Exception {
        // Encrypt the file with a known password that matches the expected output
        String password = "pass12";
        Part4.encryptFile(PLAINTEXT_FILE, ENCRYPTED_FILE, password);

        // Redirect System.out to capture output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(outputStream));
            Part4.main(new String[]{ENCRYPTED_FILE.toString(), "-t", "1"});
            String output = outputStream.toString().trim();
            System.out.println("Expected: " + password + ", Actual: " + output); // Debugging output
            Assertions.assertEquals(password, output);
        } finally {
            System.setOut(originalOut);
        }
    }

    @Test
    public void testPrintsCorrectPasswordType2() throws Exception {
        // Encrypt the file with a known password that matches the expected output
        String password = "Passwd";
        Part4.encryptFile(PLAINTEXT_FILE, ENCRYPTED_FILE, password);

        // Redirect System.out to capture output
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        PrintStream originalOut = System.out;
        try {
            System.setOut(new PrintStream(outputStream));
            Part4.main(new String[]{ENCRYPTED_FILE.toString(), "-t", "2"});
            String output = outputStream.toString().trim();
            System.out.println("Expected: " + password + ", Actual: " + output); // Debugging output
            Assertions.assertEquals(password, output);
        } finally {
            System.setOut(originalOut);
        }
    }
}
