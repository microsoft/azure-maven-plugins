/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import com.microsoft.azure.management.appservice.WebAppBase;
import lombok.Data;
import lombok.With;

import java.util.Objects;

@Data
public class AppServiceFile {
    private String name;
    private long size;
    private String mtime;
    private String crtime;
    private String mime;
    private String href;
    private String path;
    private WebAppBase app;

    public String getId() {
        return String.format("<%s>/%s", this.getApp().id(), this.getPath());
    }

    public String getFullName() {
        return String.format("<%s>/%s", this.getApp().name(), this.getName());
    }

    public Type getType() {
        return Objects.equals("inode/directory", this.mime) ? Type.DIRECTORY : Type.FILE;
    }

    public enum Type {
        DIRECTORY, FILE
    }
}
