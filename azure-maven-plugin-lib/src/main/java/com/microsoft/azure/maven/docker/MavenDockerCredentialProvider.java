/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.maven.docker;

import com.google.common.base.Preconditions;
import com.microsoft.azure.common.docker.IDockerCredentialProvider;
import com.microsoft.azure.common.exceptions.AzureExecutionException;
import com.microsoft.azure.maven.exceptions.ServerNotFoundException;

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

    @Override
    public String getUsername() throws AzureExecutionException {
    	if (server == null) {
    		initializeServer();
    	}
        return server != null ? server.getUsername() : null;
    }

    @Override
    public String getPassword() throws AzureExecutionException {
    	if (server == null) {
    		initializeServer();
    	}
        return server != null ? server.getPassword() : null;
    }

    private void initializeServer() throws ServerNotFoundException {
    	if (StringUtils.isNotBlank(serverId)) {
            server = settings.getServer(serverId);
            if (server == null) {
                throw new ServerNotFoundException(String.format("Server not found in settings.xml. ServerId=%s", serverId));
            }
    	}
    }
}
