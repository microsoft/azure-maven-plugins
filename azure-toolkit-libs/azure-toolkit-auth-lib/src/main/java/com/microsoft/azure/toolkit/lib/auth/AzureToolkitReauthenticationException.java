/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.azure.identity.AuthenticationRequiredException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import lombok.Getter;

import javax.annotation.Nonnull;

@Getter
public class AzureToolkitReauthenticationException extends AzureToolkitRuntimeException {
    @Nonnull
    private final ReauthenticationRequest request;

    AzureToolkitReauthenticationException(@Nonnull AuthenticationRequiredException cause,
                                          @Nonnull ReauthenticationRequest request, @Nonnull Object action) {
        super("Your Azure session needs reauthentication.", cause, action);
        this.request = request;
    }
}
