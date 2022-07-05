/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.management.AzureEnvironment;
import com.azure.core.util.Configuration;
import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.lib.AzService;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AzureCloud implements AzService {
    public List<AzureEnvironment> list() {
        return AzureEnvironment.knownEnvironments();
    }

    public List<String> listNames() {
        return AzureEnvironment.knownEnvironments().stream().map(AzureEnvironmentUtils::getCloudName).
            collect(Collectors.toList());
    }

    public AzureEnvironment get() {
        final String cloud = Azure.az().config().getCloud();
        return StringUtils.isNotBlank(cloud) ? AzureEnvironmentUtils.stringToAzureEnvironment(cloud) : null;
    }

    public String getName() {
        final AzureEnvironment env = get();
        return env == null ? null : AzureEnvironmentUtils.getCloudName(env);
    }

    @Override
    public void refresh() {
        // do nothing
    }

    public AzureEnvironment getOrDefault() {
        return ObjectUtils.firstNonNull(get(), AzureEnvironment.AZURE);
    }

    public AzureCloud set(AzureEnvironment environment) {
        Objects.requireNonNull(environment, "Azure environment shall not be null.");
        // change the default azure env after it is initialized in azure identity
        // see code at
        // https://github.com/Azure/azure-sdk-for-java/blob/32f8f7ca8b44035b2e5520c5e10455f42500a778/sdk/identity/azure-identity/
        // src/main/java/com/azure/identity/implementation/IdentityClientOptions.java#L42
        Configuration.getGlobalConfiguration().put(Configuration.PROPERTY_AZURE_AUTHORITY_HOST, environment.getActiveDirectoryEndpoint());
        final com.microsoft.azure.toolkit.lib.AzureConfiguration az = com.microsoft.azure.toolkit.lib.Azure.az().config();
        if (StringUtils.isNotBlank(az.getProxySource())) {
            String proxyAuthPrefix = StringUtils.EMPTY;
            if (StringUtils.isNoneBlank(az.getProxyUsername(), az.getProxyPassword())) {
                proxyAuthPrefix = az.getProxyUsername() + ":" + az.getProxyPassword() + "@";
            }
            final String proxy = String.format("http://%s%s:%d", proxyAuthPrefix, az.getHttpProxyHost(), az.getHttpProxyPort());
            Configuration.getGlobalConfiguration().put(Configuration.PROPERTY_HTTP_PROXY, proxy);
            Configuration.getGlobalConfiguration().put(Configuration.PROPERTY_HTTPS_PROXY, proxy);
        }
        final String cloud = AzureEnvironmentUtils.getCloudName(environment);
        Azure.az().config().setCloud(cloud);
        return this;
    }

    public AzureCloud setByName(String name) {
        Preconditions.checkArgument(StringUtils.isNotBlank(name), "Azure environment name shall not be null.");
        return set(AzureEnvironmentUtils.stringToAzureEnvironment(name));
    }

    public List<Subscription> getSubscriptions() {
        throw new UnsupportedOperationException("Cannot get subscriptions from service: AzureCloud.");
    }
}
