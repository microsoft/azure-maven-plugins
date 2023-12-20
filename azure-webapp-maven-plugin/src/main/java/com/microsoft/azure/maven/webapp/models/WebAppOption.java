/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */


package com.microsoft.azure.maven.webapp.models;

import com.microsoft.azure.toolkit.lib.appservice.model.OperatingSystem;
import com.microsoft.azure.toolkit.lib.appservice.model.Runtime;
import com.microsoft.azure.toolkit.lib.appservice.model.WebAppRuntime;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebApp;
import com.microsoft.azure.toolkit.lib.appservice.webapp.WebAppBase;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Objects;
import java.util.Optional;

public class WebAppOption implements Comparable<WebAppOption> {
    @Getter
    private WebApp webappInner;
    private boolean createNewPlaceHolder = false;

    public WebAppOption(@Nonnull WebApp webapp) {
        this.webappInner = webapp;
    }

    public String getId() {
        return this.webappInner == null ? null : webappInner.id();
    }

    @Override
    public String toString() {
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
        return webappInner.getAppServicePlan().getId();
    }

    public boolean isDockerWebapp() {
        return webappInner != null && webappInner.getRuntime().isDocker();
    }

    public boolean isJavaWebApp() {
        return !isDockerWebapp() && !isJavaSE();
    }

    public boolean isJavaSE() {
        if (webappInner == null) {
            return false;
        }
        final Runtime runtime = webappInner.getRuntime();
        if (runtime instanceof WebAppRuntime) {
            return ((WebAppRuntime) runtime).isJavaSE();
        }
        return false;
    }

    public OperatingSystem getOperatingSystem() {
        return Optional.ofNullable(webappInner).map(WebAppBase::getRuntime).map(Runtime::getOperatingSystem).orElse(null);
    }

    public String getDescription() {
        if (webappInner == null || webappInner.getRuntime() == null) {
            return "unknown";
        }
        return Objects.toString(webappInner.getRuntime(), null);
    }
}
