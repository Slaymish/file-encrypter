package main.java.part3;

import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.*;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import static main.java.part1.Util.bytesToHex;

/**
 * FileEncryptor class for encrypting and decrypting files using AES.
 * Author: Hamish Burke
 */
public class Part3 {
    private static final Logger LOG = Logger.getLogger(Part3.class.getSimpleName());
    private static final String ALGORITHM = "AES";

    private static final Map<String,String> ciphers = Map.of(
            "CBC","AES/CBC/PKCS5PADDING",
            "ECB","AES/ECB/PKCS5PADDING");

    public static long main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IOException {
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
                    return 0;
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
