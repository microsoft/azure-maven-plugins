/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.parser;

import com.microsoft.azure.maven.spring.parser.impl.SpringConfigurationParserImpl;

public enum SpringConfigurationParserFactory {

    INSTANCE;

    public SpringConfigurationParser getConfigurationParser() {
        return new SpringConfigurationParserImpl();
    }
}
