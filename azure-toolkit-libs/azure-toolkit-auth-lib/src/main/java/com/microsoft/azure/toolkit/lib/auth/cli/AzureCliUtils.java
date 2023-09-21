/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.cli;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.zafarkhaja.semver.Version;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.proxy.ProxyInfo;
import com.microsoft.azure.toolkit.lib.common.utils.CommandUtils;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.common.utils.Utils.distinctByKey;

public class AzureCliUtils {
    private static final String MIN_VERSION = "2.11.0";

    public static boolean isAppropriateCliInstalled() {
        try {
            final String str = AzureCliUtils.executeAzureCli("az version --output json");
            final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
            };
            final Map<String, Object> result = JsonUtils.fromJson(str, typeRef);
            final String cliVersion = (String) result.get("azure-cli");
            // we require at least azure cli version 2.11.0
            return Version.valueOf(cliVersion).greaterThanOrEqualTo(Version.valueOf(MIN_VERSION));
        } catch (final NullPointerException | NumberFormatException ex) {
            return false;
        }
    }

    public static boolean isSignedIn() {
        try {
            final String str = AzureCliUtils.executeAzureCli("az account show --output json");
            final TypeReference<HashMap<String, Object>> typeRef = new TypeReference<HashMap<String, Object>>() {
            };
            final Map<String, Object> result = JsonUtils.fromJson(str, typeRef);
            final String subscriptionId = (String) result.get("id");
            return StringUtils.isNotBlank(subscriptionId);
        } catch (final Throwable ex) {
            return false;
        }
    }

    @Nonnull
    public static List<Subscription> listSubscriptions() {
        final String jsonString = executeAzureCli("az account list --output json");
        final AzureCliSubscription[] subscriptions = JsonUtils.fromJson(jsonString, AzureCliSubscription[].class);
        return Arrays.stream(subscriptions)
            .filter(s -> StringUtils.isNoneBlank(s.getId(), s.getName()) && "Enabled".equalsIgnoreCase(s.getState()))
            .filter(distinctByKey(t -> StringUtils.lowerCase(t.getId())))
            .collect(Collectors.toList());
    }

    @Nonnull
    public static String executeAzureCli(@Nonnull String command) {
        try {
            final AzureConfiguration config = Azure.az().config();
            final Map<String, String> env = new HashMap<>();
            final ProxyInfo proxy = config.getProxyInfo();
            if (Objects.nonNull(proxy) && StringUtils.isNotBlank(proxy.getSource())) {
                String proxyAuthPrefix = StringUtils.EMPTY;
                if (StringUtils.isNoneBlank(proxy.getUsername(), proxy.getPassword())) {
                    proxyAuthPrefix = proxy.getUsername() + ":" + proxy.getPassword() + "@";
                }
                final String proxyStr = String.format("http://%s%s:%s", proxyAuthPrefix, proxy.getHost(), proxy.getPort());
                env.put("HTTPS_PROXY", proxyStr);
                env.put("HTTP_PROXY", proxyStr);
            }
            return CommandUtils.exec(command, env);
        } catch (final IOException e) {
            throw new AzureToolkitAuthenticationException(
                String.format("execute Azure Cli command '%s' failed due to error: %s.", command, e.getMessage()));
        }
    }
}
