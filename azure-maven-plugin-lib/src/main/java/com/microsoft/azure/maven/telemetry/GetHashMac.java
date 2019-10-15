/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

/**
 * Disclaimer:
 *      This class is copied from https://github.com/Microsoft/azure-tools-for-java/ with minor modification (fixing
 *      static analysis error).
 *      Location in the repo: /Utils/azuretools-core/src/com/microsoft/azuretools/azurecommons/util/GetHashMac.java
 */

package com.microsoft.azure.maven.telemetry;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetHashMac {

    private static final String MAC_REGEX = "([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}";
    private static final String MAC_REGEX_ZERO = "([0]{2}[:-]){5}[0]{2}";
    private static final Pattern MAC_PATTERN = Pattern.compile(MAC_REGEX);

    public static final String[] UNIX_COMMAND = {"/sbin/ifconfig -a || /sbin/ip link"};
    public static final String[] WINDOWS_COMMAND = {"getmac"};
    public static final String[] INVALIDATE_MAC_ADDRESS = {"00:00:00:00:00:00", "ff:ff:ff:ff:ff:ff", "ac:de:48:00:11:22"};

    public static String getHashMac() {
        final String rawMac = getRawMac();
        if (!isValidRawMac(rawMac)) {
            return null;
        }

        final Pattern patternZero = Pattern.compile(MAC_REGEX_ZERO);
        final Matcher matcher = MAC_PATTERN.matcher(rawMac);
        String mac = "";
        while (matcher.find()) {
            mac = matcher.group(0);
            if (!patternZero.matcher(mac).matches()) {
                break;
            }
        }

        return hash(mac);
    }

    private static boolean isValidRawMac(String mac) {
        final boolean isMacAddress = StringUtils.isNotEmpty(mac) && MAC_PATTERN.matcher(mac).find();
        final boolean isValidateMacAddress = !Arrays.stream(INVALIDATE_MAC_ADDRESS)
                .anyMatch(invalidateMacAddress -> StringUtils.equalsIgnoreCase(mac, invalidateMacAddress));
        return isMacAddress && isValidateMacAddress;
    }

    private static String getRawMac() {
        final StringBuilder ret = new StringBuilder();
        try {
            final String os = System.getProperty("os.name").toLowerCase();
            final String[] command = StringUtils.startsWithIgnoreCase(os, "win") ?
                    WINDOWS_COMMAND : UNIX_COMMAND;
            final ProcessBuilder builder = new ProcessBuilder(command);
            final Process process = builder.start();
            try (final InputStream inputStream = process.getInputStream();
                 final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                 final BufferedReader br = new BufferedReader(inputStreamReader)) {
                String tmp;
                while ((tmp = br.readLine()) != null) {
                    ret.append(tmp);
                }
            }
            if (process.waitFor() != 0) {
                throw new IOException("Command execute fail.");
            }
        } catch (IOException | InterruptedException ex) {
            return getRawMacWithNetworkInterface();
        }

        return ret.toString();
    }

    private static String getRawMacWithNetworkInterface() {
        final List<String> macSet = new ArrayList<>();
        try {
            final Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                final NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.getHardwareAddress() != null) {
                    final byte[] mac = networkInterface.getHardwareAddress();
                    // Refers https://www.mkyong.com/java/how-to-get-mac-address-in-java/
                    final StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    }
                    macSet.add(sb.toString());
                }
            }
        } catch (SocketException e) {
            return StringUtils.EMPTY;
        }
        Collections.sort(macSet);

        return StringUtils.join(macSet, " ");
    }

    private static String hash(String mac) {
        if (mac == null || mac.isEmpty()) {
            return null;
        }

        final String ret;
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-256");
            final byte[] bytes = mac.getBytes("UTF-8");
            md.update(bytes);
            final byte[] bytesAfterDigest = md.digest();
            final StringBuffer sb = new StringBuffer();
            for (int i = 0; i < bytesAfterDigest.length; i++) {
                sb.append(Integer.toString((bytesAfterDigest[i] & 0xff) + 0x100, 16).substring(1));
            }

            ret = sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            return null;
        } catch (UnsupportedEncodingException ex) {
            return null;
        }

        return ret;
    }
}
