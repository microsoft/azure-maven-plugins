/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.appservice;

import org.apache.commons.lang3.StringUtils;

import java.util.Locale;

/**
 * Values of &lt;configurationSource&gt; property.
 * If it is null or empty, PARENT will be used as default.
 * If set the value NEW, a brand new deployment slot without any configuration will be created.
 * If set the value PARENT, will create a new deployment slot and copy the configuration from parent.
 * Any other value will be treated as the deployment slot name to copy configuration from during creation.
 */
public enum ConfigurationSourceType {
    NEW,
    PARENT,
    OTHERS;

    public static ConfigurationSourceType fromString(String input) {
        if (StringUtils.isEmpty(input)) {
            return PARENT;
        }
        switch (input.toUpperCase(Locale.ENGLISH)) {
            case "NEW":
                return NEW;
            case "PARENT":
                return PARENT;
            default:
                return OTHERS;
        }
    }
}
