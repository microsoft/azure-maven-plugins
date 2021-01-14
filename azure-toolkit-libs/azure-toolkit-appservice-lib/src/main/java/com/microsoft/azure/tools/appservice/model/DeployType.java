/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.appservice.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum DeployType {
    WAR("war"),
    JAR("jar"),
    EAR("ear"),
    JAR_LIB("lib"),
    STATIC("static"),
    SCRIPT_STARTUP("startup"),
    ZIP("zip");

    private String value;
}
