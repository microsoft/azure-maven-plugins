/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.database;

import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URIBuilder;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Objects;

public abstract class JdbcUrl {

    private static final int MYSQL_DEFAULT_PORT = 3306;
    private static final int POSTGRE_SQL_DEFAULT_PORT = 5432;
    private static final int SQL_SERVER_DEFAULT_PORT = 1433;

    @Nonnull
    protected final URIBuilder uri;
    private String username;
    private String password;

    private JdbcUrl(@Nonnull String url) {
        Preconditions.checkArgument(StringUtils.startsWith(url, "jdbc:"), "invalid jdbc url.");
        try {
            this.uri = new URIBuilder(url.substring(5));
        } catch (final URISyntaxException e) {
            throw new AzureToolkitRuntimeException("invalid jdbc url: %s", url);
        }
    }

    @Nonnull
    public static JdbcUrl from(@Nonnull String connectionString) {
        if (StringUtils.startsWith(connectionString, "jdbc:mysql:")) {
            return new MySQLJdbcUrl(connectionString);
        } else if (StringUtils.startsWith(connectionString, "jdbc:sqlserver:")) {
            return new SQLServerJdbcUrl(connectionString);
        } else if (StringUtils.startsWith(connectionString, "jdbc:postgresql:")) {
            return new PostgreSQLJdbcUrl(connectionString);
        }
        throw new AzureToolkitRuntimeException("Unsupported jdbc url: %s", connectionString);
    }

    @Nonnull
    public static JdbcUrl mysql(@Nonnull String serverHost, @Nonnull String database) {
        return new MySQLJdbcUrl(String.format("jdbc:mysql://%s:%s/%s?serverTimezone=UTC&useSSL=true&requireSSL=false",
            encode(serverHost), MYSQL_DEFAULT_PORT, encode(database)));
    }

    @Nonnull
    public static JdbcUrl mysql(@Nonnull String serverHost) {
        return new MySQLJdbcUrl(String.format("jdbc:mysql://%s:%s?serverTimezone=UTC&useSSL=true&requireSSL=false",
            encode(serverHost), MYSQL_DEFAULT_PORT));
    }

    @Nonnull
    public static JdbcUrl postgre(@Nonnull String serverHost, @Nonnull String database) {
        // Postgre database name is required;
        return new PostgreSQLJdbcUrl(String.format("jdbc:postgresql://%s:%s/%s?ssl=true&sslmode=require",
            encode(serverHost), POSTGRE_SQL_DEFAULT_PORT, encode(database)));
    }

    @Nonnull
    public static JdbcUrl sqlserver(@Nonnull String serverHost, @Nonnull String database) {
        return new SQLServerJdbcUrl(String.format("jdbc:sqlserver://%s:%s;encrypt=true;trustServerCertificate=false;loginTimeout=30;database=%s;",
            encode(serverHost), SQL_SERVER_DEFAULT_PORT, encode(database)));
    }

    @Nonnull
    public static JdbcUrl sqlserver(@Nonnull String serverHost) {
        return new SQLServerJdbcUrl(String.format("jdbc:sqlserver://%s:%s;encrypt=true;trustServerCertificate=false;loginTimeout=30;",
            encode(serverHost), SQL_SERVER_DEFAULT_PORT));
    }

    abstract int getDefaultPort();

    @Nonnull
    public abstract String getDefaultDriverClass();

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

    @Nullable
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

    @Nonnull
    public JdbcUrl setServerHost(String serverHost) {
        this.uri.setHost(serverHost);
        return this;
    }

    @Nonnull
    public JdbcUrl setDatabase(String database) {
        this.uri.setPath("/" + database);
        return this;
    }

    @Nonnull
    public JdbcUrl setUsername(String username) {
        this.username = username;
        return this;
    }

    @Nonnull
    public JdbcUrl setPassword(String password) {
        this.password = password;
        return this;
    }

    @Nonnull
    public JdbcUrl setPort(int port) {
        this.uri.setPort(port);
        return this;
    }

    @Override
    public String toString() {
        String url = "jdbc:" + uri.toString();
        return decode(url);
    }

    private static String encode(@Nonnull String context) {
        try {
            return URLEncoder.encode(context, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AzureToolkitRuntimeException(e.getMessage());
        }
    }

    private static String decode(@Nonnull String context) {
        try {
            return URLDecoder.decode(context, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new AzureToolkitRuntimeException(e.getMessage());
        }
    }

    @Override
    public boolean equals(@Nullable Object o) {
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

        private MySQLJdbcUrl(@Nonnull String url) {
            super(url);
        }

        @Override
        int getDefaultPort() {
            return MYSQL_DEFAULT_PORT;
        }

        @Nonnull
        @Override
        public String getDefaultDriverClass() {
            return "com.mysql.cj.jdbc.Driver";
        }
    }

    private static class PostgreSQLJdbcUrl extends JdbcUrl {

        private PostgreSQLJdbcUrl(@Nonnull String url) {
            super(url);
        }

        @Override
        int getDefaultPort() {
            return POSTGRE_SQL_DEFAULT_PORT;
        }

        @Nonnull
        @Override
        public String getDefaultDriverClass() {
            return "org.postgresql.Driver";
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

        @Nonnull
        @Override
        public String getDefaultDriverClass() {
            return "com.microsoft.sqlserver.jdbc.SQLServerDriver";
        }

        @Nonnull
        @Override
        public JdbcUrl setDatabase(String database) {
            this.uri.setParameter("database", database);
            return this;
        }

        @Nullable
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
