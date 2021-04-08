/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.core.azurecli;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenRequestContext;
import com.azure.identity.implementation.util.ScopeUtil;
import com.google.gson.JsonObject;
import com.microsoft.azure.toolkit.lib.auth.TenantCredential;
import com.microsoft.azure.toolkit.lib.auth.exception.AzureToolkitAuthenticationException;
import com.microsoft.azure.toolkit.lib.auth.util.AzureCliUtils;
import org.apache.commons.lang3.StringUtils;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;

class AzureCliTokenCredential extends TenantCredential {
    private static final String CLI_GET_ACCESS_TOKEN_CMD = "az account get-access-token --resource %s %s --output json";
    private static final String CLOUD_SHELL_ENV_KEY = "ACC_CLOUD";

    public Mono<AccessToken> getAccessToken(String tenantId, TokenRequestContext request) {
        final String scopes = ScopeUtil.scopesToResource(request.getScopes());

        try {
            ScopeUtil.validateScope(scopes);
        } catch (IllegalArgumentException ex) {
            throw new AzureToolkitAuthenticationException(String.format("Invalid scope: %s", scopes));
        }

        final String azCommand = String.format(CLI_GET_ACCESS_TOKEN_CMD, scopes,
                (StringUtils.isBlank(tenantId) || isInCloudShell()) ? "" : (" -t " + tenantId));
        JsonObject result = AzureCliUtils.executeAzCommandJson(azCommand).getAsJsonObject();

        // copied from https://github.com/Azure/azure-sdk-for-java/blob/master/sdk/identity/azure-identity
        // /src/main/java/com/azure/identity/implementation/IdentityClient.java#L487
        String accessToken = result.get("accessToken").getAsString();
        String time = result.get("expiresOn").getAsString();
        String timeToSecond = time.substring(0, time.indexOf("."));
        String timeJoinedWithT = String.join("T", timeToSecond.split(" "));
        OffsetDateTime expiresOn = LocalDateTime.parse(timeJoinedWithT, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
                .atZone(ZoneId.systemDefault())
                .toOffsetDateTime().withOffsetSameInstant(ZoneOffset.UTC);
        return Mono.just(new AccessToken(accessToken, expiresOn));
    }

    static boolean isInCloudShell() {
        return System.getenv(CLOUD_SHELL_ENV_KEY) != null;
    }
}
