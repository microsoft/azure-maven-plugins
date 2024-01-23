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
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WebAppWindowsRuntime implements WebAppRuntime {
    public static final WebAppWindowsRuntime JAVASE_JAVA17 = new WebAppWindowsRuntime("Java SE", "Java 17");
    public static final WebAppWindowsRuntime JAVASE_JAVA11 = new WebAppWindowsRuntime("Java SE", "Java 11");
    public static final WebAppWindowsRuntime JAVASE_JAVA8 = new WebAppWindowsRuntime("Java SE", "Java 1.8");
    public static final WebAppWindowsRuntime TOMCAT10_JAVA17 = new WebAppWindowsRuntime("Tomcat 10.0", "Java 17");
    public static final WebAppWindowsRuntime TOMCAT10_JAVA11 = new WebAppWindowsRuntime("Tomcat 10.0", "Java 11");
    public static final WebAppWindowsRuntime TOMCAT10_JAVA8 = new WebAppWindowsRuntime("Tomcat 10.0", "Java 1.8");
    public static final WebAppWindowsRuntime TOMCAT9_JAVA17 = new WebAppWindowsRuntime("Tomcat 9.0", "Java 17");
    public static final WebAppWindowsRuntime TOMCAT9_JAVA11 = new WebAppWindowsRuntime("Tomcat 9.0", "Java 11");
    public static final WebAppWindowsRuntime TOMCAT9_JAVA8 = new WebAppWindowsRuntime("Tomcat 9.0", "Java 1.8");
    public static final WebAppWindowsRuntime TOMCAT85_JAVA17 = new WebAppWindowsRuntime("Tomcat 8.5", "Java 17");
    public static final WebAppWindowsRuntime TOMCAT85_JAVA11 = new WebAppWindowsRuntime("Tomcat 8.5", "Java 11");
    public static final WebAppWindowsRuntime TOMCAT85_JAVA8 = new WebAppWindowsRuntime("Tomcat 8.5", "Java 1.8");

    private static final AtomicReference<Boolean> loaded = new AtomicReference<>(Boolean.FALSE);
    private static final LinkedHashSet<WebAppWindowsRuntime> RUNTIMES = Sets.newLinkedHashSet(Arrays.asList(
        JAVASE_JAVA17, JAVASE_JAVA11, JAVASE_JAVA8,
        TOMCAT10_JAVA17, TOMCAT10_JAVA11, TOMCAT10_JAVA8,
        TOMCAT9_JAVA17, TOMCAT9_JAVA11, TOMCAT9_JAVA8,
        TOMCAT85_JAVA17, TOMCAT85_JAVA11, TOMCAT85_JAVA8
    ));

    @EqualsAndHashCode.Include
    private final OperatingSystem operatingSystem = OperatingSystem.WINDOWS;
    /**
     * container name in upper case, e.g. 'JAVA', 'TOMCAT', 'JBOSS', 'JETTY'
     */
    @EqualsAndHashCode.Include
    private final String containerName;
    /**
     * container version number, e.g. 'SE', '8.5', '9.0', '10.0', '7'
     */
    @EqualsAndHashCode.Include
    private final String containerVersionNumber;
    /**
     * java version number, e.g. '1.8', '17', '17.0.4', '1.8.0_202', '1.8.0_202_ZULU'
     */
    @EqualsAndHashCode.Include
    private final String javaVersionNumber;
    private final boolean deprecated;
    private final boolean hidden;
    private final boolean earlyAccess;
    private final boolean autoUpdate;
    private final boolean preview;
    @Nullable
    private final OffsetDateTime endOfLifeDate;

    private WebAppWindowsRuntime(@Nonnull WebAppMinorVersion webContainer, @Nonnull WebAppMinorVersion javaMinorVersion) {
        final WindowsJavaContainerSettings containerSettings = webContainer.stackSettings().windowsContainerSettings();
        final WebAppRuntimeSettings javaSettings = javaMinorVersion.stackSettings().windowsRuntimeSettings();
        this.deprecated = BooleanUtils.isTrue(containerSettings.isDeprecated()) || BooleanUtils.isTrue(javaSettings.isDeprecated());
        this.hidden = BooleanUtils.isTrue(containerSettings.isHidden()) || BooleanUtils.isTrue(javaSettings.isHidden());
        this.earlyAccess = BooleanUtils.isTrue(containerSettings.isEarlyAccess()) || BooleanUtils.isTrue(javaSettings.isEarlyAccess());
        this.autoUpdate = BooleanUtils.isTrue(containerSettings.isAutoUpdate()) || BooleanUtils.isTrue(javaSettings.isAutoUpdate());
        this.preview = BooleanUtils.isTrue(containerSettings.isPreview()) || BooleanUtils.isTrue(javaSettings.isPreview());
        final OffsetDateTime javaEndOfLifeDate = javaSettings.endOfLifeDate();
        final OffsetDateTime containerEndOfLifeDate = containerSettings.endOfLifeDate();
        this.endOfLifeDate = javaEndOfLifeDate == null ? containerEndOfLifeDate :
            containerEndOfLifeDate == null ? javaEndOfLifeDate :
                javaEndOfLifeDate.isAfter(containerEndOfLifeDate) ? containerEndOfLifeDate : javaEndOfLifeDate;
        this.containerName = containerSettings.javaContainer().toUpperCase();
        this.containerVersionNumber = StringUtils.equalsIgnoreCase(this.containerName, "Java") ? "SE" : containerSettings.javaContainerVersion().toUpperCase();
        this.javaVersionNumber = Runtime.extractAndFormalizeJavaVersionNumber(javaSettings.runtimeVersion().toUpperCase());
    }

    private WebAppWindowsRuntime(@Nonnull WebAppMinorVersion javaMinorVersion) {
        final WebAppRuntimeSettings javaSettings = javaMinorVersion.stackSettings().windowsRuntimeSettings();
        this.deprecated = BooleanUtils.isTrue(javaSettings.isDeprecated());
        this.hidden = BooleanUtils.isTrue(javaSettings.isHidden());
        this.earlyAccess = BooleanUtils.isTrue(javaSettings.isEarlyAccess());
        this.autoUpdate = BooleanUtils.isTrue(javaSettings.isAutoUpdate());
        this.preview = BooleanUtils.isTrue(javaSettings.isPreview());
        this.endOfLifeDate = javaSettings.endOfLifeDate();
        this.containerName = "JAVA";
        this.containerVersionNumber = "SE";
        this.javaVersionNumber = Runtime.extractAndFormalizeJavaVersionNumber(javaSettings.runtimeVersion().toUpperCase());
    }

    @SuppressWarnings("DataFlowIssue")
    private WebAppWindowsRuntime(final Map<String, Object> webContainer, final Map<String, Object> javaMinorVersion) {
        final Map<String, Object> containerSettings = Utils.get(webContainer, "$.stackSettings.windowsContainerSettings");
        final Map<String, Object> javaSettings = Utils.get(javaMinorVersion, "$.stackSettings.windowsRuntimeSettings");
        this.deprecated = BooleanUtils.isTrue(Utils.get(containerSettings, "$.isDeprecated")) || BooleanUtils.isTrue(Utils.get(javaSettings, "$.isDeprecated"));
        this.hidden = BooleanUtils.isTrue(Utils.get(containerSettings, "$.isHidden")) || BooleanUtils.isTrue(Utils.get(javaSettings, "$.isHidden"));
        this.earlyAccess = BooleanUtils.isTrue(Utils.get(containerSettings, "$.isEarlyAccess")) || BooleanUtils.isTrue(Utils.get(javaSettings, "$.isEarlyAccess"));
        this.autoUpdate = BooleanUtils.isTrue(Utils.get(containerSettings, "$.isAutoUpdate")) || BooleanUtils.isTrue(Utils.get(javaSettings, "$.isAutoUpdate"));
        this.preview = BooleanUtils.isTrue(Utils.get(containerSettings, "$.isPreview")) || BooleanUtils.isTrue(Utils.get(javaSettings, "$.isPreview"));
        final CharSequence javaEndOfLifeDateStr = Utils.get(javaSettings, "$.endOfLifeDate");
        final CharSequence containerEndOfLifeDateStr = Utils.get(containerSettings, "$.endOfLifeDate");
        final OffsetDateTime javaEndOfLifeDate = StringUtils.isBlank(javaEndOfLifeDateStr) ? null : OffsetDateTime.parse(javaEndOfLifeDateStr, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        final OffsetDateTime containerEndOfLifeDate = StringUtils.isBlank(containerEndOfLifeDateStr) ? null : OffsetDateTime.parse(containerEndOfLifeDateStr, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        this.endOfLifeDate = javaEndOfLifeDate == null ? containerEndOfLifeDate :
            containerEndOfLifeDate == null ? javaEndOfLifeDate :
                javaEndOfLifeDate.isAfter(containerEndOfLifeDate) ? containerEndOfLifeDate : javaEndOfLifeDate;
        this.containerName = ((String) Utils.get(containerSettings, "$.javaContainer")).toUpperCase();
        this.containerVersionNumber = StringUtils.equalsIgnoreCase(this.containerName, "Java") ? "SE" : ((String) Utils.get(containerSettings, "$.javaContainerVersion")).toUpperCase();
        this.javaVersionNumber = Runtime.extractAndFormalizeJavaVersionNumber(((String) Utils.get(javaSettings, "$.runtimeVersion")).toUpperCase());
    }

    @SuppressWarnings("DataFlowIssue")
    private WebAppWindowsRuntime(final Map<String, Object> javaMinorVersion) {
        final Map<String, Object> javaSettings = Utils.get(javaMinorVersion, "$.stackSettings.windowsRuntimeSettings");
        this.deprecated = BooleanUtils.isTrue(Utils.get(javaSettings, "$.isDeprecated"));
        this.hidden = BooleanUtils.isTrue(Utils.get(javaSettings, "$.isHidden"));
        this.earlyAccess = BooleanUtils.isTrue(Utils.get(javaSettings, "$.isEarlyAccess"));
        this.autoUpdate = BooleanUtils.isTrue(Utils.get(javaSettings, "$.isAutoUpdate"));
        this.preview = BooleanUtils.isTrue(Utils.get(javaSettings, "$.isPreview"));
        final CharSequence endOfLifeDateStr = Utils.get(javaSettings, "$.endOfLifeDate");
        this.endOfLifeDate = StringUtils.isBlank(endOfLifeDateStr) ? null : OffsetDateTime.parse(endOfLifeDateStr, DateTimeFormatter.ISO_ZONED_DATE_TIME);
        this.containerName = "JAVA";
        this.containerVersionNumber = "SE";
        this.javaVersionNumber = Runtime.extractAndFormalizeJavaVersionNumber(((String) Utils.get(javaSettings, "$.runtimeVersion")).toUpperCase());
    }

    private WebAppWindowsRuntime(final String containerUserText, final String javaVersionUserText) {
        final String[] containerParts = containerUserText.split(" ");
        final String[] javaParts = javaVersionUserText.split(" ");
        this.deprecated = false;
        this.hidden = false;
        this.earlyAccess = false;
        this.autoUpdate = false;
        this.preview = false;
        this.endOfLifeDate = null;
        this.containerName = containerParts[0];
        this.containerVersionNumber = StringUtils.equalsIgnoreCase(this.containerName, "Java") ? "SE" : containerParts[1].toUpperCase();
        this.javaVersionNumber = Runtime.extractAndFormalizeJavaVersionNumber(javaParts[1]);
    }

    @Override
    public WebContainer getWebContainer() {
        if ("SE".equalsIgnoreCase(this.getContainerVersionNumber())) { // Java SE web containers can only be major versions.
            return WebContainer.fromString(String.format("java %d", this.getJavaMajorVersionNumber()));
        }
        return WebAppRuntime.super.getWebContainer();
    }

    @Nullable
    public static WebAppWindowsRuntime fromContainerAndJavaVersion(final String containerName, String pContainerVersionNumber, final JavaVersion javaVersion) {
        final String containerVersionNumber = StringUtils.equalsIgnoreCase(containerName, "java") ? "SE" : pContainerVersionNumber;
        return getAllRuntimes().stream().filter(r -> StringUtils.equalsAnyIgnoreCase(javaVersion.toString(), r.javaVersionNumber) &&
                StringUtils.equalsIgnoreCase(containerName, r.containerName) &&
                StringUtils.equalsIgnoreCase(containerVersionNumber, r.containerVersionNumber))
            .findFirst().orElse(null);
    }

    @Nullable
    public static WebAppWindowsRuntime fromContainerAndJavaVersionUserText(final String pContainerUserText, String pJavaVersionUserText) {
        if (StringUtils.isBlank(pJavaVersionUserText)) {
            pJavaVersionUserText = DEFAULT_JAVA;
            AzureMessager.getMessager().warning(AzureString.format("The java version is not specified, use default version '%s'", DEFAULT_JAVA));
        }
        final String javaVersionNumber = Runtime.extractAndFormalizeJavaVersionNumber(pJavaVersionUserText);
        final String containerUserText = StringUtils.startsWithIgnoreCase(pContainerUserText, "java ") ? "Java SE" : pContainerUserText;
        return getAllRuntimes().stream().filter(r -> StringUtils.equalsAnyIgnoreCase(javaVersionNumber, r.javaVersionNumber) &&
                StringUtils.equalsIgnoreCase(containerUserText, String.format("%s %s", r.containerName, r.containerVersionNumber)))
            .findFirst().orElse(null);
    }

    public static List<WebAppWindowsRuntime> getAllRuntimes() {
        WebAppRuntime.tryLoadingAllRuntimes();
        return new ArrayList<>(RUNTIMES);
    }

    @Nonnull
    public static List<WebAppWindowsRuntime> getMajorRuntimes() {
        return getAllRuntimes().stream().filter(r -> !r.isDeprecated() && !r.isHidden() && r.isMajorVersion()).collect(Collectors.toList());
    }

    public static boolean isLoaded() {
        return loaded.get() == Boolean.TRUE;
    }

    public static boolean isLoading() {
        return loaded.get() == null;
    }

    public static void loadAllWebAppWindowsRuntimes(List<WebAppMajorVersion> javaVersions, List<WebAppMajorVersion> containerVersions) {
        if (!loaded.compareAndSet(Boolean.FALSE, null)) {
            return;
        }
        RUNTIMES.clear();
        for (final WebAppMajorVersion javaMajorVersion : javaVersions) {
            for (final WebAppMinorVersion javaMinorVersion : javaMajorVersion.minorVersions()) {
                if (Optional.ofNullable(javaMinorVersion).map(WebAppMinorVersion::stackSettings).map(WebAppRuntimes::windowsRuntimeSettings).map(WebAppRuntimeSettings::runtimeVersion).isPresent()) {
                    for (final WebAppMajorVersion containerMajorVersion : containerVersions) {
                        if (StringUtils.startsWithIgnoreCase(containerMajorVersion.value(), "java")) {
                            RUNTIMES.add(new WebAppWindowsRuntime(javaMinorVersion));
                        } else {
                            for (final WebAppMinorVersion containerMinorVersion : containerMajorVersion.minorVersions()) {
                                if (Optional.ofNullable(containerMinorVersion).map(WebAppMinorVersion::stackSettings).map(WebAppRuntimes::windowsContainerSettings).map(WindowsJavaContainerSettings::javaContainer).isPresent()) {
                                    RUNTIMES.add(new WebAppWindowsRuntime(containerMinorVersion, javaMinorVersion));
                                }
                            }
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
                if (Objects.nonNull(Utils.get(javaMinorVersion, "$.stackSettings.windowsRuntimeSettings.runtimeVersion"))) {
                    for (final Map<String, Object> containerMajorVersion : containerVersions) {
                        if (StringUtils.startsWithIgnoreCase((CharSequence) containerMajorVersion.get("value"), "java")) {
                            RUNTIMES.add(new WebAppWindowsRuntime(javaMinorVersion));
                        } else {
                            for (final Map<String, Object> containerMinorVersion : Utils.<List<Map<String, Object>>>get(containerMajorVersion, "$.minorVersions")) {
                                if (Objects.nonNull(Utils.get(containerMinorVersion, "$.stackSettings.windowsContainerSettings.javaContainer"))) {
                                    RUNTIMES.add(new WebAppWindowsRuntime(containerMinorVersion, javaMinorVersion));
                                }
                            }
                        }
                    }
                }
            }
        }
        loaded.compareAndSet(null, Boolean.TRUE);
    }

    public String toString() {
        return String.format("Windows | %s | %s", this.getContainerUserText(), this.getJavaVersionUserText());
    }
}
