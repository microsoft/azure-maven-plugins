/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.cli;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.SimpleTokenCache;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.management.AzureEnvironment;
import com.azure.identity.implementation.util.ScopeUtil;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.auth.Account;
import com.microsoft.azure.toolkit.lib.auth.AuthConfiguration;
import com.microsoft.azure.toolkit.lib.auth.AuthType;
import com.microsoft.azure.toolkit.lib.auth.AzureCloud;
import com.microsoft.azure.toolkit.lib.auth.AzureEnvironmentUtils;
import com.microsoft.azure.toolkit.lib.auth.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class AzureCliAccount extends Account {
    private static final String CLOUD_SHELL_ENV_KEY = "ACC_CLOUD";
    private final AuthType type = AuthType.AZURE_CLI;
    private String username;

    public AzureCliAccount(AuthConfiguration config) {
        super(config);
    }

    @Nonnull
    @Override
    protected List<Subscription> loadSubscriptions() {
        final List<AzureCliSubscription> cliSubs = AzureCliUtils.listSubscriptions();
        if (cliSubs.isEmpty()) {
            throw new AzureToolkitAuthenticationException("Cannot find any subscriptions in current account.");
        }
        return new ArrayList<>(cliSubs);
    }

    @Override
    protected void setupAfterLogin(TokenCredential defaultTokenCredential) {
        List<Subscription> subscriptions = this.getSubscriptions();
        final AzureCliSubscription defaultSub = (AzureCliSubscription) subscriptions.stream().filter(Subscription::isSelected).findFirst().orElse(subscriptions.get(0));
        final AzureEnvironment configuredEnv = Azure.az(AzureCloud.class).get();
        if (configuredEnv != null && defaultSub.getEnvironment() != configuredEnv) {
            throw new AzureToolkitAuthenticationException(
                String.format("The azure cloud from azure cli '%s' doesn't match with your auth configuration, " +
                        "you can change it by executing 'az cloud set --name=%s' command to change the cloud in azure cli.",
                    AzureEnvironmentUtils.getCloudName(defaultSub.getEnvironment()),
                    AzureEnvironmentUtils.getCloudName(configuredEnv)));
        }
        this.username = defaultSub.getEmail();
    }

    @Nonnull
    @Override
    protected TokenCredential buildDefaultTokenCredential() {
        final String tenantId = Optional.of(this.getConfig()).map(AuthConfiguration::getTenant).orElse(null);
        return new AzureCliTokenCredential(tenantId);
    }

    @Override
    public boolean checkAvailable() {
        return this.getManagementToken().isPresent();
    }

    @AllArgsConstructor
    static class AzureCliTokenCredential implements TokenCredential {
        private static final String CLI_GET_ACCESS_TOKEN_CMD = "az account get-access-token --resource %s %s --output json";
        private final Map<String, SimpleTokenCache> tenantResourceTokenCache = new ConcurrentHashMap<>();
        private final String tenantId;

        @Override
        public Mono<AccessToken> getToken(TokenRequestContext request) {
            final String tId = StringUtils.firstNonBlank(request.getTenantId(), this.tenantId);
            final String scopes = ScopeUtil.scopesToResource(request.getScopes());
            final String key = String.format("%s:%s", tId, scopes);
            return tenantResourceTokenCache.computeIfAbsent(key, k -> new SimpleTokenCache(() -> {
                final String azCommand = String.format(CLI_GET_ACCESS_TOKEN_CMD, scopes, (StringUtils.isBlank(tId) || isInCloudShell()) ? "" : (" -t " + tId));
                final JsonObject result = JsonUtils.getGson().fromJson(AzureCliUtils.executeAzureCli(azCommand), JsonObject.class);

                // com.azure.identity.implementation.IdentityClient.authenticateWithAzureCli
                final String accessToken = result.get("accessToken").getAsString();
                final OffsetDateTime expiresDateTime = Optional.ofNullable(result.get("expiresOn"))
                    .filter(jsonElement -> !jsonElement.isJsonNull())
                    .map(JsonElement::getAsString)
                    .map(value -> value.substring(0, value.indexOf(".")))
                    .map(value -> String.join("T", value.split(" ")))
                    .map(value -> LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                        .atZone(ZoneId.systemDefault()).toOffsetDateTime()
                        .withOffsetSameInstant(ZoneOffset.UTC))
                    .orElse(OffsetDateTime.MAX);
                return Mono.just(new AccessToken(accessToken, expiresDateTime));
            })).getToken();
        }
    }

    static boolean isInCloudShell() {
        return StringUtils.isNotBlank(System.getenv(CLOUD_SHELL_ENV_KEY));
    }
}
