package com.creditcardservice.util;

import java.security.SecureRandom;
import java.util.Random;

public final class CvvUtil {

    private static final Random RANDOM = new SecureRandom();

    private CvvUtil() {}

    public static String generateCvv(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }

    public static String maskCvv(String cvv) {
        if (cvv == null) return null;
        return "*".repeat(cvv.length());
    }
}
