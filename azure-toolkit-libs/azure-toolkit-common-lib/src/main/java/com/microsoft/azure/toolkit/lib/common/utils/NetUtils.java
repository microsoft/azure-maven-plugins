/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetUtils {

    private static final Pattern IPADDRESS_PATTERN = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");
    private static final Pattern INTACT_MAC_PATTERN = Pattern.compile("^([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}$");
    private static final Pattern MAC_PATTERN = Pattern.compile("([0-9A-Fa-f]{2}[:-]){5}[0-9A-Fa-f]{2}");
    private static final String[] INVALID_MAC_ADDRESS = {"00:00:00:00:00:00", "ff:ff:ff:ff:ff:ff", "ac:de:48:00:11:22"};
    private static final String[] UNIX_COMMAND = {"/sbin/ifconfig -a || /sbin/ip link"};
    private static final String[] WINDOWS_COMMAND = {"getmac"};

    public static String getPublicIp() {
        String ip = StringUtils.EMPTY;
        try {
            final URL url = new URL("https://ipecho.net/plain");
            final HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), StandardCharsets.UTF_8));
            while ((ip = in.readLine()) != null) {
                if (StringUtils.isNotBlank(ip)) {
                    break;
                }
            }
        } catch (IOException e) {
        }
        return ip;
    }

    public static String getHostName() {
        String hostname = "UNKNOWN_HOST";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Throwable e) {
        }
        return hostname;
    }

    public static String parseIpAddressFromMessage(String message) {
        if (StringUtils.isNotBlank(message)) {
            Matcher matcher = IPADDRESS_PATTERN.matcher(message);
            if (matcher.find()) {
                return matcher.group();
            }
        }
        return StringUtils.EMPTY;
    }

    public static String getMac() {
        final String commandMac = getMacByCommand();
        if (StringUtils.isNotBlank(commandMac)) {
            return commandMac;
        }
        return getMacByNetworkInterface();
    }

    private static String getMacByCommand() {
        List<String> macs = getMacsByCommand();
        return CollectionUtils.isNotEmpty(macs) ? macs.get(0) : StringUtils.EMPTY;
    }

    private static List<String> getMacsByCommand() {
        List<String> macs = new ArrayList<>();
        final StringBuilder ret = new StringBuilder();
        try {
            final String os = System.getProperty("os.name").toLowerCase();
            final String[] command = StringUtils.startsWithIgnoreCase(os, "win") ?
                    WINDOWS_COMMAND : UNIX_COMMAND;
            final ProcessBuilder probuilder = new ProcessBuilder(command);
            final Process process = probuilder.start();
            try (final InputStream inputStream = process.getInputStream();
                 final InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                 final BufferedReader br = new BufferedReader(inputStreamReader)) {
                String tmp;
                while ((tmp = br.readLine()) != null) {
                    ret.append(tmp);
                }
            }
            if (process.waitFor() != 0) {
                throw new IOException(String.format("Command %s execute fail.", String.join(" ", command)));
            }
        } catch (IOException | InterruptedException ex) {
            return macs;
        }
        String commandMacsString = ret.toString();

        Matcher matcher = MAC_PATTERN.matcher(commandMacsString);
        while (matcher.find()) {
            String mac = matcher.group(0);
            if (isValidMac(mac)) {
                macs.add(mac);
            }
        }
        return macs;
    }

    private static String getMacByNetworkInterface() {
        List<String> macs = getMacsByNetworkInterface();
        if (CollectionUtils.isEmpty(macs)) {
            return StringUtils.EMPTY;
        }
        return macs.get(0);
    }

    private static List<String> getMacsByNetworkInterface() {
        List<String> macs = new ArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface networkInterface = interfaces.nextElement();
                if (networkInterface.isLoopback()) {
                    continue;
                }
                if (networkInterface.getHardwareAddress() != null) {
                    byte[] mac = networkInterface.getHardwareAddress();
                    // Refers https://www.mkyong.com/java/how-to-get-mac-address-in-java/
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
                    }
                    String macStr = sb.toString();
                    if (isValidMac(macStr)) {
                        macs.add(macStr);
                    }
                }
            }
        } catch (SocketException e) {
            return macs;
        }
        return macs;
    }

    private static boolean isValidMac(String mac) {
        if (StringUtils.isEmpty(mac)) {
            return false;
        }
        if (!isValidRawMac(mac)) {
            return false;
        }
        final String fixedMac = mac.replaceAll("-", ":");
        return !StringUtils.equalsAnyIgnoreCase(fixedMac, INVALID_MAC_ADDRESS);
    }

    private static boolean isValidRawMac(String raw) {
        return StringUtils.isNotEmpty(raw) && INTACT_MAC_PATTERN.matcher(raw).find();
    }
}
