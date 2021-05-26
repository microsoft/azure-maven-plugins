/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils;

import org.apache.commons.lang3.StringUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class NetUtils {

    private static final Pattern IPADDRESS_PATTERN = Pattern.compile("\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}\\.\\d{1,3}");

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

    public static String getMac() {
        try {
            InetAddress ip = InetAddress.getLocalHost();
            NetworkInterface network = NetworkInterface.getByInetAddress(ip);
            byte[] mac = network.getHardwareAddress();
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < mac.length; i++) {
                sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? "-" : ""));
            }
            return sb.toString();
        } catch (Exception e) {
            return "UNKNOWN_MAC";
        }
    }

    public static String getHostName() {
        String hostname = "UNKNOWN_HOST";
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
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
}
