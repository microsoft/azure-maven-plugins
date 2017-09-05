/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp;

import com.microsoft.azure.management.appservice.WebApp.Update;
import com.microsoft.azure.maven.webapp.handlers.HandlerFactory;

public class DeployFacadeImplWithUpdate extends DeployFacadeBaseImpl {
    private Update update = null;

    public DeployFacadeImplWithUpdate(AbstractWebAppMojo mojo) {
        super(mojo);
    }

    @Override
    public DeployFacadeBaseImpl setupRuntime() throws Exception {
        update = HandlerFactory.getInstance()
                .getRuntimeHandler(getMojo())
                .updateAppRuntime();
        return this;
    }

    @Override
    public DeployFacadeBaseImpl applySettings() throws Exception {
        HandlerFactory.getInstance()
                .getSettingsHandler(getMojo())
                .processSettings(update);
        return this;
    }

    @Override
    public DeployFacadeBaseImpl commitChanges() throws Exception {
        update.apply();
        return this;
    }
}
