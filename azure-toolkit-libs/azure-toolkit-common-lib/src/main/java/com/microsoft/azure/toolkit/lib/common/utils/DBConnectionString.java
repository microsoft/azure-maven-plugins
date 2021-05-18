/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.Builder;

@Builder
public class DBConnectionString {

    private String url;
    private String username;
    @Builder.Default
    private String driverClassName = "com.mysql.cj.jdbc.Driver";
    @Builder.Default
    private String password = "${your_password}";
    @Builder.Default
    private Type type = Type.SPRING;

    private static final String PROPERTY_PATTERN_SPRING =
            "spring.datasource.driver-class-name=%s" + System.lineSeparator()
            + "spring.datasource.url=%s" + System.lineSeparator()
            + "spring.datasource.username=%s" + System.lineSeparator()
            + "spring.datasource.password=%s";
    private static final String PROPERTY_PATTERN_JDBC =
            "String url =\"%s\";" + System.lineSeparator()
            + "myDbConn = DriverManager.getConnection(url, \"%s\", \"%s\");";

    public String asString() {
        if (type == Type.SPRING) {
            return String.format(PROPERTY_PATTERN_SPRING, driverClassName, url, username, password);
        } else if (type == Type.JDBC) {
            return String.format(PROPERTY_PATTERN_JDBC, url, username, password);
        } else {
            throw new AzureToolkitRuntimeException("current type is not supported. type: %s", type.name());
        }
    }

    public enum Type {
        SPRING,
        JDBC
    }
}
