package top.ilovemyhome.dagtask.core.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Utility class for matching IP addresses against CIDR notation or exact IP.
 */
public final class IpAddressMatcher {

    private static final Logger LOGGER = LoggerFactory.getLogger(IpAddressMatcher.class);

    private IpAddressMatcher() {
    }

    /**
     * Check if the given client IP matches the pattern.
     * The pattern can be either a CIDR notation (e.g., "192.168.0.0/24")
     * or an exact IP address (e.g., "192.168.1.1").
     *
     * @param pattern  the CIDR or exact IP to match against
     * @param clientIp the client IP address
     * @return true if matches, false otherwise
     */
    public static boolean matches(String pattern, String clientIp) {
        if (pattern == null || clientIp == null) {
            return false;
        }

        pattern = pattern.trim();
        clientIp = clientIp.trim();

        if (pattern.isEmpty() || clientIp.isEmpty()) {
            return false;
        }

        try {
            if (pattern.contains("/")) {
                return matchesCidr(pattern, clientIp);
            }
            return pattern.equals(clientIp);
        } catch (Exception e) {
            LOGGER.warn("Failed to match IP pattern [{}] against client IP [{}]: {}",
                pattern, clientIp, e.getMessage());
            return false;
        }
    }

    private static boolean matchesCidr(String cidr, String clientIp) throws UnknownHostException {
        String[] parts = cidr.split("/");
        if (parts.length != 2) {
            return false;
        }

        String networkIp = parts[0];
        int prefixLength;
        try {
            prefixLength = Integer.parseInt(parts[1]);
        } catch (NumberFormatException e) {
            return false;
        }

        InetAddress networkAddress = InetAddress.getByName(networkIp);
        InetAddress clientAddress = InetAddress.getByName(clientIp);

        byte[] networkBytes = networkAddress.getAddress();
        byte[] clientBytes = clientAddress.getAddress();

        if (networkBytes.length != clientBytes.length) {
            return false;
        }

        int fullBytes = prefixLength / 8;
        int remainingBits = prefixLength % 8;

        for (int i = 0; i < fullBytes; i++) {
            if (networkBytes[i] != clientBytes[i]) {
                return false;
            }
        }

        if (remainingBits > 0) {
            int mask = 0xFF << (8 - remainingBits);
            return (networkBytes[fullBytes] & mask) == (clientBytes[fullBytes] & mask);
        }

        return true;
    }
}
