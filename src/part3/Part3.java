package part3;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static part3.Util.bytesToHex;

/**
 * Part3 class for measuring encryption and decryption performance using AES.
 * Author: Hamish Burke
 */
public class Part3 {
    private static final Logger LOG = Logger.getLogger(Part3.class.getSimpleName());
    private static final String ALGORITHM = "AES";

    private static final Path INPUT_FILE = Path.of("input.txt");
    private static final Path ENCRYPTED_FILE = Path.of("encrypted.enc");
    private static final Path DECRYPTED_FILE = Path.of("decrypted.txt");
    private static final Path KEY_FILE = Path.of("key.base64");
    private static final Path IV_FILE = Path.of("iv.base64");

    private static final Map<String, String> ciphers = Map.of(
            "CBC", "AES/CBC/PKCS5PADDING",
            "ECB", "AES/ECB/PKCS5PADDING",
            "CTR", "AES/CTR/NoPadding",
            "OFB", "AES/OFB/NoPadding",
            "CFB", "AES/CFB/NoPadding",
            "GCM", "AES/GCM/NoPadding");

    private static void createFile(Path path, int size) throws IOException {
        byte[] data = new byte[size];  // Create a byte array with the desired size
        Files.write(path, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
    }

    public static void main(String[] args) {
        System.out.println("Testing Performance...");

        String outputFileName = "results.csv";
        int repeatAmount = 10;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-o" -> outputFileName = args[++i];
                case "-repeats" -> repeatAmount = Integer.parseInt(args[++i]);
            }
        }

        List<String> keySizes = List.of("16", "24", "32"); // 128, 192, 256 bits
        List<String> modes = List.of("CBC", "ECB", "CTR", "OFB", "CFB", "GCM");
        List<Integer> fileSize = List.of(10 * 1024, 100 * 1024, 1000 * 1024); // file sizes in KB
        Map<String, Long> encTimes = new HashMap<>();
        Map<String, Long> decTimes = new HashMap<>();

