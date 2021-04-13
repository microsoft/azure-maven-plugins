/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth;

import com.microsoft.azure.toolkit.lib.common.model.Subscription;
import reactor.core.publisher.Mono;

import java.util.List;

public interface SubscriptionProvider {
    Mono<List<Subscription>> listSubscriptions(List<String> tenantIds);
}
