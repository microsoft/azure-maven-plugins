/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HashMacUtils {

    private static final Pattern HASHED_MAC_PATTERN = Pattern.compile("[0-9a-f]{64}");
    // Hashed mac address for iBridge device
    private static final String INVALID_HASHED_MAC_ADDRESS = "6c9d2bc8f91b89624add29c0abeae7fb42bf539fa1cdb2e3e57cd668fa9bcead";

    public static boolean isValidHashMac(String hashMac) {
        return isValidHash(hashMac) && !isDepreciatedHashMac(hashMac);
    }

    public static String getHashMac() {
        String mac = MacUtils.getMac();
        return StringUtils.isNotBlank(mac) ? hash(mac) : null;
    }

    public static String hash(String mac) {
        if (StringUtils.isEmpty(mac)) {
            return null;
        }
        String ret;
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] bytes = mac.getBytes(StandardCharsets.UTF_8);
            md.update(bytes);
            byte[] bytesAfterDigest = md.digest();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < bytesAfterDigest.length; i++) {
                sb.append(Integer.toString((bytesAfterDigest[i] & 0xff) + 0x100, 16).substring(1));
            }
            ret = sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
        return ret;
    }

    private static boolean isValidHash(String hashMac) {
        if (StringUtils.isBlank(hashMac)) {
            return false;
        }
        Matcher matcher = HASHED_MAC_PATTERN.matcher(hashMac);
        return matcher.matches();
    }

    private static boolean isDepreciatedHashMac(String hashMac) {
        return StringUtils.containsIgnoreCase(System.getProperty("os.name"), "mac") && StringUtils.equalsIgnoreCase(INVALID_HASHED_MAC_ADDRESS, hashMac);
    }
}
