/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.util;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.zafarkhaja.semver.Version;
import com.google.gson.JsonObject;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.model.AzureCliSubscription;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.utils.CommandUtils;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.microsoft.azure.toolkit.lib.common.utils.Utils.distinctByKey;

public class AzureCliUtils {
    private static final String MIN_VERSION = "2.11.0";

    public static void ensureMinimumCliVersion() {
        try {
            final JsonObject result = JsonUtils.getGson().fromJson(AzureCliUtils.executeAzureCli("az version --output json"), JsonObject.class);
            final String cliVersion = result.get("azure-cli").getAsString();
            // we require at least azure cli version 2.11.0
            if (Version.valueOf(cliVersion).lessThan(Version.valueOf(MIN_VERSION))) {
                throw new AzureToolkitAuthenticationException(String.format("your Azure Cli version '%s' is too old, " +
                        "you need to upgrade your CLI with 'az upgrade'.", cliVersion));
            }
        } catch (NullPointerException | NumberFormatException ex) {
            throw new AzureToolkitAuthenticationException(
                    String.format("Azure Cli is not ready, " +
                            "please make sure your Azure Cli is installed and signed-in, the detailed error is : %s", ex.getMessage()));
        }
    }

    @Nonnull
    public static List<AzureCliSubscription> listSubscriptions() {
        final String jsonString = executeAzureCli("az account list --output json");
        final ObjectMapper mapper = new ObjectMapper();
        try {
            final AzureCliSubscription[] subscriptions = mapper.readValue(jsonString, AzureCliSubscription[].class);
            return Arrays.stream(subscriptions)
                .filter(s -> StringUtils.isNoneBlank(s.getId(), s.getName()) && s.getState().equalsIgnoreCase("Enabled"))
                .filter(distinctByKey(t -> StringUtils.lowerCase(t.getId())))
                .collect(Collectors.toList());
        } catch (JsonProcessingException e) {
            throw new AzureToolkitRuntimeException("failed to load subscriptions from Azure CLI");
        }
    }

    @Nonnull
    public static String executeAzureCli(@Nonnull String command) {
        try {
            final AzureConfiguration config = Azure.az().config();
            Map<String, String> env = new HashMap<>();
            if (StringUtils.isNotBlank(config.getProxySource())) {
                String proxyAuthPrefix = StringUtils.EMPTY;
                if (StringUtils.isNoneBlank(config.getProxyUsername(), config.getProxyPassword())) {
                    proxyAuthPrefix = config.getProxyUsername() + ":" + config.getProxyPassword() + "@";
                }
                String proxyStr = String.format("http://%s%s:%s", proxyAuthPrefix, config.getHttpProxyHost(), config.getHttpProxyPort());
                env.put("HTTPS_PROXY", proxyStr);
                env.put("HTTP_PROXY", proxyStr);
            }
            return CommandUtils.exec(command, env);
        } catch (IOException e) {
            throw new AzureToolkitAuthenticationException(
                    String.format("execute Azure Cli command '%s' failed due to error: %s.", command, e.getMessage()));
        }
    }
}
