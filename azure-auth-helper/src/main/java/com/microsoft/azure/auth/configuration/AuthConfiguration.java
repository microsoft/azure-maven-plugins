/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth.configuration;

import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.common.ConfigurationProblem;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.List;

import static com.microsoft.azure.common.ConfigurationProblem.Severity.ERROR;
import static com.microsoft.azure.common.ConfigurationProblem.Severity.WARNING;


public class AuthConfiguration {
    private String client;
    private String tenant;
    private String key;
    private String certificate;
    private String certificatePassword;
    private String environment;
    private String serverId;
    private String httpProxyHost;
    private String httpProxyPort;

    public String getClient() {
        return client;
    }

    public void setClient(String client) {
        this.client = client;
    }

    public String getTenant() {
        return tenant;
    }

    public void setTenant(String tenant) {
        this.tenant = tenant;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getCertificate() {
        return certificate;
    }

    public void setCertificate(String certificate) {
        this.certificate = certificate;
    }

    public String getCertificatePassword() {
        return certificatePassword;
    }

    public void setCertificatePassword(String certificatePassword) {
        this.certificatePassword = certificatePassword;
    }

    public String getEnvironment() {
        return environment;
    }

    public void setEnvironment(String environment) {
        this.environment = environment;
    }

    public String getServerId() {
        return serverId;
    }

    public void setServerId(String serverId) {
        this.serverId = serverId;
    }

    public String getHttpProxyHost() {
        return httpProxyHost;
    }

    public void setHttpProxyHost(String httpProxyHost) {
        this.httpProxyHost = httpProxyHost;
    }

    public String getHttpProxyPort() {
        return httpProxyPort;
    }

    public void setHttpProxyPort(String httpProxyPort) {
        this.httpProxyPort = httpProxyPort;
    }

    public List<ConfigurationProblem> validate() {
        final List<ConfigurationProblem> results = new ArrayList<>();
        final String errorMessagePostfix = StringUtils.isNotBlank(serverId) ? "server: " + serverId + " in settings.xml." : "<auth> configuration.";
        // The <serverId> check should be done out of this class.
        if (StringUtils.isBlank(tenant)) {
            results.add(new ConfigurationProblem("tenant", tenant, "Cannot find 'tenant' in " + errorMessagePostfix, ERROR));
        }
        if (StringUtils.isBlank(client)) {
            results.add(new ConfigurationProblem("client", client, "Cannot find 'client' in " + errorMessagePostfix, ERROR));
        }

        if (StringUtils.isBlank(key) && StringUtils.isBlank(certificate)) {
            results.add(new ConfigurationProblem("key", null, "Cannot find either 'key' or 'certificate' in " + errorMessagePostfix, ERROR));
        }

        if (StringUtils.isNotBlank(key) && StringUtils.isNotBlank(certificate)) {
            results.add(new ConfigurationProblem(null, null, "It is illegal to specify both 'key' and 'certificate' in " + errorMessagePostfix, WARNING));
        }

        if (StringUtils.isNotBlank(environment) && !AzureAuthHelper.validateEnvironment(environment)) {
            results.add(new ConfigurationProblem("environment", environment,
                    String.format("Invalid environment string '%s' in " + errorMessagePostfix, environment), ERROR));
        }

        if ((StringUtils.isNotBlank(httpProxyHost) && StringUtils.isBlank(httpProxyPort)) ||
                (StringUtils.isBlank(httpProxyHost) && StringUtils.isNotBlank(httpProxyPort))) {
            results.add(new ConfigurationProblem(null, null, "'httpProxyHost' and 'httpProxyPort' must both be set if you want to use proxy.", ERROR));
        }

        if (StringUtils.isNotBlank(httpProxyPort)) {
            if (!StringUtils.isNumeric(httpProxyPort)) {
                results.add(new ConfigurationProblem("httpProxyPort", httpProxyPort,
                        String.format("Invalid integer number for httpProxyPort: '%s'.", httpProxyPort), ERROR));
            } else if (NumberUtils.toInt(httpProxyPort) <= 0 || NumberUtils.toInt(httpProxyPort) > 65535) {
                results.add(new ConfigurationProblem("httpProxyPort", httpProxyPort,
                        String.format("Invalid range of httpProxyPort: '%s', it should be a number between %d and %d", httpProxyPort, 1, 65535), ERROR));
            }

        }
        return results;
    }

}
