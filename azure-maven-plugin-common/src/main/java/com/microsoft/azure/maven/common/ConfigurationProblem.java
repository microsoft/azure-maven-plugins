/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.common;

public class ConfigurationProblem {

    /**
     * The different severity levels for a problem, in decreasing order.
     */
    public static enum Severity {
        ERROR,
        WARNING
    }

    private final String key;
    private final String value;
    private final String errorMessage;
    private final Severity severity;

    public ConfigurationProblem(String key, String value, String errorMessage, Severity severity) {
        this.key = key;
        this.value = value;
        this.errorMessage = errorMessage;
        this.severity = severity;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public Severity getSeverity() {
        return severity;
    }
}
