package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.SharedTokenCacheCredentialBuilder;

import javax.annotation.Nonnull;

public class SharedTokenCacheAccount extends Account {
    private final AuthConfiguration config;

    public SharedTokenCacheAccount(@Nonnull AuthConfiguration config) {
        super(config.getType(), config.getClient());
        this.config = config;
    }

    @Nonnull
    @Override
    protected TokenCredential buildDefaultTokenCredential() {
        return new SharedTokenCacheCredentialBuilder()
            .tokenCachePersistenceOptions(PERSISTENCE_OPTIONS)
            // default tenant id in azure identity is organizations
            // see https://github.com/Azure/azure-sdk-for-java/blob/026664ea871586e681ab674e0332b6cc2352c655
            // /sdk/identity/azure-identity/src/main/java/com/azure/identity/implementation/IdentityClient.java#L139
            // .tenantId(CollectionUtils.isEmpty(config.getTenantIds()) ? "organizations" : config.getTenantIds().get(0))
            .username(config.getUsername())
            .clientId(config.getClient())
            .build();
    }

    @Override
    public boolean checkAvailable() {
        return this.getManagementToken().isPresent();
    }
}
