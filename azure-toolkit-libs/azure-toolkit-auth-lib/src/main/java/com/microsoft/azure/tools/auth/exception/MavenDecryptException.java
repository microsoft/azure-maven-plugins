/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.exception;

public class MavenDecryptException extends AzureLoginException {
    private static final long serialVersionUID = 5207024853556212112L;
    private String propertyName;
    private String propertyValue;

    public MavenDecryptException(String propertyName, String propertyValue, String message) {
        super(message);
        this.propertyName = propertyName;
        this.propertyValue = propertyValue;
    }

    /**
     * @return the property name
     */
    public String getPropertyName() {
        return propertyName;
    }

    /**
     * @return the propertyValue
     */
    public String getPropertyValue() {
        return propertyValue;
    }
}
