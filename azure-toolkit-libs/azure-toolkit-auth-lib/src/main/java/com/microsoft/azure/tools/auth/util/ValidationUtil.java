/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.util;

import com.microsoft.azure.tools.auth.AuthHelper;
import com.microsoft.azure.tools.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.tools.auth.model.AuthConfiguration;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Objects;

public class ValidationUtil {
    public static AuthConfiguration validateMavenAuthConfiguration(AuthConfiguration config) throws InvalidConfigurationException {
        String tenant = config.getTenant();
        String client = config.getClient();
        String key = config.getKey();
        String certificate = config.getCertificate();
        String environment = config.getEnvironment();
        String errorMessage = null;
        if (StringUtils.isBlank(tenant)) {
            errorMessage = "Cannot find 'tenant'";
        } else if (StringUtils.isBlank(client)) {
            errorMessage = "Cannot find 'client'";
        } else if (StringUtils.isAllBlank(key, certificate)) {
            errorMessage = "Cannot find either 'key' or 'certificate'";
        } else if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(certificate)) {
            errorMessage = "It is wrong to specify both 'key' and 'certificate'";
        } else if (StringUtils.isNotBlank(environment) && !AuthHelper.validateEnvironment(environment)) {
            errorMessage = String.format("Invalid environment string '%s'", environment);
        }
        if (Objects.nonNull(errorMessage)) {
            throw new InvalidConfigurationException(errorMessage);
        }
        validateHttpProxy(config.getHttpProxyHost(), config.getHttpProxyPort());
        return config;
    }

    public static void validateHttpProxy(String httpProxyHost, String httpProxyPort) throws InvalidConfigurationException {
        if ((StringUtils.isNotBlank(httpProxyHost) && StringUtils.isBlank(httpProxyPort)) ||
                (StringUtils.isBlank(httpProxyHost) && StringUtils.isNotBlank(httpProxyPort))) {
            throw new InvalidConfigurationException("if you want to use proxy, 'httpProxyHost' and 'httpProxyPort' must both be set");
        } else if (StringUtils.isNotBlank(httpProxyPort)) {
            if (!StringUtils.isNumeric(httpProxyPort)) {
                throw new InvalidConfigurationException(String.format("Invalid integer number for httpProxyPort: '%s'", httpProxyPort));
            } else if (NumberUtils.toInt(httpProxyPort) <= 0 || NumberUtils.toInt(httpProxyPort) > 65535) {
                throw new InvalidConfigurationException(
                        String.format("Invalid range of httpProxyPort: '%s', it should be a number between %d and %d", httpProxyPort, 1, 65535));
            }
        }

    }
}
