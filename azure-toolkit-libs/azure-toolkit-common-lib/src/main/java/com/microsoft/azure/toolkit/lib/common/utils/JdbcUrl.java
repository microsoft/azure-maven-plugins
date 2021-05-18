/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils;

import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Objects;

public class JdbcUrl {

    private final URIBuilder uri;

    private JdbcUrl(String url) {
        Preconditions.checkArgument(StringUtils.startsWith(url, "jdbc:"), "invalid jdbc url.");
        String convertedUrl = url;
        if (StringUtils.startsWith(url, "jdbc:sqlserver:")) {
            convertedUrl = StringUtils.replaceOnce(url, ";", "?").replaceAll(";", "&");
        }
        try {
            this.uri = new URIBuilder(convertedUrl.substring(5));
        } catch (final URISyntaxException e) {
            throw new AzureToolkitRuntimeException("invalid jdbc url: %s", url);
        }
    }

    public static JdbcUrl from(String connectionString) {
        Preconditions.checkArgument(StringUtils.startsWith(connectionString, "jdbc:"), "invalid jdbc url.");
        return new JdbcUrl(connectionString);
    }

    public static JdbcUrl mysql(String serverHost, String database) {
        return new JdbcUrl(String.format("jdbc:mysql://%s:3306/%s?serverTimezone=UTC&useSSL=true&requireSSL=false", encode(serverHost), encode(database)));
    }

    public static JdbcUrl mysql(String serverHost) {
        return new JdbcUrl(String.format("jdbc:mysql://%s:3306?serverTimezone=UTC&useSSL=true&requireSSL=false", encode(serverHost)));
    }

    public static JdbcUrl sqlserver(String serverHost, String database) {
        return new JdbcUrl(String.format(
            "jdbc:sqlserver://%s:1433;encrypt=true;trustServerCertificate=false;loginTimeout=30;database=%s;", encode(serverHost), encode(database)));
    }

    public static JdbcUrl sqlserver(String serverHost) {
        return new JdbcUrl(String.format("jdbc:sqlserver://%s:1433;encrypt=true;trustServerCertificate=false;loginTimeout=30;", encode(serverHost)));
    }

    public int getPort() {
        if (this.uri.getPort() >= 0) {
            return this.uri.getPort();
        }
        // default port
        if (StringUtils.equals(this.uri.getScheme(), "mysql")) {
            return 3306;
        } else if (StringUtils.equals(this.uri.getScheme(), "sqlserver")) {
            return 1433;
        }
        throw new AzureToolkitRuntimeException("unknown jdbc url scheme: %s", this.uri.getScheme());
    }

    public String getServerHost() {
        return decode(this.uri.getHost());
    }

    public String getDatabase() {
        final String path = this.uri.getPath();
        return decode(StringUtils.startsWith(path, "/") ? path.substring(1) : path);
    }

    @Override
    public String toString() {
        String url = "jdbc:" + uri.toString();
        if (StringUtils.equals(uri.getScheme(), "sqlserver")) {
            url = "jdbc:" + StringUtils.replaceOnce(uri.toString(), "?", ";").replaceAll("&", ";");
        }
        return decode(url);
    }

    private static String encode(String context) {
        try {
            return URLEncoder.encode(context, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AzureToolkitRuntimeException(e.getMessage());
        }
    }

    private static String decode(String context) {
        try {
            return URLDecoder.decode(context, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AzureToolkitRuntimeException(e.getMessage());
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        final JdbcUrl jdbcUrl = (JdbcUrl) o;
        return Objects.equals(uri.toString(), jdbcUrl.uri.toString());
    }

    @Override
    public int hashCode() {
        return Objects.hash(uri.toString());
    }

}
