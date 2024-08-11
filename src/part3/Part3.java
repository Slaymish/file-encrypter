package part3;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
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

import static part1.Util.bytesToHex;

/**
 * FileEncryptor class for encrypting and decrypting files using AES.
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

    private static final Map<String,String> ciphers = Map.of(
            "CBC","AES/CBC/PKCS5PADDING",
            "ECB","AES/ECB/PKCS5PADDING");

    private static void createFile(Path path, Integer size) throws IOException {
        byte[] data = new byte[(int) size];  // Create a byte array with the desired size
        Files.write(path, data, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        // truncate existing means if the file already exists, it will be truncated to 0 bytes
    }

    public static void main(String[] args) {
        System.out.println("Testing Performance...");

        // args = "-o results.csv"
        // args = "-repeats 10"
        String outputFileName = "results.csv";
        int repeatAmount = 10;

        for (int i = 0; i < args.length; i++) {
            switch (args[i]) {
                case "-o" -> outputFileName = args[++i];
                case "-repeats" -> repeatAmount = Integer.parseInt(args[++i]);
            }
        }



        List<String> keySizes = List.of("16", "24", "32"); // 128, 192, 256 bits
        List<String> modes = List.of("CBC", "ECB");
        List<Integer> fileSize = List.of(10 * 1024, 100 * 1024, 1000 * 1024); // file sizes in KB (adjust as needed)
        Map<String, Long> encTimes = new HashMap<>(); // key = mode + keySize + fileSize, value = time taken
        Map<String, Long> decTimes = new HashMap<>();

        // Measure performance
        for (String mode : modes) {
            for (Integer size : fileSize) {
                for (String keySize : keySizes) {
                    long encTime = 0;
                    long decTime = 0;
                    for (int i = 0; i < repeatAmount; i++) {
                        long encryptionTime = 0;
                        long decryptionTime = 0;
                        try {
                            // Create a file of specified size
                            createFile(INPUT_FILE, size);

                            encryptionTime = checkPerformance(new String[]{"enc", "-i", INPUT_FILE.toString(), "-o", ENCRYPTED_FILE.toString(), "-c", mode, "-ks", keySize});
                            decryptionTime = checkPerformance(new String[]{"dec", "-k", KEY_FILE.toString(), "-iv", IV_FILE.toString(), "-i", ENCRYPTED_FILE.toString(), "-o", DECRYPTED_FILE.toString(), "-c", mode, "-ks", keySize});

                            // Clean up before the next test
                            Files.deleteIfExists(ENCRYPTED_FILE);
                            Files.deleteIfExists(DECRYPTED_FILE);
                        } catch (IOException | NoSuchAlgorithmException | NoSuchPaddingException |
                                 InvalidAlgorithmParameterException | InvalidKeyException e) {
                            e.printStackTrace();
                        }

                        encTime += encryptionTime;
                        decTime += decryptionTime;
                    }

                    encTimes.put(mode + "-" + keySize + "-" + size, encTime / repeatAmount);
                    decTimes.put(mode + "-" + keySize + "-" + size, decTime / repeatAmount);
                }
            }
        }

        // Write the results to a CSV file
        try (java.io.PrintStream out = new java.io.PrintStream(outputFileName)) {
            out.println("Mode,Key Size (bits),File Size (KB),Encryption Time (ns),Decryption Time (ns)");
            for (String key : encTimes.keySet()) {
                String[] parts = key.split("-");
                String mode = parts[0];
                String keySize = parts[1];
                String size = parts[2];
                out.println(mode + "," + keySize + "," + size + "," + encTimes.get(key) + "," + decTimes.get(key));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        System.out.println("Performance comparison completed and saved to " + outputFileName);

        // Clean up
        try {
            Files.deleteIfExists(INPUT_FILE);
            Files.deleteIfExists(KEY_FILE);
            Files.deleteIfExists(IV_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private static long checkPerformance(String[] args) throws IOException, NoSuchPaddingException, NoSuchAlgorithmException, InvalidAlgorithmParameterException, InvalidKeyException {

        if (args.length < 1) {
            System.err.println("Usage: java Part1 <enc/dec> -i <inputFile> -o <outputFile> [-k <keyFile>] [-iv <ivFile>] [-c <CBC/EBC>]");
            return 0;
        }

        String operation = args[0]; // enc/dec
        String inputFile = null;
        String outputFile = null;
        String keyFile = null;
        String ivFile = null;
        String cipherToUse = "CBC";
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
        LOG.info("Using keysize and iv: " + keySize);

        if (inputFile == null || outputFile == null) {
            System.err.println("Input and output files are required.");
            System.exit(1);
        }

        SecureRandom sr = new SecureRandom();
        byte[] key = new byte[keySize];
        byte[] initVector = new byte[16];
        IvParameterSpec iv;
        SecretKeySpec skeySpec;
        Cipher cipher = Cipher.getInstance(ciphers.get(cipherToUse));

        if (operation.equals("enc")) {
            if (keyFile == null) {
                sr.nextBytes(key); // 128 bit key
                Files.write(Path.of("key.base64"), key);
                LOG.info("Key saved to key.base64: " + bytesToHex(key));
            } else {
                key = Files.readAllBytes(Path.of(keyFile));
                LOG.info("Key loaded from " + keyFile + ": " + bytesToHex(key));
            }

            if (ivFile == null) {
                sr.nextBytes(initVector);
                Files.write(Path.of("iv.base64"), initVector);
                LOG.info("IV saved to iv.base64: " + bytesToHex(initVector));
            } else {
                initVector = Files.readAllBytes(Path.of(ivFile));
                LOG.info("IV loaded from " + ivFile + ": " + bytesToHex(initVector));
            }

            iv = new IvParameterSpec(initVector);
            skeySpec = new SecretKeySpec(key, ALGORITHM);

            if (cipherToUse.equals("CBC"))
                cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            else
                cipher.init(Cipher.ENCRYPT_MODE,skeySpec);

            return encryptFiles(cipher, Path.of(inputFile), Path.of(outputFile));
        } else if (operation.equals("dec")) {
            if (keyFile == null || ivFile == null) {
                System.err.println("Key file and IV file are required for decryption.");
                System.exit(1);
            }

            key = Files.readAllBytes(Path.of(keyFile));
            initVector = Files.readAllBytes(Path.of(ivFile));
            LOG.info("Key loaded from " + keyFile + ": " + bytesToHex(key));
            LOG.info("IV loaded from " + ivFile + ": " + bytesToHex(initVector));

            iv = new IvParameterSpec(initVector);
            skeySpec = new SecretKeySpec(key, ALGORITHM);

            if (cipherToUse.equals("CBC"))
                cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            else
                cipher.init(Cipher.DECRYPT_MODE,skeySpec);

            return decryptFiles(cipher, Path.of(inputFile), Path.of(outputFile));

        } else {
            System.err.println("Unknown operation: " + operation);
            System.exit(1);
        }

        return 0;
    }

    /**
     * Encrypt the input file and write to the output file.
     *
     * @param cipher     The initialized Cipher for encryption.
     * @param inputFile  The input file path.
     * @param outputFile The output file path.
     * @return
     */
    private static long encryptFiles(Cipher cipher, Path inputFile, Path outputFile) throws IOException {
        try {
            // Read the entire input file into memory
            byte[] inputData = Files.readAllBytes(inputFile);

            // Measure time taken for encryption only
            long startTime = System.nanoTime();
            byte[] encryptedData = cipher.doFinal(inputData);
            long endTime = System.nanoTime();

            // Write the encrypted data to the output file
            Files.write(outputFile, encryptedData);

            LOG.info("Encryption finished, saved at " + outputFile);
            System.out.println("Encryption finished, saved at " + outputFile);
            System.out.println("Time taken for encryption: " + (endTime - startTime) + " nanoseconds");
            return (endTime-startTime);
        } catch (GeneralSecurityException ex) {
            LOG.log(Level.SEVERE, "Error during encryption", ex);
            throw new IOException("Error during encryption", ex);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "IOException during file operation", ex);
            throw ex;
        }
    }

    /**
     * Decrypt the input file and write to the output file.
     * @param cipher The initialized Cipher for decryption.
     * @param inputFile The input file path.
     * @param outputFile The output file path.
     */
    private static long decryptFiles(Cipher cipher, Path inputFile, Path outputFile) throws IOException {
        try {
            // Read the entire encrypted file into memory
            byte[] encryptedData = Files.readAllBytes(inputFile);

            // Measure time taken for decryption only
            long startTime = System.nanoTime();
            byte[] decryptedData = cipher.doFinal(encryptedData);
            long endTime = System.nanoTime();

            // Write the decrypted data to the output file
            Files.write(outputFile, decryptedData);

            LOG.info("Decryption complete, saved at " + outputFile);
            System.out.println("Decryption complete, saved at " + outputFile);
            System.out.println("Time taken for decryption: " + (endTime - startTime) + " nanoseconds");
            return (endTime-startTime);
        } catch (GeneralSecurityException ex) {
            LOG.log(Level.SEVERE, "Error during decryption", ex);
            throw new IOException("Error during decryption", ex);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "IOException during file operation", ex);
            throw ex;
        }
    }
}
