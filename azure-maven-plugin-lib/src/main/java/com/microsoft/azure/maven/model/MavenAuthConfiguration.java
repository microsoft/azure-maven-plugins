/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.model;

import com.microsoft.azure.tools.auth.model.AuthConfiguration;
import lombok.Getter;
import lombok.Setter;

public class MavenAuthConfiguration extends AuthConfiguration {
    @Setter
    @Getter
    private String serverId;
}
