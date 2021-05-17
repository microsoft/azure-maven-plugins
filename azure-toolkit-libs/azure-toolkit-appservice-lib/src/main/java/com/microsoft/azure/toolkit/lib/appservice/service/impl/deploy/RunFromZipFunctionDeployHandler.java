/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.service.impl.deploy;

import com.azure.resourcemanager.appservice.models.WebAppBase;

import java.io.File;

public class RunFromZipFunctionDeployHandler extends ZIPFunctionDeployHandler {
    private static final String APP_SETTING_WEBSITE_RUN_FROM_PACKAGE = "WEBSITE_RUN_FROM_PACKAGE";
    private static final String RUN_FROM_PACKAGE_VALUE = "1";

    @Override
    public void deploy(File file, WebAppBase functionApp) {
        DeployUtils.updateFunctionAppSetting(functionApp, APP_SETTING_WEBSITE_RUN_FROM_PACKAGE, RUN_FROM_PACKAGE_VALUE);
        try {
            // work around for issue https://dev.azure.com/msazure/Unified%20Platform%20KPIs/_workitems/edit/7481871
            Thread.sleep(20 * 1000);
        } catch (InterruptedException e) {
            // Sorrow exception here as we just wait for 20s for kudu app settings update, which is not required in normal cases
        }
        super.deploy(file, functionApp);
    }
}
