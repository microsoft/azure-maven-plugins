/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven;

import com.microsoft.azure.management.Azure;
import com.microsoft.azure.maven.auth.AuthConfiguration;
import com.microsoft.azure.maven.auth.AzureAuthHelper;
import com.microsoft.azure.maven.telemetry.*;
import org.apache.maven.execution.MavenSession;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.project.MavenProject;
import org.apache.maven.settings.Settings;
import org.apache.maven.shared.filtering.MavenResourcesFiltering;

import java.util.UUID;

import static com.microsoft.azure.maven.telemetry.AppInsightsProxy.*;

/**
 * Base abstract class for shared configurations and operations.
 */
public abstract class AbstractAzureMojo extends AbstractMojo
        implements TelemetryConfiguration, AuthConfiguration {
    public static final String AZURE_INIT_FAIL = "Failed to initialize Azure client object.";

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    protected MavenProject project;

    @Parameter(defaultValue = "${session}", readonly = true, required = true)
    protected MavenSession session;

    /**
     * The system settings for Maven. This is the instance resulting from
     * merging global and user-level settings files.
     */
    @Parameter(defaultValue = "${settings}", readonly = true, required = true)
    protected Settings settings;

    @Component(role = MavenResourcesFiltering.class, hint = "default")
    protected MavenResourcesFiltering mavenResourcesFiltering;

    @Parameter(property = "authentication")
    protected AuthenticationSetting authentication;

    @Parameter(property = "subscriptionId")
    protected String subscriptionId = "";

    @Parameter(property = "allowTelemetry", defaultValue = "true")
    protected boolean allowTelemetry;

    @Parameter(property = "failsOnError", defaultValue = "true")
    protected boolean failsOnError;

    private Azure azure;

    private TelemetryProxy telemetryProxy;

    private String sessionId = UUID.randomUUID().toString();

    private String installationId = GetHashMac.getHashMac();

    private String pluginName = Utils.getValueFromPluginDescriptor("artifactId");

    private String pluginVersion = Utils.getValueFromPluginDescriptor("version");

    public MavenProject getProject() {
        return project;
    }

    public MavenSession getSession() {
        return session;
    }

    public MavenResourcesFiltering getMavenResourcesFiltering() {
        return mavenResourcesFiltering;
    }

    public Settings getSettings() {
        return settings;
    }

    public AuthenticationSetting getAuthenticationSetting() {
        return authentication;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public boolean isTelemetryAllowed() {
        return allowTelemetry;
    }

    public boolean isFailingOnError() {
        return failsOnError;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getInstallationId() {
        return installationId;
    }

    public String getPluginName() {
        return pluginName;
    }

    public String getPluginVersion() {
        return pluginVersion;
    }

    public String getUserAgent() {
        return String.format("%s/%s %s:%s %s:%s",
                getPluginName(), getPluginVersion(),
                INSTALLATION_ID_KEY, getInstallationId(),
                SESSION_ID_KEY, getSessionId());
    }

    public Azure getAzureClient() {
        if (azure == null) {
            initAzureClient();
        }
        return azure;
    }

    protected void initAzureClient() {
        azure = new AzureAuthHelper(this).getAzureClient();
    }

    public TelemetryProxy getTelemetryProxy() {
        if (telemetryProxy == null) {
            initTelemetry();
        }
        return telemetryProxy;
    }

    protected void initTelemetry() {
        telemetryProxy = new AppInsightsProxy(this);
        if (!isTelemetryAllowed()) {
            telemetryProxy.trackEvent(TelemetryEvent.TELEMETRY_NOT_ALLOWED);
            telemetryProxy.disable();
        }
    }

    @Override
    public void execute() throws MojoExecutionException, MojoFailureException {
        if (getAzureClient() == null) {
            getTelemetryProxy().trackEvent(TelemetryEvent.INIT_FAILURE);
            throw new MojoExecutionException(AZURE_INIT_FAIL);
        } else {
            // Repopulate subscriptionId in case it is not configured.
            getTelemetryProxy().addDefaultProperty(SUBSCRIPTION_ID_KEY, azure.subscriptionId());
        }
    }
}
