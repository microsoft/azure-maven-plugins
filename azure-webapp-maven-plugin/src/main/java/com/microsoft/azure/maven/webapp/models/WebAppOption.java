/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */


package com.microsoft.azure.maven.webapp.models;

import com.microsoft.azure.toolkit.lib.legacy.appservice.AppServiceUtils;
import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.OperatingSystem;
import com.microsoft.azure.management.appservice.implementation.SiteConfigResourceInner;
import com.microsoft.azure.management.appservice.implementation.SiteInner;
import com.microsoft.azure.management.appservice.implementation.WebAppsInner;
import org.apache.commons.lang3.StringUtils;
import rx.Observable;

public class WebAppOption implements Comparable<WebAppOption> {
    public static final WebAppOption CREATE_NEW = new WebAppOption();
    private static final String CREATE_NEW_STRING = "<create>";
    private SiteInner siteInner;
    private SiteConfigResourceInner siteConfig;
    private WebAppsInner webappClient;
    private boolean createNewPlaceHolder = false;

    public WebAppOption(SiteInner siteInner, WebAppsInner webappClient) {
        this.siteInner = siteInner;
        this.webappClient = webappClient;
    }

    public String getId() {
        return this.siteInner == null ? null : siteInner.id();
    }

    @Override
    public String toString() {
        if (this.isCreateNew()) {
            return CREATE_NEW_STRING;
        }
        return siteInner != null ? String.format("%s (%s)", siteInner.name(), getDescription().toLowerCase()) : null;
    }

    @Override
    public int compareTo(WebAppOption other) {
        final int typeCompareResult = new Boolean(createNewPlaceHolder).compareTo(other.isCreateNew());
        if (typeCompareResult != 0) {
            return typeCompareResult;
        }

        final String name1 = toString();
        final String name2 = other.siteInner != null ? other.toString() : null;
        return StringUtils.compare(name1, name2);

    }

    private WebAppOption() {
        this.createNewPlaceHolder = true;
    }

    public boolean isCreateNew() {
        return this.createNewPlaceHolder;
    }

    public Observable<WebAppOption> loadConfigurationSync() {
        return Observable.fromCallable(() -> {
            this.siteConfig = webappClient.getConfiguration(siteInner.resourceGroup(), siteInner.name());
            return this;
        });
    }

    public String getServicePlanId() {
        if (siteInner == null) {
            return null;
        }
        return siteInner.serverFarmId();
    }

    public boolean isDockerWebapp() {
        final String linuxFxVersion = getLinuxFxVersion();
        return StringUtils.containsIgnoreCase(linuxFxVersion, "DOCKER|");
    }

    public boolean isJavaWebApp() {
        if (siteInner == null) {
            return false;
        }
        final OperatingSystem os = getOperatingSystem();
        final JavaVersion javaVersion = getJavaVersion();
        final String linuxFxVersion = getLinuxFxVersion();
        return (os == OperatingSystem.WINDOWS && javaVersion != JavaVersion.OFF) ||
                os == OperatingSystem.LINUX && (StringUtils.containsIgnoreCase(linuxFxVersion, "-jre") ||
                StringUtils.containsIgnoreCase(linuxFxVersion, "-java"));
    }

    public boolean isJavaSE() {
        if (!isJavaWebApp() || isDockerWebapp()) {
            return false;
        }

        final OperatingSystem os = getOperatingSystem();
        if (os == OperatingSystem.WINDOWS) {
            return StringUtils.startsWithIgnoreCase(siteConfig.javaContainer(), "java");
        }
        if (os == OperatingSystem.LINUX) {
            final String linuxFxVersion = getLinuxFxVersion();
            return StringUtils.startsWithIgnoreCase(linuxFxVersion, "java");
        }
        return false;
    }

    public String getLinuxFxVersion() {
        if (siteConfig == null) {
            return null;
        }
        return siteConfig.linuxFxVersion();
    }

    public JavaVersion getJavaVersion() {
        if (siteConfig == null || siteConfig.javaVersion() == null) {
            return JavaVersion.OFF;
        }
        return JavaVersion.fromString(siteConfig.javaVersion());
    }

    public OperatingSystem getOperatingSystem() {
        if (siteInner == null || siteConfig == null) {
            return null;
        }
        if (siteInner.kind() != null && siteInner.kind().toLowerCase().contains("linux")) {
            return OperatingSystem.LINUX;
        } else {
            return OperatingSystem.WINDOWS;
        }
    }

    public String getDescription() {
        if (siteInner == null || siteConfig == null) {
            return "unknown";
        }
        if (isDockerWebapp()) {
            return "docker";
        }
        final OperatingSystem os = getOperatingSystem();
        if (os == OperatingSystem.WINDOWS) {
            if (StringUtils.isNotBlank(siteConfig.javaContainer())) {
                return "windows, " + siteConfig.javaContainer() + " " + siteConfig.javaContainerVersion();
            } else {
                return "windows, java " + siteConfig.javaVersion();
            }
        } else {
            return "linux, " + AppServiceUtils.parseRuntimeStack(getLinuxFxVersion());
        }
    }
}
