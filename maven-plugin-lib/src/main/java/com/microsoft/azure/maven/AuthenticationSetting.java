/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import java.io.File;

public class AuthenticationSetting {
    /**
     * serverId from settings.xml
     */
    private String serverId;

    /**
     * Authentication file.
     */
    private File file;

    public String getServerId() {
        return serverId;
    }

    public void setServerId(final String serverId) {
        this.serverId = serverId;
    }

    public File getFile() {
        return file;
    }

    public void setFile(final File file) {
        this.file = file;
    }
}
