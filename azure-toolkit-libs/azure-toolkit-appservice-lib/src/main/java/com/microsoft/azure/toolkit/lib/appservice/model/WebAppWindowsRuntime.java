/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import com.azure.resourcemanager.appservice.models.JavaVersion;
import com.azure.resourcemanager.appservice.models.WebAppMajorVersion;
import com.azure.resourcemanager.appservice.models.WebAppMinorVersion;
import com.azure.resourcemanager.appservice.models.WebAppRuntimeSettings;
import com.azure.resourcemanager.appservice.models.WebAppRuntimes;
import com.azure.resourcemanager.appservice.models.WebContainer;
import com.azure.resourcemanager.appservice.models.WindowsJavaContainerSettings;
import com.google.common.collect.Sets;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WebAppWindowsRuntime implements WebAppRuntime {
    public static final WebAppWindowsRuntime JAVASE_JAVA17 = new WebAppWindowsRuntime("Java SE", "Java 17");
    public static final WebAppWindowsRuntime JAVASE_JAVA11 = new WebAppWindowsRuntime("Java SE", "Java 11");
    public static final WebAppWindowsRuntime JAVASE_JAVA8 = new WebAppWindowsRuntime("Java SE", "Java 8");
    public static final WebAppWindowsRuntime TOMCAT10_JAVA17 = new WebAppWindowsRuntime("Tomcat 10.0", "Java 17");
    public static final WebAppWindowsRuntime TOMCAT10_JAVA11 = new WebAppWindowsRuntime("Tomcat 10.0", "Java 11");
    public static final WebAppWindowsRuntime TOMCAT10_JAVA8 = new WebAppWindowsRuntime("Tomcat 10.0", "Java 8");
    public static final WebAppWindowsRuntime TOMCAT9_JAVA17 = new WebAppWindowsRuntime("Tomcat 9.0", "Java 17");
    public static final WebAppWindowsRuntime TOMCAT9_JAVA11 = new WebAppWindowsRuntime("Tomcat 9.0", "Java 11");
    public static final WebAppWindowsRuntime TOMCAT9_JAVA8 = new WebAppWindowsRuntime("Tomcat 9.0", "Java 8");
    public static final WebAppWindowsRuntime TOMCAT85_JAVA17 = new WebAppWindowsRuntime("Tomcat 8.5", "Java 17");
    public static final WebAppWindowsRuntime TOMCAT85_JAVA11 = new WebAppWindowsRuntime("Tomcat 8.5", "Java 11");
    public static final WebAppWindowsRuntime TOMCAT85_JAVA8 = new WebAppWindowsRuntime("Tomcat 8.5", "Java 8");

    private static final AtomicReference<Boolean> loaded = new AtomicReference<>(Boolean.FALSE);
    private static final LinkedHashSet<WebAppWindowsRuntime> RUNTIMES = Sets.newLinkedHashSet(Arrays.asList(
        JAVASE_JAVA17, JAVASE_JAVA11, JAVASE_JAVA8,
        TOMCAT10_JAVA17, TOMCAT10_JAVA11, TOMCAT10_JAVA8,
        TOMCAT9_JAVA17, TOMCAT9_JAVA11, TOMCAT9_JAVA8,
        TOMCAT85_JAVA17, TOMCAT85_JAVA11, TOMCAT85_JAVA8
    ));

    @Getter
    @EqualsAndHashCode.Include
    private final OperatingSystem operatingSystem = OperatingSystem.WINDOWS;
    /**
     * container name in upper case, e.g. 'JAVA', 'TOMCAT', 'JBOSS', 'JETTY'
     */
    @Getter
    @EqualsAndHashCode.Include
    private final String containerName;
    /**
     * container version number, e.g. '17', '17.0.4', '1.8.202'
     */
    @Getter
    @EqualsAndHashCode.Include
    private final String containerVersionNumber;
    /**
     * java version number, e.g. '17', '17.0.4', '1.8.0_202', '1.8.0_202_ZULU'
     */
    @Getter
    @EqualsAndHashCode.Include
    private final String javaVersionNumber;
    private final String javaVersionDisplayText;
    @Getter
    private final boolean deprecatedOrHidden;

    private WebAppWindowsRuntime(@Nonnull WebAppMinorVersion webContainer, @Nonnull WebAppMinorVersion javaMinorVersion) {
        final WindowsJavaContainerSettings containerSettings = webContainer.stackSettings().windowsContainerSettings();
        final WebAppRuntimeSettings javaSettings = javaMinorVersion.stackSettings().windowsRuntimeSettings();
        this.deprecatedOrHidden = BooleanUtils.isTrue(containerSettings.isHidden())
            || BooleanUtils.isTrue(javaSettings.isHidden())
            || BooleanUtils.isTrue(containerSettings.isDeprecated())
            || BooleanUtils.isTrue(javaSettings.isDeprecated());
        this.containerName = containerSettings.javaContainer().toUpperCase();
        this.containerVersionNumber = StringUtils.equalsIgnoreCase(this.containerName, "Java") ? "SE" : containerSettings.javaContainerVersion().toUpperCase();
        this.javaVersionNumber = javaSettings.runtimeVersion().toUpperCase();
        this.javaVersionDisplayText = javaMinorVersion.displayText();
    }

    @SuppressWarnings("DataFlowIssue")
    public WebAppWindowsRuntime(final Map<String, Object> webContainer, final Map<String, Object> javaMinorVersion) {
        final Map<String, Object> containerSettings = Utils.get(webContainer, "$.stackSettings.windowsContainerSettings");
        final Map<String, Object> javaSettings = Utils.get(javaMinorVersion, "$.stackSettings.windowsRuntimeSettings");
        this.deprecatedOrHidden = BooleanUtils.isTrue(Utils.get(containerSettings, "$.isHidden"))
            || BooleanUtils.isTrue(Utils.get(javaSettings, "$.isHidden"))
            || BooleanUtils.isTrue(Utils.get(containerSettings, "$.isDeprecated"))
            || BooleanUtils.isTrue(Utils.get(javaSettings, "$.isDeprecated"));
        this.containerName = ((String) Utils.get(containerSettings, "$.javaContainer")).toUpperCase();
        this.containerVersionNumber = StringUtils.equalsIgnoreCase(this.containerName, "Java") ? "SE" : ((String) Utils.get(containerSettings, "$.javaContainerVersion")).toUpperCase();
        this.javaVersionNumber = ((String) Utils.get(javaSettings, "$.runtimeVersion")).toUpperCase();
        this.javaVersionDisplayText = Utils.get(javaMinorVersion, "$.displayText");
    }

    WebAppWindowsRuntime(final String containerUserText, final String javaVersionUserText) {
        final String[] containerParts = containerUserText.split(" ");
        final String[] javaParts = javaVersionUserText.split(" ");
        this.deprecatedOrHidden = false;
        this.containerName = containerParts[0];
        this.containerVersionNumber = StringUtils.equalsIgnoreCase(this.containerName, "Java") ? "SE" : containerParts[1].toUpperCase();
        this.javaVersionNumber = javaParts[1];
        this.javaVersionDisplayText = javaVersionUserText;
    }

    @Override
    public WebContainer getWebContainer() {
        if ("SE".equalsIgnoreCase(this.getContainerVersionNumber())) { // Java SE web containers can only be major versions.
            return WebContainer.fromString(String.format("java %d", this.getJavaMajorVersionNumber()));
        }
        return WebAppRuntime.super.getWebContainer();
    }

    @Nullable
    public static WebAppWindowsRuntime fromContainerAndJavaVersion(final String containerName, String containerVersionNumber, final JavaVersion javaVersion) {
        containerVersionNumber = StringUtils.equalsIgnoreCase(containerName, "java") ? "SE" : containerVersionNumber;
        final String containerUserText = String.format("%s %s", containerName, containerVersionNumber);
        final String javaVersionUserText = String.format("java %s", javaVersion.toString());
        return fromContainerAndJavaVersionUserText(containerUserText, javaVersionUserText);
    }

    @Nullable
    public static WebAppWindowsRuntime fromContainerAndJavaVersionUserText(final String containerUserText, final String javaVersionUserText) {
        final String finalContainerUserText = StringUtils.startsWithIgnoreCase(containerUserText, "java ") ? "Java SE" : containerUserText;
        return RUNTIMES.stream().filter(r -> {
            final String containerText = String.format("%s %s", r.containerName, r.containerVersionNumber);
            final String javaVersionText = String.format("java %s", r.javaVersionNumber);
            if (StringUtils.equalsAnyIgnoreCase(r.javaVersionNumber, "8", "1.8")) {
                return StringUtils.equalsIgnoreCase(finalContainerUserText, containerText) &&
                    StringUtils.equalsAnyIgnoreCase(javaVersionUserText, "java 1.8", "java 8");
            }
            return StringUtils.equalsIgnoreCase(finalContainerUserText, containerText) &&
                StringUtils.equalsAnyIgnoreCase(javaVersionUserText, javaVersionText, r.javaVersionDisplayText);
        }).findFirst().orElse(null);
    }

    public static List<WebAppWindowsRuntime> getAllRuntimes() {
        return new ArrayList<>(RUNTIMES);
    }

    @Nonnull
    public static List<WebAppWindowsRuntime> getMajorRuntimes() {
        return RUNTIMES.stream().filter(r -> !r.isDeprecatedOrHidden() && !r.isMinorVersion()).collect(Collectors.toList());
    }

    public static boolean isLoaded() {
        return loaded.get() == Boolean.TRUE;
    }

    public static void loadAllWebAppWindowsRuntimes(List<WebAppMajorVersion> javaVersions, List<WebAppMajorVersion> containerVersions) {
        if (!loaded.compareAndSet(Boolean.FALSE, null)) {
            return;
        }
        RUNTIMES.clear();
        for (final WebAppMajorVersion javaMajorVersion : javaVersions) {
            for (final WebAppMinorVersion javaMinorVersion : javaMajorVersion.minorVersions()) {
                for (final WebAppMajorVersion containerMajorVersion : containerVersions) {
                    for (final WebAppMinorVersion containerMinorVersion : containerMajorVersion.minorVersions()) {
                        final boolean containerSettings = Optional.ofNullable(containerMinorVersion).map(WebAppMinorVersion::stackSettings)
                            .map(WebAppRuntimes::windowsContainerSettings).map(WindowsJavaContainerSettings::javaContainer).isPresent();
                        final boolean javaSettings = Optional.ofNullable(javaMinorVersion).map(WebAppMinorVersion::stackSettings)
                            .map(WebAppRuntimes::windowsRuntimeSettings).map(WebAppRuntimeSettings::runtimeVersion).isPresent();
                        if (containerSettings && javaSettings) {
                            RUNTIMES.add(new WebAppWindowsRuntime(containerMinorVersion, javaMinorVersion));
                        }
                    }
                }
            }
        }
        loaded.compareAndSet(null, Boolean.TRUE);
    }

    @SuppressWarnings("DataFlowIssue")
    public static void loadAllWebAppWindowsRuntimesFromMap(final List<Map<String, Object>> javaVersions, final List<Map<String, Object>> containerVersions) {
        if (!loaded.compareAndSet(Boolean.FALSE, null)) {
            return;
        }
        RUNTIMES.clear();
        for (final Map<String, Object> javaMajorVersion : javaVersions) {
            for (final Map<String, Object> javaMinorVersion : Utils.<List<Map<String, Object>>>get(javaMajorVersion, "$.minorVersions")) {
                for (final Map<String, Object> containerMajorVersion : containerVersions) {
                    for (final Map<String, Object> containerMinorVersion : Utils.<List<Map<String, Object>>>get(containerMajorVersion, "$.minorVersions")) {
                        final boolean containerSettings = Objects.nonNull(Utils.get(containerMinorVersion, "$.stackSettings.windowsContainerSettings.javaContainer"));
                        final boolean javaSettings = Objects.nonNull(Utils.get(javaMinorVersion, "$.stackSettings.windowsRuntimeSettings.runtimeVersion"));
                        if (containerSettings && javaSettings) {
                            RUNTIMES.add(new WebAppWindowsRuntime(containerMinorVersion, javaMinorVersion));
                        }
                    }
                }
            }
        }
        loaded.compareAndSet(null, Boolean.TRUE);
    }

    public String toString() {
        return String.format("Windows: %s - %s", this.getContainerUserText(), this.getJavaVersionUserText());
    }
}
