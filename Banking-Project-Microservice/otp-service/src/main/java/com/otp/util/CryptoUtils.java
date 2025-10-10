package com.otp.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

public final class CryptoUtils {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private CryptoUtils() {}

    public static String randomNumericCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        // generate uniformly distributed digits
        for (int i = 0; i < length; i++) {
            int digit = SECURE_RANDOM.nextInt(10);
            sb.append(digit);
        }
        return sb.toString();
    }

    public static String randomSaltHex(int bytes) {
        byte[] salt = new byte[bytes];
        SECURE_RANDOM.nextBytes(salt);
        return toHex(salt);
    }

    public static String sha256Hex(String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return toHex(digest);
        } catch (NoSuchAlgorithmException e) {
            // Should never happen in a standard JRE
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private static String toHex(byte[] bytes) {
        char[] hexArray = "0123456789abcdef".toCharArray();
        char[] hexChars = new char[bytes.length * 2];
        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }
}
