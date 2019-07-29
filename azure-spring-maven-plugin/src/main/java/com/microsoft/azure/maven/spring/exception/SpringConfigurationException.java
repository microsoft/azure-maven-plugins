/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.exception;

public class SpringConfigurationException extends Exception {

    public SpringConfigurationException(String message) {
        super(message);
    }

    public SpringConfigurationException(String message, Throwable cause) {
        super(message, cause);
    }

}
