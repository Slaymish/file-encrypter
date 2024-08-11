package part1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
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
import java.util.Base64;

/**
 * FileEncryptor class for encrypting and decrypting files using AES.
 * Author: Hamish Burke
 */
public class Part1 {
    private static final Logger LOG = Logger.getLogger(Part1.class.getSimpleName());

    private static final String ALGORITHM = "AES";
    private static final Map<String, String> ciphers = Map.of(
            "CBC", "AES/CBC/PKCS5PADDING",
            "ECB", "AES/ECB/PKCS5PADDING",
            "CTR", "AES/CTR/NoPadding",
            "OFB", "AES/OFB/NoPadding",
            "CFB", "AES/CFB/NoPadding",
            "GCM", "AES/GCM/NoPadding");

    /**
     * Main method for encrypting and decrypting files using AES.
     *
     * @param args Command line arguments.
     * @throws NoSuchAlgorithmException
     * @throws NoSuchPaddingException
     * @throws InvalidKeyException
     * @throws InvalidAlgorithmParameterException
     * @throws IOException
     */
    public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IOException {
        if (args.length < 1) {
            System.err.println("Usage: java Part1 <enc/dec> -i <inputFile> -o <outputFile> [-k <keyFile>] [-iv <ivFile>] [-m <mode>]");
            return;
        }

        String operation = args[0]; // enc/dec
        String inputFile = null;
        String outputFile = null;
        String keyFile = null;
        String ivFile = null;
        String mode = "AES/CBC/PKCS5PADDING";

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "-i", "--input-file" -> inputFile = args[++i];
                case "-o", "--output-file" -> outputFile = args[++i];
                case "-k", "--key-file" -> keyFile = args[++i];
                case "-iv", "--initialisation-vector" -> ivFile = args[++i];
                case "-m", "--mode" -> mode = ciphers.getOrDefault(args[++i], "AES/CBC/PKCS5PADDING");
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    return;
                }
            }
        }

        if (inputFile == null) {
            System.err.println("Input file is required.");
            System.exit(1);
        }

        if (outputFile == null) { // set the output name if not provided
            outputFile = operation.equals("enc") ? inputFile + ".enc" : inputFile.replaceFirst("\\.enc$", "") + ".dec";
        }


        Path inputFilePath = Path.of(inputFile);
        if (!Files.exists(inputFilePath)) {
            LOG.severe("Input file does not exist: " + inputFilePath.toAbsolutePath());
            throw new NoSuchFileException(inputFilePath.toString());
        }

        Path outputFilePath = Path.of(outputFile);

        SecureRandom sr = new SecureRandom();
        byte[] key = new byte[16];
        byte[] initVector = new byte[16]; // 16 bytes IV
        IvParameterSpec iv;
        SecretKeySpec skeySpec;

        if (operation.equals("enc")) {
            if (keyFile == null) {
                sr.nextBytes(key); // Generate random key
                // Save the key file in the same directory as the input file
                Path keyFilePath = inputFilePath.resolveSibling("key.base64");
                Files.write(keyFilePath, Base64.getEncoder().encode(key));
                LOG.info("Key saved to " + keyFilePath.toAbsolutePath() + ": " + Util.bytesToHex(key));


            } else {
                key = Base64.getDecoder().decode(Files.readAllBytes(Path.of(keyFile)));
                LOG.info("Key loaded from " + keyFile + ": " + Util.bytesToHex(key));
            }

            if (ivFile == null) {
                sr.nextBytes(initVector); // Generate random IV
                Files.write(inputFilePath.resolveSibling("iv.base64"), Base64.getEncoder().encode(initVector));
                LOG.info("IV saved to iv.base64: " + Util.bytesToHex(initVector));
            } else {
                initVector = Base64.getDecoder().decode(Files.readAllBytes(Path.of(ivFile)));
                LOG.info("IV loaded from " + ivFile + ": " + Util.bytesToHex(initVector));
            }

            iv = new IvParameterSpec(initVector);
            skeySpec = new SecretKeySpec(key, ALGORITHM);
            Cipher cipher = Cipher.getInstance(mode);
            if (mode.equals(ciphers.get("CBC"))) {
                cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, skeySpec);
            }

            encryptFiles(cipher, inputFilePath, outputFilePath);
        } else if (operation.equals("dec")) {
            if (keyFile == null || ivFile == null) {
                System.err.println("Key file and IV file are required for decryption.");
                System.exit(1);
            }




            // Load the key file from the same directory as the input file
            Path keyFilePath = inputFilePath.resolveSibling("key.base64");
            key = Base64.getDecoder().decode(Files.readAllBytes(keyFilePath));
            LOG.info("Key loaded from " + keyFilePath.toAbsolutePath() + ": " + Util.bytesToHex(key));

            if (!Files.exists(keyFilePath)) {
                LOG.severe("Key file does not exist: " + keyFilePath.toAbsolutePath());
                throw new NoSuchFileException(keyFilePath.toString());
            }


            initVector = Base64.getDecoder().decode(Files.readAllBytes(Path.of(ivFile)));
            LOG.info("IV loaded from " + ivFile + ": " + Util.bytesToHex(initVector));

            iv = new IvParameterSpec(initVector);
            skeySpec = new SecretKeySpec(key, ALGORITHM);
            Cipher cipher = Cipher.getInstance(mode);

            if (mode.equals(ciphers.get("CBC"))) {
                cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);
            } else {
                cipher.init(Cipher.DECRYPT_MODE, skeySpec);
            }

            decryptFiles(cipher, inputFilePath, outputFilePath);
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
