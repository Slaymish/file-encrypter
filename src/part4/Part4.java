package part4;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.spec.KeySpec;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.logging.Logger;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public class Part4 {
    private static final Logger LOG = Logger.getLogger(Part4.class.getSimpleName());
    private static final String ALGORITHM = "AES";
    private static final String CIPHER = "AES/CBC/PKCS5PADDING";
    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_SIZE = 128;
    private static final int ITERATIONS = 1000; // usually 65536
    private static final int SALT_SIZE = 16;
    private static final int IV_SIZE = 16;

    public static void main(String[] args) throws Exception {
        if (args.length < 3) {
            System.err.println("Usage: java Part4 <ciphertextPath> <type>");
            return;
        }

        String ciphertextPath = args[0];

        if (!args[1].equals("-t") && !args[1].equals("--type")) {
            System.err.println("Usage: java Part4 <ciphertextPath> <type>");
            return;
        }
        int type = Integer.parseInt(args[2]);

        byte[] ciphertext = Files.readAllBytes(Paths.get(ciphertextPath));

        long startTime = System.currentTimeMillis();
        String password = bruteForceAttack(ciphertext, type);
        long endTime = System.currentTimeMillis();
        System.out.println("Time taken: " + (endTime - startTime) + "ms");
        System.out.println(password);
    }

    public static String bruteForceAttack(byte[] ciphertext, int type) throws InterruptedException {
        char[] charset = switch (type) {
            case 0 -> "abcdefghijklmnopqrstuvwxyz".toCharArray();
            case 1 -> "abcdefghijklmnopqrstuvwxyz0123456789".toCharArray();
            case 2 -> "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();
            default -> throw new IllegalArgumentException("Invalid type");
        };

        // Create a thread pool
        ExecutorService executor = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        List<Future<String>> futures = new ArrayList<>();

        // Submit tasks for each possible password length
        for (int len = 1; len <= 6; len++) {
            final int length = len;
            futures.add(executor.submit(() -> tryPasswordsInThread(ciphertext, charset, length)));
        }

        // Wait for any thread to return a result
        for (Future<String> future : futures) {
            try {
                String result = future.get();
                if (result != null) {
                    executor.shutdownNow();  // Stop all other threads
                    return result;
                }
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }

        executor.shutdown();
        return null;
    }

    public static boolean tryPasswords(byte[] ciphertext, char[] attempt, char[] charset, int pos) {
        if (pos == attempt.length) {
            String password = new String(attempt);
            return decryptAndCheck(ciphertext, password);
        } else {
            for (char c : charset) {
                attempt[pos] = c;
                if (tryPasswords(ciphertext, attempt, charset, pos + 1)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean decryptAndCheck(byte[] ciphertext, String password) {
        try {
            // Extract salt (first 16 bytes), IV (next 16 bytes), and actual ciphertext
            byte[] salt = Arrays.copyOfRange(ciphertext, 0, SALT_SIZE);
            byte[] iv = Arrays.copyOfRange(ciphertext, SALT_SIZE, SALT_SIZE + IV_SIZE);
            byte[] actualCiphertext = Arrays.copyOfRange(ciphertext, SALT_SIZE + IV_SIZE, ciphertext.length);

            // Generate key from password using PBKDF2 with the extracted salt
            SecretKeyFactory factory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
            KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATIONS, KEY_SIZE);
            SecretKey tmp = factory.generateSecret(spec);
            SecretKeySpec secretKeySpec = new SecretKeySpec(tmp.getEncoded(), ALGORITHM);

            // Decrypt using the key and IV
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, secretKeySpec, new IvParameterSpec(iv));
            byte[] decrypted = cipher.doFinal(actualCiphertext);

            // Convert to string and check for readability
            String decryptedText = new String(decrypted, "UTF-8");

            // Perform both padding and readability checks
            return isReadableText(decryptedText);
        } catch (BadPaddingException | IllegalBlockSizeException e) {
            // These exceptions indicate decryption errors, continue brute-forcing
            return false;
        } catch (Exception e) {
            // Other exceptions should also return false
            return false;
        }
    }

    private static String tryPasswordsInThread(byte[] ciphertext, char[] charset, int length) {
        char[] attempt = new char[length];
        if (tryPasswords(ciphertext, attempt, charset, 0)) {
            return new String(attempt);
        }
        return null;
    }

    private static boolean isReadableText(String text) {
        int readableChars = 0;
        int maxNonPrintable = (int) (text.length() * 0.2);
        int nonPrintableCount = 0;

        for (char c : text.toCharArray()) {
            if (c >= 32 && c <= 126) { // ASCII printable characters
                readableChars++;
            } else {
                nonPrintableCount++;
                if (nonPrintableCount > maxNonPrintable) {
                    return false;
                }
            }
        }
        return true;
    }
}
