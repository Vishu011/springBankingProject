package com.creditcardservice.util;

import java.security.SecureRandom;
import java.util.Random;

/**
 * Utility for generating PANs with simple brand-based prefixes and Luhn check digit.
 * Note: These are demo numbers, not real BINs. Marked clearly as demo.
 */
public final class CardNumberUtil {

    private static final Random RANDOM = new SecureRandom();

    private CardNumberUtil() {}

    public static String generatePan(String brand) {
        String prefix = switch (brand) {
            case "VISA" -> "4";
            case "MASTERCARD" -> randomMastercardPrefix();
            case "AMEX" -> randomAmexPrefix(); // 34 or 37
            case "DISCOVERY" -> "6";
            case "RUPAY" -> "60";
            default -> "9";
        };

        int length = brand.equals("AMEX") ? 15 : 16;
        StringBuilder sb = new StringBuilder(prefix);

        // Fill with random digits until length-1 (reserve last for check digit)
        while (sb.length() < length - 1) {
            sb.append(RANDOM.nextInt(10));
        }

        // Compute Luhn check digit
        int check = luhnCheckDigit(sb.toString());
        sb.append(check);

        return sb.toString();
    }

    public static int luhnCheckDigit(String numberWithoutCheck) {
        int sum = 0;
        boolean doubleIt = true; // start from rightmost of numberWithoutCheck (which will become second from right)
        for (int i = numberWithoutCheck.length() - 1; i >= 0; i--) {
            int n = numberWithoutCheck.charAt(i) - '0';
            if (doubleIt) {
                n *= 2;
                if (n > 9) n = (n % 10) + 1;
            }
            sum += n;
            doubleIt = !doubleIt;
        }
        return (10 - (sum % 10)) % 10;
    }

    private static String randomMastercardPrefix() {
        // 51-55 simple demo range
        int start = 51 + RANDOM.nextInt(5);
        return String.valueOf(start);
    }

    private static String randomAmexPrefix() {
        return RANDOM.nextBoolean() ? "34" : "37";
    }

    public static String maskPan(String pan) {
        if (pan == null || pan.length() < 4) return "****";
        String last4 = pan.substring(pan.length() - 4);
        return "**** **** **** " + last4;
    }
}
