package com.ecematerial;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

public final class HashingUtil {
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65_536;
    private static final int KEY_LENGTH = 256;
    private static final int SALT_LENGTH = 16;

    private HashingUtil() {
    }

    public static String hashValue(String value) {
        byte[] salt = new byte[SALT_LENGTH];
        new SecureRandom().nextBytes(salt);
        byte[] hash = derive(value.toCharArray(), salt);
        return Base64.getEncoder().encodeToString(salt)
            + ":" + ITERATIONS
            + ":" + Base64.getEncoder().encodeToString(hash);
    }

    public static boolean matches(String rawValue, String storedHash) {
        if (rawValue == null || storedHash == null || storedHash.isBlank()) {
            return false;
        }

        String[] parts = storedHash.split(":");
        if (parts.length != 3) {
            return false;
        }

        byte[] salt = Base64.getDecoder().decode(parts[0]);
        int iterations = Integer.parseInt(parts[1]);
        byte[] expectedHash = Base64.getDecoder().decode(parts[2]);
        byte[] actualHash = derive(rawValue.toCharArray(), salt, iterations);
        return java.security.MessageDigest.isEqual(expectedHash, actualHash);
    }

    private static byte[] derive(char[] value, byte[] salt) {
        return derive(value, salt, ITERATIONS);
    }

    private static byte[] derive(char[] value, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(value, salt, iterations, KEY_LENGTH);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException exception) {
            throw new IllegalStateException("Failed to hash credential", exception);
        } finally {
            spec.clearPassword();
        }
    }
}
