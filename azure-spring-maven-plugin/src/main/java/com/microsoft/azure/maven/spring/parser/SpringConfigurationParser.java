/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.parser;

import com.microsoft.azure.maven.spring.AbstractSpringMojo;
import com.microsoft.azure.maven.spring.SpringConfiguration;

public interface SpringConfigurationParser {
    SpringConfiguration parse(AbstractSpringMojo springMojo);
}
