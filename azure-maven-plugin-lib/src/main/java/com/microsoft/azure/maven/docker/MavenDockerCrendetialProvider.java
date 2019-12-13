/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */
package com.microsoft.azure.maven.docker;

import com.google.common.base.Preconditions;
import com.microsoft.azure.common.docker.IDockerCredentialProvider;
import com.microsoft.azure.maven.exceptions.ServerNotFoundException;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;

public class MavenDockerCrendetialProvider implements IDockerCredentialProvider {
    private Server server;

    public MavenDockerCrendetialProvider(Settings settings, String serverId) throws ServerNotFoundException {
        if (StringUtils.isNotBlank(serverId)) {
            Preconditions.checkNotNull(settings, "Maven 'settings' is required");
            this.server = getServer(settings, serverId);
            if (server == null) {
                throw new ServerNotFoundException(String.format("Server not found in settings.xml. ServerId=%s", serverId));
            }
        }
    }

    @Override
    public String getUsername() {
        return server != null ? server.getUsername() : null;
    }

    @Override
    public String getPassword() {
        return server != null ? server.getPassword() : null;
    }

    private static Server getServer(final Settings settings, final String serverId) {

        return settings.getServer(serverId);
    }
}
