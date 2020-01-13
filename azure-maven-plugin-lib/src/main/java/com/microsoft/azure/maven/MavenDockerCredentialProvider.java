/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import com.google.common.base.Preconditions;
import com.microsoft.azure.common.exceptions.AzureExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

public class MavenDockerCredentialProvider {
    private Server server;
    private Settings settings;
    private String serverId;

    private MavenDockerCredentialProvider(Settings settings, String serverId) {
        if (StringUtils.isNotBlank(serverId)) {
            Preconditions.checkNotNull(settings, "Maven 'settings' is required");
            this.serverId = serverId;
            this.settings = settings;
        }
    }

    public static MavenDockerCredentialProvider fromMavenSettings(Settings settings, String serverId) {
        return new MavenDockerCredentialProvider(settings, serverId);
    }

    public String getUsername() throws AzureExecutionException {
        if (server == null) {
            initializeServer();
        }
        return server != null ? server.getUsername() : null;
    }

    public String getPassword() throws AzureExecutionException {
        if (server == null) {
            initializeServer();
        }
        return server != null ? server.getPassword() : null;
    }

    public void validate() throws AzureExecutionException {
        if (server == null) {
            initializeServer();
        }
    }

    private void initializeServer() throws AzureExecutionException {
        if (StringUtils.isNotBlank(serverId)) {
            server = settings.getServer(serverId);
            if (server == null) {
                throw new AzureExecutionException(String.format("Server not found in settings.xml. ServerId=%s", serverId));
            }
        }
    }
}
