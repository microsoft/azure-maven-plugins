/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.database;

import lombok.Builder;
import org.apache.commons.lang3.StringUtils;

@Builder
public class DatabaseTemplateUtils {

    private static final String DEFAULT_DRIVER_CLASS_NAME = "com.mysql.cj.jdbc.Driver";
    private static final String DEFAULT_PASSWORD = "${your_password}";
    private static final String DEFAULT_USERNAME = "${your_username}";

    private static final String PROPERTY_PATTERN_SPRING =
            "spring.datasource.driver-class-name=%s" + System.lineSeparator()
            + "spring.datasource.url=%s" + System.lineSeparator()
            + "spring.datasource.username=%s" + System.lineSeparator()
            + "spring.datasource.password=%s";

    private static final String PROPERTY_PATTERN_JDBC =
            "String url =\"%s\";" + System.lineSeparator()
            + "myDbConn = DriverManager.getConnection(url, \"%s\", \"%s\");";

    public static String toSpringTemplate(JdbcUrl jdbcUrl, String driverClassName) {
        String driverClass = StringUtils.isNotBlank(driverClassName) ? driverClassName : DEFAULT_DRIVER_CLASS_NAME;
        String url = jdbcUrl.toString();
        String username = StringUtils.isNotBlank(jdbcUrl.getUsername()) ? jdbcUrl.getUsername() : DEFAULT_USERNAME;
        String password = StringUtils.isNotBlank(jdbcUrl.getPassword()) ? jdbcUrl.getPassword() : DEFAULT_PASSWORD;
        return String.format(PROPERTY_PATTERN_SPRING, driverClass, url, username, password);
    }

    public static String toSpringTemplate(JdbcUrl jdbcUrl) {
        String url = jdbcUrl.toString();
        String username = StringUtils.isNotBlank(jdbcUrl.getUsername()) ? jdbcUrl.getUsername() : DEFAULT_USERNAME;
        String password = StringUtils.isNotBlank(jdbcUrl.getPassword()) ? jdbcUrl.getPassword() : DEFAULT_PASSWORD;
        return String.format(PROPERTY_PATTERN_JDBC, url, username, password);
    }

}
