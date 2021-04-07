package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import lombok.AllArgsConstructor;
import lombok.Setter;
import reactor.core.publisher.Mono;

@Setter
@AllArgsConstructor
public class SingleTenantCredential extends TenantCredential {

    private TokenCredential credential;

    @Override
    protected Mono<AccessToken> getAccessToken(String tenantId, TokenRequestContext request) {
        return credential.getToken(request);
    }
}
