package part1;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import static part1.Util.bytesToHex;

/**
 *
 * @author Erik Costlow
 */
public class FileEncryptor {
    private static final Logger LOG = Logger.getLogger(FileEncryptor.class.getSimpleName());

    private static final String ALGORITHM = "AES";
    private static final String CIPHER = "AES/CBC/PKCS5PADDING";

    public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeyException, InvalidAlgorithmParameterException, IOException {
        if (args.length < 1) {
            System.err.println("Usage: java FileEncryptor <enc/dec> -i <inputFile> -o <outputFile> [-k <keyFile>] [-iv <ivFile>]");
            System.exit(1);
        }

        String operation = args[0]; // enc/dec
        String inputFile = null;
        String outputFile = null;
        String keyFile = null;
        String ivFile = null;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "-i" -> inputFile = args[++i]; // increments THEN returns
                case "-o" -> outputFile = args[++i];
                case "-k" -> keyFile = args[++i];
                case "-iv" -> ivFile = args[++i];
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    System.exit(1);
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
            sr.nextBytes(key); // 128 bit key
            sr.nextBytes(initVector); // 16 bytes IV
            iv = new IvParameterSpec(initVector);
            skeySpec = new SecretKeySpec(key, ALGORITHM);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            if (keyFile != null) {
                Files.write(Path.of(keyFile), key);
                LOG.info("Key saved to " + keyFile);
            }
            if (ivFile != null) {
                Files.write(Path.of(ivFile), initVector);
                LOG.info("IV saved to " + ivFile);
            }


            encryptFiles(cipher, Path.of(inputFile), Path.of(outputFile));
        } else if (operation.equals("dec")) {
            if (keyFile != null) {
                key = Files.readAllBytes(Path.of(keyFile));
                LOG.info("Key loaded from " + keyFile + ": " + Util.bytesToHex(key));
            }
            if (ivFile != null) {
                initVector = Files.readAllBytes(Path.of(ivFile));
                LOG.info("IV loaded from " + ivFile + ": " + Util.bytesToHex(initVector));
            }

            iv = new IvParameterSpec(initVector);
            skeySpec = new SecretKeySpec(key, ALGORITHM);
            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            decryptFiles(cipher, Path.of(inputFile), Path.of(outputFile), skeySpec, iv);
        } else {
            System.err.println("Unknown Operation " + operation);
            System.exit(1);
        }
    }


    /**
     * ENCYRPT yo SHII
     * @param cipher
     * @param inputFile
     * @param outputFile
     */
    private static void encryptFiles(Cipher cipher, Path inputFile, Path outputFile) {
        //Look for files here
        try (InputStream fin = Files.newInputStream(inputFile);
             OutputStream fout = Files.newOutputStream(outputFile);
             CipherOutputStream cipherOut = new CipherOutputStream(fout, cipher) {
             }) {

            final byte[] bytes = new byte[1024];
            for(int length=fin.read(bytes); length!=-1; length = fin.read(bytes)){
                cipherOut.write(bytes, 0, length);
            }
        } catch (IOException e) {
            LOG.log(Level.INFO, "Unable to encrypt", e);
        }

        LOG.info("Encryption finished, saved at " + outputFile);
    }


    /**
     * Decypt yo files!
     *
     * @param cipher
     * @param inputFile
     * @param outputFile
     * @param skeySpec
     * @param iv
     * @throws InvalidAlgorithmParameterException
     * @throws InvalidKeyException
     */
    private static void decryptFiles(Cipher cipher, Path inputFile, Path outputFile, SecretKeySpec skeySpec, IvParameterSpec iv) {
        try {
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            LOG.info("Decrypting with key: " + Util.bytesToHex(skeySpec.getEncoded()));
            LOG.info("Decrypting with IV: " + bytesToHex(iv.getIV()));

            try (InputStream encryptedData = Files.newInputStream(inputFile);
                 CipherInputStream decryptStream = new CipherInputStream(encryptedData, cipher);
                 OutputStream decryptedOut = Files.newOutputStream(outputFile)) {

                final byte[] bytes = new byte[1024];
                int length;
                while ((length = decryptStream.read(bytes)) != -1) {
                    decryptedOut.write(bytes, 0, length);
                }
            }

            LOG.info("Decryption complete, open " + outputFile);
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "IOException during decryption", ex);
        } catch (InvalidAlgorithmParameterException | InvalidKeyException ex) {
            LOG.log(Level.SEVERE, "Error during decryption setup", ex);
        }
    }

}