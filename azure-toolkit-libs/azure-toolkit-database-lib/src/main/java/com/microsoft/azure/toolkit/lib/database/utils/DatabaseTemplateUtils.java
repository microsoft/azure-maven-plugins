/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.database.utils;

import com.microsoft.azure.toolkit.lib.database.JdbcUrl;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;

@Builder
public class DatabaseTemplateUtils {

    private static final String DEFAULT_MYSQL_DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
    private static final String DEFAULT_SQLSERVER_DRIVER_CLASS_NAME = "com.microsoft.sqlserver.jdbc.SQLServerDriver";
    private static final String DEFAULT_PASSWORD = "${your_password}";
    private static final String DEFAULT_USERNAME = "${your_username}";

    private static final String PROPERTY_PATTERN_SPRING =
            "spring.datasource.driver-class-name=%s" + System.lineSeparator()
            + "spring.datasource.url=%s" + System.lineSeparator()
            + "spring.datasource.username=%s" + System.lineSeparator()
            + "spring.datasource.password=%s";

    private static final String PROPERTY_PATTERN_JDBC =
            "String url =\"%s\";" + System.lineSeparator()
            + "Connection myDbConnection = DriverManager.getConnection(url, \"%s\", \"%s\");";

    public static String toSpringTemplate(@Nonnull JdbcUrl jdbcUrl, String driverClassName) {
        String driverClass = StringUtils.firstNonBlank(driverClassName, DEFAULT_MYSQL_DRIVER_CLASS_NAME);
        String username = StringUtils.firstNonBlank(jdbcUrl.getUsername(), DEFAULT_USERNAME);
        String password = StringUtils.firstNonBlank(jdbcUrl.getPassword(), DEFAULT_PASSWORD);
        String url = jdbcUrl.toString();
        return String.format(PROPERTY_PATTERN_SPRING, driverClass, url, username, password);
    }

    public static String toJdbcTemplate(@Nonnull JdbcUrl jdbcUrl) {
        String username = StringUtils.firstNonBlank(jdbcUrl.getUsername(), DEFAULT_USERNAME);
        String password = StringUtils.firstNonBlank(jdbcUrl.getPassword(), DEFAULT_PASSWORD);
        String url = jdbcUrl.toString();
        return String.format(PROPERTY_PATTERN_JDBC, url, username, password);
    }

}
