/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.auth.model;


import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class SubscriptionEntity {
    private String environment;
    private String id;
    private String name;
    private String email;
    private String tenantId;
    private boolean selected;
}
