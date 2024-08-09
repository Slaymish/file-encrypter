package main.java.part1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static main.java.part1.Util.bytesToHex;

/**
 * FileEncryptor class for encrypting and decrypting files using AES.
 * Author: Hamish Burke
 */
public class Part1 {
    private static final Logger LOG = Logger.getLogger(Part1.class.getSimpleName());

    private static final String ALGORITHM = "AES";

    private static final String CIPHER = "AES/CBC/PKCS5PADDING";

    public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IOException {
        if (args.length < 1) {
            System.err.println("Usage: java Part1 <enc/dec> -i <inputFile> -o <outputFile> [-k <keyFile>] [-iv <ivFile>]");
            return;
        }

        String operation = args[0]; // enc/dec
        String inputFile = null;
        String outputFile = null;
        String keyFile = null;
        String ivFile = null;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "-i" -> inputFile = args[++i];
                case "-o" -> outputFile = args[++i];
                case "-k" -> keyFile = args[++i];
                case "-iv" -> ivFile = args[++i];
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    return;
                }
            }
        }

        if (inputFile == null || outputFile == null) {
            System.err.println("Input and output files are required.");
            System.exit(1);
        }

        SecureRandom sr = new SecureRandom();
        byte[] key = new byte[16];
        byte[] initVector = new byte[16];
        IvParameterSpec iv;
        SecretKeySpec skeySpec;

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
                sr.nextBytes(initVector); // 16 bytes IV
                Files.write(Path.of("iv.base64"), initVector);
                LOG.info("IV saved to iv.base64: " + bytesToHex(initVector));
            } else {
                initVector = Files.readAllBytes(Path.of(ivFile));
                LOG.info("IV loaded from " + ivFile + ": " + bytesToHex(initVector));
            }

            iv = new IvParameterSpec(initVector);
            skeySpec = new SecretKeySpec(key, ALGORITHM);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            encryptFiles(cipher, Path.of(inputFile), Path.of(outputFile));
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
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            decryptFiles(cipher, Path.of(inputFile), Path.of(outputFile));
        } else {
            System.err.println("Unknown operation: " + operation);
            System.exit(1);
        }
    }

    /**
     * Encrypt the input file and write to the output file.
     * @param cipher The initialized Cipher for encryption.
     * @param inputFile The input file path.
     * @param outputFile The output file path.
     */
    private static void encryptFiles(Cipher cipher, Path inputFile, Path outputFile) throws IOException {
        try (InputStream fin = Files.newInputStream(inputFile);
             OutputStream fout = Files.newOutputStream(outputFile);
             CipherOutputStream cipherOut = new CipherOutputStream(fout, cipher)) {

            final byte[] bytes = new byte[1024];
            int length;
            while ((length = fin.read(bytes)) != -1) {
                cipherOut.write(bytes, 0, length);
            }
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Unable to encrypt", e);
            throw e;  // Ensure the exception is thrown
        }

        LOG.info("Encryption finished, saved at " + outputFile);
        System.out.println("Encryption finished, saved at " + outputFile);
    }

    /**
     * Decrypt the input file and write to the output file.
     * @param cipher The initialized Cipher for decryption.
     * @param inputFile The input file path.
     * @param outputFile The output file path.
     */
    private static void decryptFiles(Cipher cipher, Path inputFile, Path outputFile) throws IOException {
        try (InputStream encryptedData = Files.newInputStream(inputFile);
             CipherInputStream decryptStream = new CipherInputStream(encryptedData, cipher);
             OutputStream decryptedOut = Files.newOutputStream(outputFile)) {

            final byte[] bytes = new byte[1024];
            int length;
            while ((length = decryptStream.read(bytes)) != -1) {
                decryptedOut.write(bytes, 0, length);
            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "IOException during decryption", ex);
            throw ex;  // Ensure the exception is thrown
        } catch (Exception ex) {
            LOG.log(Level.SEVERE, "Exception during decryption", ex);
            throw new IOException("Error during decryption", ex);
        }

        LOG.info("Decryption complete, saved at " + outputFile);
        System.out.println("Decryption complete, saved at " + outputFile);
    }
}
