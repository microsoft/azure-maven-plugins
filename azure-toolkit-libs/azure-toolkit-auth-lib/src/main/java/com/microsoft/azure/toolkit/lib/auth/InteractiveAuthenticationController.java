/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.credential.AccessToken;
import com.azure.core.credential.TokenCredential;
import com.azure.core.credential.TokenRequestContext;
import org.apache.commons.lang3.reflect.FieldUtils;
import reactor.core.publisher.Mono;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Field;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

final class InteractiveAuthenticationController {
    private static final String AUTOMATIC_AUTHENTICATION_FIELD = "automaticAuthentication";

    @Nonnull
    private final TokenCredential credential;
    private int activeTokenRequests;
    private CompletableFuture<Void> authentication;

    InteractiveAuthenticationController(@Nonnull TokenCredential credential) {
        this.credential = credential;
        this.getAutomaticAuthenticationField();
    }

    boolean isFor(@Nonnull TokenCredential credential) {
        return this.credential == credential;
    }

    Mono<AccessToken> getToken(@Nonnull TokenRequestContext request) {
        return Mono.defer(() -> {
            final CompletableFuture<Void> ongoingAuthentication;
            synchronized (this) {
                ongoingAuthentication = this.authentication;
                if (Objects.isNull(ongoingAuthentication)) {
                    this.activeTokenRequests++;
                }
            }
            if (Objects.nonNull(ongoingAuthentication)) {
                return Mono.fromFuture(ongoingAuthentication).then(this.getToken(request));
            }
            return this.credential.getToken(request).doFinally(ignored -> this.completeTokenRequest());
        });
    }

    void authenticate(@Nonnull TokenRequestContext request) {
        while (true) {
            final CompletableFuture<Void> currentAuthentication;
            final boolean owner;
            synchronized (this) {
                if (Objects.nonNull(this.authentication)) {
                    currentAuthentication = this.authentication;
                    owner = false;
                } else {
                    currentAuthentication = new CompletableFuture<>();
                    this.authentication = currentAuthentication;
                    owner = true;
                }
            }
            if (!owner) {
                currentAuthentication.join();
                continue;
            }

            try {
                this.awaitActiveTokenRequests();
                this.enableAutomaticAuthentication();
                this.credential.getToken(request).blockOptional()
                    .orElseThrow(() -> new AzureToolkitAuthenticationException("Failed to retrieve token."));
            } catch (final RuntimeException e) {
                this.finishAuthentication(currentAuthentication, e);
                throw e;
            } catch (final Error e) {
                this.finishAuthentication(currentAuthentication, e);
                throw e;
            }
            this.finishAuthentication(currentAuthentication, null);
            return;
        }
    }

    void enableAutomaticAuthentication() {
        this.setAutomaticAuthentication(true);
    }

    void disableAutomaticAuthentication() {
        this.setAutomaticAuthentication(false);
    }

    private void awaitActiveTokenRequests() {
        synchronized (this) {
            while (this.activeTokenRequests > 0) {
                try {
                    this.wait();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new AzureToolkitAuthenticationException("Interrupted while waiting to reauthenticate.", e);
                }
            }
        }
    }

    private void completeTokenRequest() {
        synchronized (this) {
            this.activeTokenRequests--;
            this.notifyAll();
        }
    }

    private void finishAuthentication(@Nonnull CompletableFuture<Void> currentAuthentication,
                                      @Nullable Throwable failure) {
        Throwable completionFailure = failure;
        try {
            this.disableAutomaticAuthentication();
        } catch (final RuntimeException | Error e) {
            if (Objects.isNull(completionFailure)) {
                completionFailure = e;
            } else {
                completionFailure.addSuppressed(e);
            }
        } finally {
            synchronized (this) {
                this.authentication = null;
                this.notifyAll();
            }
        }
        if (Objects.isNull(completionFailure)) {
            currentAuthentication.complete(null);
        } else {
            currentAuthentication.completeExceptionally(completionFailure);
            if (Objects.isNull(failure)) {
                if (completionFailure instanceof RuntimeException) {
                    throw (RuntimeException) completionFailure;
                }
                throw (Error) completionFailure;
            }
        }
    }

    private void setAutomaticAuthentication(boolean enabled) {
        try {
            // Azure Identity has no public runtime switch for this behavior.
            FieldUtils.writeField(this.getAutomaticAuthenticationField(), this.credential, enabled, true);
        } catch (final IllegalAccessException e) {
            throw new AzureToolkitAuthenticationException("Failed to configure interactive authentication.", e);
        }
    }

    @Nonnull
    private Field getAutomaticAuthenticationField() {
        final Field field = FieldUtils.getField(this.credential.getClass(), AUTOMATIC_AUTHENTICATION_FIELD, true);
        if (Objects.isNull(field) || field.getType() != boolean.class) {
            throw new AzureToolkitAuthenticationException("The Azure Identity credential does not support controlled interactive authentication.");
        }
        return field;
    }
}
