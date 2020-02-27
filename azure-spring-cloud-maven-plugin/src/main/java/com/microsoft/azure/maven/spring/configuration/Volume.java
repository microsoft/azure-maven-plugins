/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.spring.configuration;

public class Volume {
    private String path;
    private String size;
    private Boolean persist;

    public String getPath() {
        return path;
    }

    public String getSize() {
        return size;
    }

    public Boolean isPersist() {
        return persist;
    }

    public Volume withPath(String path) {
        this.path = path;
        return this;
    }

    public Volume withSize(String size) {
        this.size = size;
        return this;
    }

    public Volume withPersist(Boolean persist) {
        this.persist = persist;
        return this;
    }
}
