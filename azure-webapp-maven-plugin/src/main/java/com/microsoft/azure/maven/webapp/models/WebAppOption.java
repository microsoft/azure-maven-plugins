/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */


package com.microsoft.azure.maven.webapp.models;

import com.microsoft.azure.toolkit.lib.appservice.model.JavaVersion;
import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.WebContainer;
import com.microsoft.azure.toolkit.lib.appservice.service.IWebApp;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Objects;

public class WebAppOption implements Comparable<WebAppOption> {
    public static final WebAppOption CREATE_NEW = new WebAppOption();
    private static final String CREATE_NEW_STRING = "<create>";
    private IWebApp webappInner;
    private boolean createNewPlaceHolder = false;

    public WebAppOption(@Nonnull IWebApp webapp) {
        this.webappInner = webapp;
    }

    public String getId() {
        return this.webappInner == null ? null : webappInner.id();
    }

    @Override
    public String toString() {
        if (this.isCreateNew()) {
            return CREATE_NEW_STRING;
        }
        return webappInner != null ? String.format("%s (%s)", webappInner.name(), getDescription()) : null;
    }

    @Override
    public int compareTo(WebAppOption other) {
        final int typeCompareResult = Boolean.compare(createNewPlaceHolder, other.isCreateNew());
        if (typeCompareResult != 0) {
            return typeCompareResult;
        }

        final String name1 = toString();
        final String name2 = other.webappInner != null ? other.toString() : null;
        return StringUtils.compare(name1, name2);

    }

    private WebAppOption() {
        this.createNewPlaceHolder = true;
    }

    public boolean isCreateNew() {
        return this.createNewPlaceHolder;
    }

    public String getServicePlanId() {
        if (webappInner == null) {
            return null;
        }
        return webappInner.plan().id();
    }

    public boolean isDockerWebapp() {
        return webappInner != null && Objects.equals(webappInner.getRuntime().getOperatingSystem(), OperatingSystem.DOCKER);
    }

    public boolean isJavaWebApp() {
        return getJavaVersion() != JavaVersion.OFF;
    }

    public boolean isJavaSE() {
        if (webappInner == null) {
            return false;
        }
        if (!isJavaWebApp() || isDockerWebapp()) {
            return false;
        }

        return Objects.equals(webappInner.getRuntime().getWebContainer(), WebContainer.JAVA_SE);
    }

    public JavaVersion getJavaVersion() {
        if (webappInner == null || webappInner.getRuntime() == null) {
            return JavaVersion.OFF;
        }
        return ObjectUtils.firstNonNull(webappInner.getRuntime().getJavaVersion(), JavaVersion.OFF);
    }

    public OperatingSystem getOperatingSystem() {
        if (webappInner == null) {
            return null;
        }
        return webappInner.getRuntime().getOperatingSystem();
    }

    public String getDescription() {
        if (webappInner == null || webappInner.getRuntime() == null) {
            return "unknown";
        }
        return Objects.toString(webappInner.getRuntime(), null);
    }
}
