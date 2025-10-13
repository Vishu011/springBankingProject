package com.otp.util;

import jakarta.servlet.http.HttpServletRequest;

public final class IpUtils {
    private IpUtils() {}

    /**
     * Extract client IP address considering reverse proxies.
     * Checks X-Forwarded-For (first IP), then X-Real-IP, then remoteAddr.
     */
    public static String extractClientIp(HttpServletRequest request) {
        if (request == null) return null;

        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            // XFF can contain multiple IPs: "client, proxy1, proxy2"
            String first = xff.split(",")[0].trim();
            if (!first.isBlank()) return first;
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isBlank()) {
            return xRealIp.trim();
        }

        String remote = request.getRemoteAddr();
        return (remote != null && !remote.isBlank()) ? remote : null;
    }
}
