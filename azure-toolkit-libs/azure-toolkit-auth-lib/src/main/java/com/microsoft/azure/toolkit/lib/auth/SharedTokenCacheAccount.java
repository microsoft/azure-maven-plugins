package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.credential.TokenCredential;
import com.azure.identity.SharedTokenCacheCredentialBuilder;
import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import lombok.extern.slf4j.Slf4j;

import javax.annotation.Nonnull;

@Slf4j
public class SharedTokenCacheAccount extends Account {

    public SharedTokenCacheAccount(@Nonnull AuthConfiguration config) {
        super(config);
    }

    @Nonnull
    @Override
    protected TokenCredential buildDefaultTokenCredential() {
        final AuthConfiguration config = getConfig();
        return new SharedTokenCacheCredentialBuilder()
                .tokenCachePersistenceOptions(PERSISTENCE_OPTIONS)
                // default tenant id in azure identity is organizations
                // see https://github.com/Azure/azure-sdk-for-java/blob/026664ea871586e681ab674e0332b6cc2352c655
                // /sdk/identity/azure-identity/src/main/java/com/azure/identity/implementation/IdentityClient.java#L139
                // .tenantId(CollectionUtils.isEmpty(config.getTenantIds()) ? "organizations" : config.getTenantIds().get(0))
                .tenantId(config.getTenant())
                .username(config.getUsername())
                .clientId(config.getClient())
                .build();
    }

    @Override
    public boolean checkAvailable() {
        final boolean available = this.getManagementToken().isPresent();
        log.trace("Auth type ({}) is {}available.", TextUtils.cyan(this.getType().name()), available ? "" : TextUtils.yellow("NOT "));
        return available;
    }

    @Override
    public AuthType getType() {
        return this.getConfig().getType();
    }
}
