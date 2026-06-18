package com.hyperion.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Base64;

public final class PinHashUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private PinHashUtil() {
    }

    public static String hash(String pin) {
        byte[] salt = generateSalt();
        byte[] hash = generateHash(pin.toCharArray(), salt);

        return String.join(":",
                ALGORITHM,
                String.valueOf(ITERATIONS),
                Base64.getEncoder().encodeToString(salt),
                Base64.getEncoder().encodeToString(hash)
        );
    }

    public static boolean verify(String pin, String storedHash) {
        if (pin == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        String[] parts = storedHash.split(":");

        if (parts.length != 4) {
            return false;
        }

        String algorithm = parts[0];
        int iterations = Integer.parseInt(parts[1]);
        byte[] salt = Base64.getDecoder().decode(parts[2]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[3]);
        byte[] actualHash = generateHash(pin.toCharArray(), salt, algorithm, iterations);

        boolean matches = MessageDigest.isEqual(expectedHash, actualHash);
        Arrays.fill(actualHash, (byte) 0);
        return matches;
    }

    private static byte[] generateSalt() {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        return salt;
    }

    private static byte[] generateHash(char[] pin, byte[] salt) {
        return generateHash(pin, salt, ALGORITHM, ITERATIONS);
    }

    private static byte[] generateHash(char[] pin, byte[] salt, String algorithm, int iterations) {
        try {
            PBEKeySpec spec = new PBEKeySpec(pin, salt, iterations, KEY_LENGTH);
            SecretKeyFactory factory = SecretKeyFactory.getInstance(algorithm);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("Could not hash PIN.", exception);
        }
    }
}
