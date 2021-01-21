/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.util;

import com.microsoft.azure.tools.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.tools.auth.model.AuthConfiguration;
import org.apache.commons.lang3.StringUtils;

import java.util.Objects;

public class ValidationUtil {

    private static final int MAX_PORT_NUMBER = 65535;

    public static AuthConfiguration validateMavenAuthConfiguration(AuthConfiguration config) throws InvalidConfigurationException {
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
        validateHttpProxy(config.getHttpProxyHost(), config.getHttpProxyPort());
        return config;
    }

    public static void validateHttpProxy(String httpProxyHost, Integer httpProxyPort) throws InvalidConfigurationException {
        String httpProxyPortStr = Objects.toString(httpProxyHost, null);
        if (StringUtils.isAllBlank(httpProxyHost, httpProxyPortStr)) {
            return;
        }
        if (StringUtils.isAnyBlank(httpProxyHost, httpProxyPortStr)) {
            throw new InvalidConfigurationException("if you want to use proxy, 'httpProxyHost' and 'httpProxyPort' must both be set");
        }
        if (httpProxyPort <= 0 || httpProxyPort > MAX_PORT_NUMBER) {
            throw new InvalidConfigurationException(
                    String.format("Invalid range of httpProxyPort: '%s', it should be a number between %d and %d", httpProxyPort, 1, MAX_PORT_NUMBER));
        }
    }
}
