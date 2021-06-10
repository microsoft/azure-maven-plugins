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

public abstract class JdbcUrl {

    private static final int MYSQL_DEFAULT_PORT = 3306;
    private static final int SQL_SERVER_DEFAULT_PORT = 1433;

    protected final URIBuilder uri;
    private String username;
    private String password;

    private JdbcUrl(String url) {
        Preconditions.checkArgument(StringUtils.startsWith(url, "jdbc:"), "invalid jdbc url.");
        try {
            this.uri = new URIBuilder(url.substring(5));
        } catch (final URISyntaxException e) {
            throw new AzureToolkitRuntimeException("invalid jdbc url: %s", url);
        }
    }

    public static JdbcUrl from(String connectionString) {
        if (StringUtils.startsWith(connectionString, "jdbc:mysql:")) {
            return new MySQLJdbcUrl(connectionString);
        } else if (StringUtils.startsWith(connectionString, "jdbc:sqlserver:")) {
            return new SQLServerJdbcUrl(connectionString);
        }
        throw new AzureToolkitRuntimeException("Unsupported jdbc url: %s", connectionString);
    }

    public static JdbcUrl mysql(String serverHost, String database) {
        return new MySQLJdbcUrl(String.format("jdbc:mysql://%s:%s/%s?serverTimezone=UTC&useSSL=true&requireSSL=false",
            encode(serverHost), MYSQL_DEFAULT_PORT, encode(database)));
    }

    public static JdbcUrl mysql(String serverHost) {
        return new MySQLJdbcUrl(String.format("jdbc:mysql://%s:%s?serverTimezone=UTC&useSSL=true&requireSSL=false",
            encode(serverHost), MYSQL_DEFAULT_PORT));
    }

    public static JdbcUrl sqlserver(String serverHost, String database) {
        return new SQLServerJdbcUrl(String.format("jdbc:sqlserver://%s:%s;encrypt=true;trustServerCertificate=false;loginTimeout=30;database=%s;",
            encode(serverHost), SQL_SERVER_DEFAULT_PORT, encode(database)));
    }

    public static JdbcUrl sqlserver(String serverHost) {
        return new SQLServerJdbcUrl(String.format("jdbc:sqlserver://%s:%s;encrypt=true;trustServerCertificate=false;loginTimeout=30;",
            encode(serverHost), SQL_SERVER_DEFAULT_PORT));
    }

    abstract int getDefaultPort();

    public int getPort() {
        if (this.uri.getPort() >= 0) {
            return this.uri.getPort();
        }
        // default port
        return getDefaultPort();
    }

    public String getServerHost() {
        return decode(this.uri.getHost());
    }

    public String getDatabase() {
        final String path = this.uri.getPath();
        return decode(StringUtils.startsWith(path, "/") ? path.substring(1) : path);
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
        this.uri.setPath("/" + database);
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

    @Override
    public String toString() {
        String url = "jdbc:" + uri.toString();
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

    private static class MySQLJdbcUrl extends JdbcUrl {

        private MySQLJdbcUrl(String url) {
            super(url);
        }

        @Override
        int getDefaultPort() {
            return MYSQL_DEFAULT_PORT;
        }

    }

    private static class SQLServerJdbcUrl extends JdbcUrl {

        private SQLServerJdbcUrl(String url) {
            super(StringUtils.replaceOnce(url, ";", "?").replaceAll(";", "&"));
        }

        @Override
        int getDefaultPort() {
            return SQL_SERVER_DEFAULT_PORT;
        }

        @Override
        public JdbcUrl setDatabase(String database) {
            this.uri.setParameter("database", database);
            return this;
        }

        @Override
        public String getDatabase() {
            return this.uri.getQueryParams().stream().filter(e -> StringUtils.equals(e.getName(), "database"))
                    .map(NameValuePair::getValue).findFirst().orElse(null);
        }

        @Override
        public String toString() {
            String url = "jdbc:" + StringUtils.replaceOnce(uri.toString(), "?", ";").replaceAll("&", ";");
            return JdbcUrl.decode(url);
        }
    }

}
