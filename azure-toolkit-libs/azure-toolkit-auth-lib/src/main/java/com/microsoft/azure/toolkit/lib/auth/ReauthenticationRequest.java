/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.core.credential.TokenRequestContext;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

public final class ReauthenticationRequest {
    private enum State {
        REQUIRED,
        AUTHENTICATING,
        COMPLETED
    }

    @Getter
    @Nonnull
    private final String tenantId;
    @Getter
    @Nonnull
    private final TokenRequestContext tokenRequestContext;
    @Nonnull
    private final Consumer<ReauthenticationRequest> authenticator;
    private final AtomicReference<State> state = new AtomicReference<>(State.REQUIRED);

    ReauthenticationRequest(@Nonnull String tenantId, @Nonnull TokenRequestContext tokenRequestContext,
                            @Nonnull Consumer<ReauthenticationRequest> authenticator) {
        this.tenantId = tenantId;
        this.tokenRequestContext = tokenRequestContext;
        this.authenticator = authenticator;
    }

    public void authenticate() {
        if (!this.state.compareAndSet(State.REQUIRED, State.AUTHENTICATING)) {
            return;
        }
        try {
            this.authenticator.accept(this);
            this.state.set(State.COMPLETED);
        } catch (final RuntimeException | Error e) {
            this.state.set(State.REQUIRED);
            throw e;
        }
    }

    boolean isCompleted() {
        return this.state.get() == State.COMPLETED;
    }
}