        // Measure performance
        for (String mode : modes) {
            for (Integer size : fileSize) {
                for (String keySize : keySizes) {
                    long encTime = 0;
                    long decTime = 0;
                    for (int i = 0; i < repeatAmount; i++) {
                        try {
                            createFile(INPUT_FILE, size);

                            long encryptionTime = checkPerformance(new String[]{"enc", "-i", INPUT_FILE.toString(), "-o", ENCRYPTED_FILE.toString(), "-c", mode, "-ks", keySize});
                            long decryptionTime = checkPerformance(new String[]{"dec", "-k", KEY_FILE.toString(), "-iv", IV_FILE.toString(), "-i", ENCRYPTED_FILE.toString(), "-o", DECRYPTED_FILE.toString(), "-c", mode, "-ks", keySize});

                            encTime += encryptionTime;
                            decTime += decryptionTime;

                            // Clean up before the next test
                            Files.deleteIfExists(ENCRYPTED_FILE);
                            Files.deleteIfExists(DECRYPTED_FILE);
                        } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException |
                                 InvalidAlgorithmParameterException | InvalidKeyException e) {
                            LOG.log(Level.SEVERE, "Error during performance test", e);
                        }
                    }

                    encTimes.put(mode + "-" + keySize + "-" + size, encTime / repeatAmount);
                    decTimes.put(mode + "-" + keySize + "-" + size, decTime / repeatAmount);
                }
            }
        }

        // Write the results to a CSV file
        try (java.io.PrintStream out = new java.io.PrintStream(outputFileName)) {
            out.println("Mode,Key Size (bits),File Size (KB),Mean Encryption Time (ns),Mean Decryption Time (ns)");
            for (String key : encTimes.keySet()) {
                String[] parts = key.split("-");
                String mode = parts[0];
                String keySize = parts[1];
                String size = parts[2];
                out.println(mode + "," + keySize + "," + size + "," + encTimes.get(key) + "," + decTimes.get(key));
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Error writing results to CSV file", e);
        }

        System.out.println("Performance comparison completed and saved to " + outputFileName);

        // Clean up
        try {
            Files.deleteIfExists(INPUT_FILE);
            Files.deleteIfExists(KEY_FILE);
            Files.deleteIfExists(IV_FILE);
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Error during cleanup", e);
        }
    }

    private static long checkPerformance(String[] args) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {
        String operation = args[0]; // enc/dec
        String inputFile = null;
        String outputFile = null;
        String keyFile = null;
        String ivFile = null;
        String cipherToUse = null;
        int keySize = 16; // default

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "-i" -> inputFile = args[++i];
                case "-o" -> outputFile = args[++i];
                case "-k" -> keyFile = args[++i];
                case "-iv" -> ivFile = args[++i];
                case "-c" -> cipherToUse = args[++i];
                case "-ks" -> keySize = Integer.parseInt(args[++i]);
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    System.exit(1);
                }
            }
        }

        LOG.info("Using cipher mode of operation: " + cipherToUse);
        LOG.info("Using keysize: " + keySize);

        if (inputFile == null || outputFile == null) {
            System.err.println("Input and output files are required.");
            System.exit(1);
        }

        SecureRandom sr = new SecureRandom();
        byte[] key = new byte[keySize];
        byte[] initVector = new byte[16];
        byte[] gcmIv = new byte[12]; // 12 bytes IV for GCM
        IvParameterSpec iv = null;
        GCMParameterSpec gcmSpec = null;
        SecretKeySpec skeySpec;

        Cipher cipher = Cipher.getInstance(ciphers.get(cipherToUse));

        if (operation.equals("enc")) {
            sr.nextBytes(key); // Generate key
            Files.write(KEY_FILE, key);
            LOG.info("Key saved to key.base64: " + bytesToHex(key));

            if (cipherToUse.equals("GCM")) {
                sr.nextBytes(gcmIv);
                Files.write(IV_FILE, gcmIv);
                LOG.info("GCM IV saved to iv.base64: " + bytesToHex(gcmIv));
                gcmSpec = new GCMParameterSpec(128, gcmIv); // 128-bit tag length
            } else {
                sr.nextBytes(initVector);
                Files.write(IV_FILE, initVector);
                LOG.info("IV saved to iv.base64: " + bytesToHex(initVector));
                iv = new IvParameterSpec(initVector);
            }

            skeySpec = new SecretKeySpec(key, ALGORITHM);

            if (cipherToUse.equals("GCM")) {
                cipher.init(Cipher.ENCRYPT_MODE, skeySpec, gcmSpec);
            } else if (cipherToUse.equals("ECB")) {
                cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            }

            return encryptFiles(cipher, Path.of(inputFile), Path.of(outputFile));
        } else if (operation.equals("dec")) {
            key = Files.readAllBytes(KEY_FILE);
            initVector = Files.readAllBytes(IV_FILE);
            LOG.info("Key loaded from key.base64: " + bytesToHex(key));
            LOG.info("IV loaded from iv.base64: " + bytesToHex(initVector));

            if (cipherToUse.equals("GCM")) {
                gcmSpec = new GCMParameterSpec(128, initVector);
            } else {
                iv = new IvParameterSpec(initVector);
            }

            skeySpec = new SecretKeySpec(key, ALGORITHM);

            if (cipherToUse.equals("GCM")) {
                cipher.init(Cipher.DECRYPT_MODE, skeySpec, gcmSpec);
            } else if (cipherToUse.equals("ECB")) {
                cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            } else {
                cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            }

            return decryptFiles(cipher, Path.of(inputFile), Path.of(outputFile));
        } else {
            System.err.println("Unknown operation: " + operation);
            System.exit(1);
        }

        return 0;
    }

    private static long encryptFiles(Cipher cipher, Path inputFile, Path outputFile) throws IOException {
        try {
            byte[] inputData = Files.readAllBytes(inputFile);

            long startTime = System.nanoTime();
            byte[] encryptedData = cipher.doFinal(inputData);
            long endTime = System.nanoTime();

            Files.write(outputFile, encryptedData);

            LOG.info("Encryption finished, saved at " + outputFile);
            return (endTime - startTime);
        } catch (GeneralSecurityException ex) {
            LOG.log(Level.SEVERE, "Error during encryption", ex);
            throw new IOException("Error during encryption", ex);
        }
    }

    private static long decryptFiles(Cipher cipher, Path inputFile, Path outputFile) throws IOException {
        try {
            byte[] encryptedData = Files.readAllBytes(inputFile);

            long startTime = System.nanoTime();
            byte[] decryptedData = cipher.doFinal(encryptedData);
            long endTime = System.nanoTime();

            Files.write(outputFile, decryptedData);

            LOG.info("Decryption complete, saved at " + outputFile);
            return (endTime - startTime);
        } catch (GeneralSecurityException ex) {
            LOG.log(Level.SEVERE, "Error during decryption", ex);
            throw new IOException("Error during decryption", ex);
        }
    }
}
