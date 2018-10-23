/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.configuration;

import org.apache.maven.model.Resource;
import java.util.Collections;
import java.util.List;

public class Deployment {
    protected List<Resource> resources = Collections.emptyList();

    public List<Resource> getResources() {
        return this.resources;
    }

    public void setResources(final List<Resource> value) {
        this.resources = value;
    }
}
