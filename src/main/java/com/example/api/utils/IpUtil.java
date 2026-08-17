package com.example.api.utils;

import jakarta.servlet.http.HttpServletRequest;

public class IpUtil {

    private static final String LOOPBACK_ADDRESS = "127.0.0.1";
    private static final String IPV6_ADDRESS = "0:0:0:0:0:0:0:1";

    /**
     * Returns the client's real IP from the HttpServletRequest, resolving through chained proxies.
     */
    public static String getIpAddr(HttpServletRequest request) {
        try {
            String ip = request.getHeader("x-forwarded-for");
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("WL-Proxy-Client-IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_CLIENT_IP");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getHeader("HTTP_X_FORWARDED_FOR");
            }
            if (ip == null || ip.length() == 0 || "unknown".equalsIgnoreCase(ip)) {
                ip = request.getRemoteAddr();
            }
            //on Windows, localhost comes back as IPv6 0:0:0:0:0:0:0:1; normalize it to 127.0.0.1
            if (IPV6_ADDRESS.equals(ip)) {
                ip = LOOPBACK_ADDRESS;
            }
            return ip;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }
}
