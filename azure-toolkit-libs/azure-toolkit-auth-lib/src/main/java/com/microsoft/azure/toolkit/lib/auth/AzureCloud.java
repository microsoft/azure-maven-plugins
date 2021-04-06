/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.management.AzureEnvironment;
import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureService;
import com.microsoft.azure.toolkit.lib.auth.util.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class AzureCloud implements AzureService {
    public List<AzureEnvironment> list() {
        return AzureEnvironment.knownEnvironments();
    }

    public List<String> listNames() {
        return AzureEnvironment.knownEnvironments().stream().map(AzureEnvironmentUtils::getCloudNameForAzureCli).
                collect(Collectors.toList());
    }

    public AzureEnvironment get() {
        final String cloud = Azure.az().config().getCloud();
        return StringUtils.isNotBlank(cloud) ? AzureEnvironmentUtils.stringToAzureEnvironment(cloud) : null;
    }

    public String getName() {
        final AzureEnvironment env = get();
        return env == null ? null : AzureEnvironmentUtils.getCloudNameForAzureCli(env);
    }

    public AzureEnvironment getOrDefault() {
        return ObjectUtils.firstNonNull(get(), AzureEnvironment.AZURE);
    }

    public AzureCloud set(AzureEnvironment environment) {
        Objects.requireNonNull(environment, "Azure environment shall not be null.");
        final String cloud = AzureEnvironmentUtils.getCloudNameForAzureCli(environment);
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
