package com.challenge.badapi.util;

import jakarta.servlet.http.HttpServletRequest;

public final class ClientIpUtils {

    private ClientIpUtils() {
    }

    /**
     * Resolves the client IP, preferring X-Forwarded-For (set by reverse
     * proxies / tunnels) over the raw socket address.
     */
    public static String getClientIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isEmpty()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }
}
