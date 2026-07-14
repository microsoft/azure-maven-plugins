/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import com.azure.core.management.AzureEnvironment;
import com.azure.identity.AuthenticationRequiredException;
import com.azure.identity.InteractiveBrowserCredential;
import com.azure.identity.InteractiveBrowserCredentialBuilder;
import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Test;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import java.time.OffsetDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

public class AccountTest {
    @Test
    public void controlsAzureIdentityAutomaticAuthentication() throws IllegalAccessException {
        final InteractiveBrowserCredential credential = new InteractiveBrowserCredentialBuilder()
            .disableAutomaticAuthentication()
            .build();
        final InteractiveAuthenticationController controller = new InteractiveAuthenticationController(credential);

        controller.enableAutomaticAuthentication();
        assertTrue((boolean) FieldUtils.readField(credential, "automaticAuthentication", true));
        controller.disableAutomaticAuthentication();
        assertFalse((boolean) FieldUtils.readField(credential, "automaticAuthentication", true));
    }

    @Test
    public void tokenRequestsWaitForOngoingInteractiveAuthentication() throws Exception {
        final BlockingCredential credential = new BlockingCredential();
        final InteractiveAuthenticationController controller = new InteractiveAuthenticationController(credential);
        final TokenRequestContext request = new TokenRequestContext().addScopes("scope");

        final CompletableFuture<Void> authentication = CompletableFuture.runAsync(() -> controller.authenticate(request));
        assertTrue(credential.authenticationStarted.await(5, TimeUnit.SECONDS));
        final CompletableFuture<AccessToken> queuedToken = controller.getToken(request).toFuture();
        assertFalse(queuedToken.isDone());

        credential.continueAuthentication.countDown();
        authentication.get(5, TimeUnit.SECONDS);
        queuedToken.get(5, TimeUnit.SECONDS);
        assertEquals(1, credential.authenticationCount.get());
        assertFalse(credential.automaticAuthentication);
    }

    @Test
    public void concurrentTenantReauthenticationVerifiesEachTenant() throws Exception {
        final MultiTenantCredential credential = new MultiTenantCredential();
        final InteractiveAuthenticationController controller = new InteractiveAuthenticationController(credential);
        final TokenRequestContext firstRequest = new TokenRequestContext().addScopes("scope").setTenantId("first");
        final TokenRequestContext secondRequest = new TokenRequestContext().addScopes("scope").setTenantId("second");

        final CompletableFuture<Void> first = CompletableFuture.runAsync(() -> controller.authenticate(firstRequest));
        assertTrue(credential.firstAuthenticationStarted.await(5, TimeUnit.SECONDS));
        final CompletableFuture<Void> second = CompletableFuture.runAsync(() -> controller.authenticate(secondRequest));
        credential.continueFirstAuthentication.countDown();

        first.get(5, TimeUnit.SECONDS);
        second.get(5, TimeUnit.SECONDS);
        assertEquals(2, credential.authenticationCount.get());
        assertTrue(credential.authenticatedTenants.contains("first"));
        assertTrue(credential.authenticatedTenants.contains("second"));
        assertFalse(credential.automaticAuthentication);
    }

    @Test
    public void interactiveAuthenticationCanBeRepeatedForOneTenant() {
        final TestAccount account = new TestAccount();
        account.login();
        assertEquals(1, account.credential.authenticationCount.get());
        assertFalse(account.credential.automaticAuthentication);

        account.credential.requireAuthentication.set(true);
        final TokenCredential tenantCredential = account.getTenantTokenCredential("tenant");
        final TokenRequestContext context = new TokenRequestContext().addScopes("https://management.core.windows.net//.default");
        final AzureToolkitReauthenticationException first = getAuthenticationException(tenantCredential, context);
        final AzureToolkitReauthenticationException second = getAuthenticationException(tenantCredential, context);
        assertSame(first.getRequest(), second.getRequest());

        first.getRequest().authenticate();
        assertEquals(2, account.credential.authenticationCount.get());
        assertFalse(account.credential.automaticAuthentication);
        assertFalse(account.credential.requireAuthentication.get());
        tenantCredential.getToken(context).block();
    }

    private static AzureToolkitReauthenticationException getAuthenticationException(TokenCredential credential,
                                                                                     TokenRequestContext context) {
        try {
            credential.getToken(context).block();
            throw new AssertionError("Expected AzureToolkitReauthenticationException");
        } catch (AzureToolkitReauthenticationException e) {
            return e;
        }
    }

