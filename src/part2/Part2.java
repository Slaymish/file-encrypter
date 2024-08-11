package part2;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class Part2 {
    private static final Logger LOG = Logger.getLogger(Part2.class.getSimpleName());

    private static final String ALGORITHM = "AES";
    private static final String CIPHER = "AES/CBC/PKCS5PADDING";
    private static final String KDF_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int KEY_SIZE = 128;
    private static final int ITERATIONS = 65536;
    private static final int SALT_SIZE = 16;

    public static void main(String[] args) throws NoSuchAlgorithmException, NoSuchPaddingException, InvalidKeySpecException, IOException, InvalidAlgorithmParameterException, InvalidKeyException {
        if (args.length < 1) {
            System.err.println("Usage: java Part2 <enc/dec> -i <inputFile> -o <outputFile> -p <password>");
            System.exit(1);
        }

        String operation = args[0]; // enc/dec
        String inputFile = null;
        String outputFile = null;
        String password = null;

        for (int i = 1; i < args.length; i++) {
            switch (args[i]) {
                case "-i", "--input-file" -> inputFile = args[++i];
                case "-o", "--output-file" -> outputFile = args[++i];
                case "-p", "--pass" -> password = args[++i];
                default -> {
                    System.err.println("Unknown argument: " + args[i]);
                    System.exit(1);
                }
            }
        }

        if (inputFile == null || outputFile == null || password == null) {
            System.err.println("Input file, output file, and password are required.");
            System.exit(1);
        }

        byte[] salt = new byte[SALT_SIZE];
        SecureRandom sr = new SecureRandom();

        if (operation.equals("enc")) {
            sr.nextBytes(salt);
            byte[] key = deriveKey(password.toCharArray(), salt);
            byte[] initVector = new byte[16];
            sr.nextBytes(initVector);
            IvParameterSpec iv = new IvParameterSpec(initVector);
            SecretKeySpec skeySpec = new SecretKeySpec(key, ALGORITHM);

            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.ENCRYPT_MODE, skeySpec, iv);

            LOG.info("Derived key from password: " + Base64.getEncoder().encodeToString(key));
            LOG.info("Derived IV from password: " + Base64.getEncoder().encodeToString(initVector));
            LOG.info("Salt used: " + Base64.getEncoder().encodeToString(salt));

            encryptFiles(cipher, Path.of(inputFile), Path.of(outputFile), salt, initVector);
        } else if (operation.equals("dec")) {
            byte[] fileContent = Files.readAllBytes(Path.of(inputFile));
            System.arraycopy(fileContent, 0, salt, 0, SALT_SIZE);
            byte[] initVector = new byte[16];
            System.arraycopy(fileContent, SALT_SIZE, initVector, 0, 16);

            byte[] key = deriveKey(password.toCharArray(), salt);
            IvParameterSpec iv = new IvParameterSpec(initVector);
            SecretKeySpec skeySpec = new SecretKeySpec(key, ALGORITHM);

            Cipher cipher = Cipher.getInstance(CIPHER);
            cipher.init(Cipher.DECRYPT_MODE, skeySpec, iv);

            LOG.info("Derived key from password: " + Base64.getEncoder().encodeToString(key));
            LOG.info("Derived IV from password: " + Base64.getEncoder().encodeToString(initVector));
            LOG.info("Salt used: " + Base64.getEncoder().encodeToString(salt));

            decryptFiles(cipher, fileContent, Path.of(outputFile));
        } else {
            System.err.println("Unknown operation: " + operation);
            System.exit(1);
        }
    }

    private static byte[] deriveKey(char[] password, byte[] salt) throws NoSuchAlgorithmException, InvalidKeySpecException {
        PBEKeySpec spec = new PBEKeySpec(password, salt, ITERATIONS, KEY_SIZE);
        SecretKeyFactory keyFactory = SecretKeyFactory.getInstance(KDF_ALGORITHM);
        return keyFactory.generateSecret(spec).getEncoded();
    }

    private static void encryptFiles(Cipher cipher, Path inputFile, Path outputFile, byte[] salt, byte[] initVector) {
        try (InputStream fin = Files.newInputStream(inputFile);
             OutputStream fout = Files.newOutputStream(outputFile);
             CipherOutputStream cipherOut = new CipherOutputStream(fout, cipher)) {

            fout.write(salt);
            fout.write(initVector);

            final byte[] bytes = new byte[1024];
            int length;
            while ((length = fin.read(bytes)) != -1) {
                cipherOut.write(bytes, 0, length);
            }
        } catch (IOException e) {
            LOG.log(Level.INFO, "Unable to encrypt", e);
            System.err.println("Error: Unable to encrypt the file. Please check the input file and try again.");
        }

        LOG.info("Encryption finished, saved at " + outputFile);
        System.out.println("Encryption finished, saved at " + outputFile);
    }

    private static void decryptFiles(Cipher cipher, byte[] fileContent, Path outputFile) {
        try (CipherInputStream decryptStream = new CipherInputStream(new ByteArrayInputStream(fileContent, SALT_SIZE + 16, fileContent.length - (SALT_SIZE + 16)), cipher);
             OutputStream decryptedOut = Files.newOutputStream(outputFile)) {

            final byte[] bytes = new byte[1024];
            int length;
            while ((length = decryptStream.read(bytes)) != -1) {
                decryptedOut.write(bytes, 0, length);
            }
        } catch (IOException ex) {
            LOG.log(Level.SEVERE, "IOException during decryption", ex);
            System.err.println("Error: Unable to decrypt the file. Please check the input file and try again.");
        }

        LOG.info("Decryption complete, saved at " + outputFile);
        System.out.println("Decryption complete, saved at " + outputFile);
    }
}
