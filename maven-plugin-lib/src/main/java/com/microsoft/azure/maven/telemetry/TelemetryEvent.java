/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.telemetry;

public class TelemetryEvent {
    public static final String TELEMETRY_NOT_ALLOWED = "TelemetryNotAllowed";
    public static final String INIT_FAILURE = "InitFailure";
    public static final String DEPLOY_START = "DeployStart";
    public static final String DEPLOY_SUCCESS = "DeploySuccess";
    public static final String DEPLOY_FAILURE = "DeployFailure";
}
