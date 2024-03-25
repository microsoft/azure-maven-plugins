/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.appservice;

import com.google.common.base.Preconditions;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.legacy.docker.IDockerCredentialProvider;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

public class MavenDockerCredentialProvider implements IDockerCredentialProvider {
    private Server server;
    private Settings settings;
    private String serverId;

    public MavenDockerCredentialProvider(Settings settings, String serverId) {
        if (StringUtils.isNotBlank(serverId)) {
            Preconditions.checkNotNull(settings, "Maven 'settings' is required");
            this.serverId = serverId;
            this.settings = settings;
        }
    }

    public static MavenDockerCredentialProvider fromMavenSettings(Settings settings, String serverId) {
        return new MavenDockerCredentialProvider(settings, serverId);
    }

    public String getUsername() {
        if (server == null) {
            initializeServer();
        }
        return server != null ? server.getUsername() : null;
    }

    public String getPassword() {
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

    private void initializeServer() {
        if (StringUtils.isNotBlank(serverId)) {
            server = settings.getServer(serverId);
            if (server == null) {
                throw new AzureToolkitRuntimeException(String.format("Server not found in settings.xml. ServerId=%s", serverId));
            }
        }
    }
}
