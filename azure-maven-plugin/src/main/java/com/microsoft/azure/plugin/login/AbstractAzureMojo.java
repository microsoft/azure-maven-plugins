/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.plugin.login;

import com.microsoft.azure.maven.telemetry.AppInsightHelper;
import com.microsoft.azure.maven.telemetry.MojoStatus;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.descriptor.PluginDescriptor;
import org.apache.maven.plugins.annotations.Parameter;

import java.util.HashMap;
import java.util.Map;

import static com.microsoft.azure.maven.telemetry.Constants.TELEMETRY_KEY_ERROR_CODE;
import static com.microsoft.azure.maven.telemetry.Constants.TELEMETRY_KEY_ERROR_MESSAGE;
import static com.microsoft.azure.maven.telemetry.Constants.TELEMETRY_KEY_ERROR_TYPE;
import static com.microsoft.azure.maven.telemetry.Constants.TELEMETRY_KEY_PLUGIN_NAME;
import static com.microsoft.azure.maven.telemetry.Constants.TELEMETRY_KEY_PLUGIN_VERSION;
import static com.microsoft.azure.maven.telemetry.Constants.TELEMETRY_VALUE_ERROR_CODE_FAILURE;
import static com.microsoft.azure.maven.telemetry.Constants.TELEMETRY_VALUE_ERROR_CODE_SUCCESS;
import static com.microsoft.azure.maven.telemetry.Constants.TELEMETRY_VALUE_SYSTEM_ERROR;
import static com.microsoft.azure.maven.telemetry.Constants.TELEMETRY_VALUE_USER_ERROR;

public abstract class AbstractAzureMojo extends AbstractMojo {

    protected Map<String, String> telemetries;

    @Parameter(property = "isTelemetryAllowed", defaultValue = "true")
    protected boolean isTelemetryAllowed;

    @Parameter(defaultValue = "${plugin}", readonly = true, required = true)
    protected PluginDescriptor plugin;

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        try {
            initTelemetry();
            doExecute();
            handleSuccess();
        } catch (Exception e) {
            handleException(e);
        } finally {
            AppInsightHelper.INSTANCE.close();
        }
    }

    protected void initTelemetry() {
        telemetries = new HashMap<>();
        if (!isTelemetryAllowed) {
            AppInsightHelper.INSTANCE.disable();
            return;
        }
        telemetries.put(TELEMETRY_KEY_PLUGIN_NAME, plugin.getArtifactId());
        telemetries.put(TELEMETRY_KEY_PLUGIN_VERSION, plugin.getVersion());
        trackMojoExecution(MojoStatus.Start);
    }

    protected void handleSuccess() {
        telemetries.put(TELEMETRY_KEY_ERROR_CODE, TELEMETRY_VALUE_ERROR_CODE_SUCCESS);
        trackMojoExecution(MojoStatus.Success);
    }

    protected void handleException(Exception exception) {
        final boolean isUserError = exception instanceof IllegalArgumentException;
        telemetries.put(TELEMETRY_KEY_ERROR_CODE, TELEMETRY_VALUE_ERROR_CODE_FAILURE);
        telemetries.put(TELEMETRY_KEY_ERROR_TYPE, isUserError ? TELEMETRY_VALUE_USER_ERROR : TELEMETRY_VALUE_SYSTEM_ERROR);
        telemetries.put(TELEMETRY_KEY_ERROR_MESSAGE, exception.getMessage());
        trackMojoExecution(MojoStatus.Failure);
    }

    protected void trackMojoExecution(MojoStatus status) {
        final String eventName = String.format("%s.%s", this.getClass().getSimpleName(), status.name());
        AppInsightHelper.INSTANCE.trackEvent(eventName, getTelemetries(), false);
    }

    public Map<String, String> getTelemetries() {
        return telemetries;
    }

    public abstract void doExecute() throws MojoFailureException, MojoExecutionException;
}
