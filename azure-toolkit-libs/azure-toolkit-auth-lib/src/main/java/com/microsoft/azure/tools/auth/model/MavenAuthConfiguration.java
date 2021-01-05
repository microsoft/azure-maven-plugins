/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.auth.model;

import com.microsoft.azure.tools.auth.AuthHelper;
import com.microsoft.azure.tools.auth.exception.InvalidConfigurationException;
import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.Objects;

@Setter
@Getter
public class MavenAuthConfiguration {
    private String authType;
    private String client;
    private String tenant;
    private String key;
    private String certificate;
    private String certificatePassword;
    private String environment;
    private String serverId;
    private String httpProxyHost;
    private String httpProxyPort;

    public void validate() throws InvalidConfigurationException {
        String messagePrefix = null;
        if (StringUtils.isBlank(tenant)) {
            messagePrefix = "Cannot find 'tenant'";
        } else if (StringUtils.isBlank(client)) {
            messagePrefix = "Cannot find 'client'";
        } else if (StringUtils.isAllBlank(key, certificate)) {
            messagePrefix = "Cannot find either 'key' or 'certificate'";
        } else if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(certificate)) {
            messagePrefix = "It is wrong to specify both 'key' and 'certificate'";
        } else if (StringUtils.isNotBlank(environment) && !AuthHelper.validateEnvironment(environment)) {
            messagePrefix = "Invalid environment string '%s'";
        } else if (StringUtils.isNotBlank(httpProxyHost) && StringUtils.isBlank(httpProxyPort)) {
            messagePrefix = "if you want to use proxy, 'httpProxyHost' and 'httpProxyPort' must both be set";
        } else if (StringUtils.isNotBlank(httpProxyPort)) {
            if (!StringUtils.isNumeric(httpProxyPort)) {
                messagePrefix = String.format("Invalid integer number for httpProxyPort: '%s'", httpProxyPort);
            } else if (NumberUtils.toInt(httpProxyPort) <= 0 || NumberUtils.toInt(httpProxyPort) > 65535) {
                messagePrefix = String.format("Invalid range of httpProxyPort: '%s', it should be a number between %d and %d", httpProxyPort, 1, 65535);
            }
        }

        final String messagePostfix = StringUtils.isNotBlank(serverId) ? (" in <server>: \"" + serverId + "\" at maven settings.xml.")
                : " in <auth> configuration.";
        if (Objects.nonNull(messagePrefix)) {
            throw new InvalidConfigurationException(messagePrefix + messagePostfix);
        }
    }

    public MavenAuthConfiguration validateAndReturn() throws InvalidConfigurationException {
        validate();
        return this;
    }
}