    private static class TestAccount extends Account {
        private final TestCredential credential = new TestCredential();

        private TestAccount() {
            super(new AuthConfiguration(AuthType.OAUTH2));
        }

        @Nonnull
        @Override
        protected TokenCredential buildDefaultTokenCredential() {
            return this.credential;
        }

        @Override
        protected boolean supportsInteractiveAuthentication() {
            return true;
        }

        @Override
        public List<Subscription> reloadSubscriptions() {
            this.getTenantTokenCredential("tenant").getToken(
                new TokenRequestContext().addScopes("https://management.core.windows.net//.default")).block();
            return Collections.emptyList();
        }

        @Nonnull
        @Override
        public List<String> getTenantIds() {
            return Collections.emptyList();
        }

        @Override
        protected void setupAfterLogin(TokenCredential defaultTokenCredential) {
        }

        @Override
        public boolean checkAvailable() {
            return true;
        }

        @Override
        public AzureEnvironment getEnvironment() {
            return AzureEnvironment.AZURE;
        }

        @Override
        public AuthType getType() {
            return AuthType.OAUTH2;
        }
    }

    private static class TestCredential implements TokenCredential {
        private final AtomicBoolean requireAuthentication = new AtomicBoolean(true);
        private final AtomicInteger authenticationCount = new AtomicInteger();
        private boolean automaticAuthentication;

        @Override
        public Mono<AccessToken> getToken(TokenRequestContext request) {
            if (this.requireAuthentication.get()) {
                if (!this.automaticAuthentication) {
                    return Mono.error(new AuthenticationRequiredException("Authentication required", request));
                }
                this.requireAuthentication.set(false);
                this.authenticationCount.incrementAndGet();
            }
            return Mono.just(new AccessToken("token", OffsetDateTime.now().plusHours(1)));
        }
    }

    private static class BlockingCredential implements TokenCredential {
        private final AtomicInteger authenticationCount = new AtomicInteger();
        private final CountDownLatch authenticationStarted = new CountDownLatch(1);
        private final CountDownLatch continueAuthentication = new CountDownLatch(1);
        private final AtomicBoolean requireAuthentication = new AtomicBoolean(true);
        private boolean automaticAuthentication;

        @Override
        public Mono<AccessToken> getToken(TokenRequestContext request) {
            return Mono.fromCallable(() -> {
                if (this.requireAuthentication.get()) {
                    if (!this.automaticAuthentication) {
                        throw new AuthenticationRequiredException("Authentication required", request);
                    }
                    this.authenticationCount.incrementAndGet();
                    this.authenticationStarted.countDown();
                    try {
                        this.continueAuthentication.await();
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new AzureToolkitAuthenticationException("Authentication interrupted.", e);
                    }
                    this.requireAuthentication.set(false);
                }
                return new AccessToken("token", OffsetDateTime.now().plusHours(1));
            });
        }
    }

    private static class MultiTenantCredential implements TokenCredential {
        private final AtomicInteger authenticationCount = new AtomicInteger();
        private final AtomicBoolean firstAuthentication = new AtomicBoolean(true);
        private final CountDownLatch firstAuthenticationStarted = new CountDownLatch(1);
        private final CountDownLatch continueFirstAuthentication = new CountDownLatch(1);
        private final Set<String> authenticatedTenants = Collections.newSetFromMap(new ConcurrentHashMap<>());
        private boolean automaticAuthentication;

        @Override
        public Mono<AccessToken> getToken(TokenRequestContext request) {
            return Mono.fromCallable(() -> {
                final String tenantId = request.getTenantId();
                if (!this.authenticatedTenants.contains(tenantId)) {
                    if (!this.automaticAuthentication) {
                        throw new AuthenticationRequiredException("Authentication required", request);
                    }
                    this.authenticationCount.incrementAndGet();
                    if (this.firstAuthentication.compareAndSet(true, false)) {
                        this.firstAuthenticationStarted.countDown();
                        try {
                            this.continueFirstAuthentication.await();
                        } catch (final InterruptedException e) {
                            Thread.currentThread().interrupt();
                            throw new AzureToolkitAuthenticationException("Authentication interrupted.", e);
                        }
                    }
                    this.authenticatedTenants.add(tenantId);
                }
                return new AccessToken("token", OffsetDateTime.now().plusHours(1));
            });
        }
    }
}
