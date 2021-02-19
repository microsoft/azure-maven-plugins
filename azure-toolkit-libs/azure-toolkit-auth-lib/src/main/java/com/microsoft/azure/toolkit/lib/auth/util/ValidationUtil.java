/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.util;

import com.microsoft.azure.toolkit.lib.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.toolkit.lib.auth.model.AuthConfiguration;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class ValidationUtil {
    public static AuthConfiguration validateAuthConfiguration(AuthConfiguration config) throws InvalidConfigurationException {
        String tenant = config.getTenant();
        String client = config.getClient();
        String key = config.getKey();
        String certificate = config.getCertificate();
        String errorMessage = null;
        if (StringUtils.isBlank(tenant)) {
            errorMessage = "Cannot find 'tenant'";
        } else if (StringUtils.isBlank(client)) {
            errorMessage = "Cannot find 'client'";
        } else if (StringUtils.isAllBlank(key, certificate)) {
            errorMessage = "Cannot find either 'key' or 'certificate'";
        } else if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(certificate)) {
            errorMessage = "It is wrong to specify both 'key' and 'certificate'";
        }
        if (Objects.nonNull(errorMessage)) {
            throw new InvalidConfigurationException(errorMessage);
        }
        return config;
    }
}
