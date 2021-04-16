/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import reactor.core.publisher.Mono;

import java.util.List;

public interface TenantProvider {
    Mono<List<String>> listTenants();
}
