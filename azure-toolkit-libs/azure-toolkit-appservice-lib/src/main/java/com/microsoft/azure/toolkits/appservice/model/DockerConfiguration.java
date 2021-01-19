/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.toolkits.appservice.model;

import lombok.Getter;
import lombok.experimental.SuperBuilder;
import org.apache.commons.lang3.StringUtils;

@Getter
@SuperBuilder
public class DockerConfiguration {
    private String image;
    private String registryUrl;
    private String userName;
    private String password;

    public boolean isPublic() {
        return StringUtils.isAllEmpty(userName, password);
    }
}
