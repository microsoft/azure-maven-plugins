/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import com.microsoft.azure.toolkit.lib.appservice.service.IAppService;
import com.microsoft.azure.toolkit.lib.common.logging.Log;
import org.apache.commons.lang3.StringUtils;

import java.util.concurrent.TimeUnit;


public class WebAppUtils {
    private static final String SERVICE_PLAN_NOT_APPLICABLE = "The App Service Plan '%s' is not a %s Plan";
    private static final String CONFIGURATION_NOT_APPLICABLE =
            "The configuration is not applicable for the target Web App (%s). Please correct it in pom.xml.";
    private static final String STOP_APP = "Stopping Web App before deploying artifacts...";
    private static final String START_APP = "Starting Web App after deploying artifacts...";
    private static final String STOP_APP_DONE = "Successfully stopped Web App.";
    private static final String START_APP_DONE = "Successfully started Web App.";
    private static final String RUNNING = "Running";

    public static void stopAppService(IAppService target) {
        Log.info(STOP_APP);
        target.stop();
        // workaround for the resources release problem.
        // More details: https://github.com/Microsoft/azure-maven-plugins/issues/191
        try {
            TimeUnit.SECONDS.sleep(10 /* 10 seconds */);
        } catch (InterruptedException e) {
            // swallow exception
        }
        Log.info(STOP_APP_DONE);
    }

    public static void startAppService(IAppService target) {
        if (!StringUtils.equalsIgnoreCase(target.state(), RUNNING)) {
            Log.info(START_APP);
            target.start();
            Log.info(START_APP_DONE);
        }
    }
}
