/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.database;

import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Objects;

public class JdbcUrl {

    private static final int MYSQL_DEFAULT_PORT = 3306;
    private static final int SQLSERVER_DEFAULT_PORT = 1433;

    private final URIBuilder uri;
    private String username;
    private String password;

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
        return new JdbcUrl(connectionString);
    }

    public static JdbcUrl mysql(String serverHost, String database) {
        return new JdbcUrl(String.format("jdbc:mysql://%s:%s/%s?serverTimezone=UTC&useSSL=true&requireSSL=false",
            encode(serverHost), MYSQL_DEFAULT_PORT, encode(database)));
    }

    public static JdbcUrl mysql(String serverHost) {
        return new JdbcUrl(String.format("jdbc:mysql://%s:%s?serverTimezone=UTC&useSSL=true&requireSSL=false",
            encode(serverHost), MYSQL_DEFAULT_PORT));
    }

    public static JdbcUrl sqlserver(String serverHost, String database) {
        return new JdbcUrl(String.format("jdbc:sqlserver://%s:%s;encrypt=true;trustServerCertificate=false;loginTimeout=30;database=%s;",
            encode(serverHost), SQLSERVER_DEFAULT_PORT, encode(database)));
    }

    public static JdbcUrl sqlserver(String serverHost) {
        return new JdbcUrl(String.format("jdbc:sqlserver://%s:%s;encrypt=true;trustServerCertificate=false;loginTimeout=30;",
            encode(serverHost), SQLSERVER_DEFAULT_PORT));
    }

    public int getPort() {
        if (this.uri.getPort() >= 0) {
            return this.uri.getPort();
        }
        // default port
        if (StringUtils.equals(this.uri.getScheme(), "mysql")) {
            return MYSQL_DEFAULT_PORT;
        } else if (StringUtils.equals(this.uri.getScheme(), "sqlserver")) {
            return SQLSERVER_DEFAULT_PORT;
        }
        throw new AzureToolkitRuntimeException("unknown jdbc url scheme: %s", this.uri.getScheme());
    }

    public String getServerHost() {
        return decode(this.uri.getHost());
    }

    public String getDatabase() {
        if (StringUtils.equals(this.uri.getScheme(), "sqlserver")) {
            return this.getParameter("database");
        } else {
            final String path = this.uri.getPath();
            return decode(StringUtils.startsWith(path, "/") ? path.substring(1) : path);
        }
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public JdbcUrl setServerHost(String serverHost) {
        this.uri.setHost(serverHost);
        return this;
    }

    public JdbcUrl setDatabase(String database) {
        if (StringUtils.equals(this.uri.getScheme(), "sqlserver")) {
            this.uri.setParameter("database", database);
        } else {
            this.uri.setPath("/" + database);
        }
        return this;
    }

    public JdbcUrl setUsername(String username) {
        this.username = username;
        return this;
    }

    public JdbcUrl setPassword(String password) {
        this.password = password;
        return this;
    }

    public JdbcUrl setPort(int port) {
        this.uri.setPort(port);
        return this;
    }

    private String getParameter(String param) {
        return this.uri.getQueryParams().stream().filter(e -> StringUtils.equals(e.getName(), param)).map(NameValuePair::getValue).findFirst().orElse(null);
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
