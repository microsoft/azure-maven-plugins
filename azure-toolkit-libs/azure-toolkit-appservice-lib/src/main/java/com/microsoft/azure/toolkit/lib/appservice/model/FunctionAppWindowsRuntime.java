/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import com.azure.resourcemanager.appservice.models.FunctionAppMajorVersion;
import com.azure.resourcemanager.appservice.models.FunctionAppMinorVersion;
import com.azure.resourcemanager.appservice.models.FunctionAppRuntimeSettings;
import com.azure.resourcemanager.appservice.models.FunctionAppRuntimes;
import com.azure.resourcemanager.appservice.models.JavaVersion;
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
public class FunctionAppWindowsRuntime implements FunctionAppRuntime {
    public static final FunctionAppWindowsRuntime FUNCTION_JAVA17 = new FunctionAppWindowsRuntime("Java 17");
    public static final FunctionAppWindowsRuntime FUNCTION_JAVA11 = new FunctionAppWindowsRuntime("Java 11");
    public static final FunctionAppWindowsRuntime FUNCTION_JAVA8 = new FunctionAppWindowsRuntime("Java 1.8");
    private static final LinkedHashSet<FunctionAppWindowsRuntime> RUNTIMES = Sets.newLinkedHashSet(Arrays.asList(FUNCTION_JAVA17, FUNCTION_JAVA11, FUNCTION_JAVA8));

    private static final AtomicReference<Boolean> loaded = new AtomicReference<>(Boolean.FALSE);
    @EqualsAndHashCode.Include
    private final OperatingSystem operatingSystem = OperatingSystem.WINDOWS;
    /**
     * java version number, e.g. '1.8', '11', '17'
     */
    @Nonnull
    @EqualsAndHashCode.Include
    private final String javaVersionNumber;
    private final boolean deprecated;
    private final boolean hidden;
    private final boolean earlyAccess;
    private final boolean autoUpdate;
    private final boolean preview;
    @Nullable
    private final OffsetDateTime endOfLifeDate;

    private FunctionAppWindowsRuntime(@Nonnull FunctionAppMinorVersion javaVersion) {
        final FunctionAppRuntimeSettings settings = javaVersion.stackSettings().windowsRuntimeSettings();
        this.javaVersionNumber = Runtime.extractAndFormalizeJavaVersionNumber(settings.runtimeVersion());
        this.deprecated = BooleanUtils.isTrue(settings.isDeprecated());
        this.hidden = BooleanUtils.isTrue(settings.isHidden());
        this.earlyAccess = BooleanUtils.isTrue(settings.isEarlyAccess());
        this.autoUpdate = BooleanUtils.isTrue(settings.isAutoUpdate());
        this.preview = BooleanUtils.isTrue(settings.isPreview());
        this.endOfLifeDate = settings.endOfLifeDate();
    }

    private FunctionAppWindowsRuntime(@Nonnull Map<String, Object> javaVersion) {
        final Map<String, Object> settings = Utils.get(javaVersion, "$.stackSettings.windowsRuntimeSettings");
        this.javaVersionNumber = Runtime.extractAndFormalizeJavaVersionNumber(Objects.requireNonNull(Utils.get(settings, "$.runtimeVersion")));
        this.deprecated = BooleanUtils.isTrue(Utils.get(settings, "$.isDeprecated"));
        this.hidden = BooleanUtils.isTrue(Utils.get(settings, "$.isHidden"));
        this.earlyAccess = BooleanUtils.isTrue(Utils.get(settings, "$.isEarlyAccess"));
        this.autoUpdate = BooleanUtils.isTrue(Utils.get(settings, "$.isAutoUpdate"));
        this.preview = BooleanUtils.isTrue(Utils.get(settings, "$.isPreview"));
        final CharSequence endOfLifeDateStr = Utils.get(settings, "$.endOfLifeDate");
        this.endOfLifeDate = StringUtils.isBlank(endOfLifeDateStr) ? null : OffsetDateTime.parse(endOfLifeDateStr, DateTimeFormatter.ISO_ZONED_DATE_TIME);
    }

    private FunctionAppWindowsRuntime(@Nonnull String javaVersionUserText) {
        final String[] javaParts = javaVersionUserText.split(" ");
        this.javaVersionNumber = Runtime.extractAndFormalizeJavaVersionNumber(javaParts[1]);
        this.deprecated = false;
        this.hidden = false;
        this.earlyAccess = false;
        this.autoUpdate = false;
        this.preview = false;
        this.endOfLifeDate = null;
    }

    @Nullable
    public static FunctionAppWindowsRuntime fromJavaVersion(final JavaVersion javaVersion) {
        return RUNTIMES.stream()
            .filter(runtime -> StringUtils.equalsIgnoreCase(runtime.javaVersionNumber, javaVersion.toString()))
            .findFirst().orElse(null);
    }

    @Nullable
    public static FunctionAppWindowsRuntime fromJavaVersionUserText(String v) {
        if (StringUtils.isBlank(v)) {
            v = DEFAULT_JAVA;
            AzureMessager.getMessager().warning(AzureString.format("The java version is not specified, use default version '%s'", DEFAULT_JAVA));
        }
        return fromJavaVersion(JavaVersion.fromString(Runtime.extractAndFormalizeJavaVersionNumber(v)));
    }

    public static List<FunctionAppWindowsRuntime> getAllRuntimes() {
        return new ArrayList<>(RUNTIMES);
    }

    @Nonnull
    public static List<FunctionAppWindowsRuntime> getMajorRuntimes() {
        return RUNTIMES.stream().filter(r -> !r.isDeprecated() && !r.isHidden() && r.isMajorVersion()).collect(Collectors.toList());
    }

    public static boolean isLoaded() {
        return loaded.get() == Boolean.TRUE;
    }

    public static void loadAllFunctionAppWindowsRuntimes(List<FunctionAppMajorVersion> javaVersions) {
        if (!loaded.compareAndSet(Boolean.FALSE, null)) {
            return;
        }
        final List<FunctionAppMinorVersion> javaMinorVersions = javaVersions.stream()
            .flatMap(majorVersion -> majorVersion.minorVersions().stream())
            .collect(Collectors.toList());

        RUNTIMES.clear();
        javaMinorVersions.forEach(javaMinorVersion -> Optional.ofNullable(javaMinorVersion)
            .map(FunctionAppMinorVersion::stackSettings)
            .map(FunctionAppRuntimes::windowsRuntimeSettings)
            .map(FunctionAppRuntimeSettings::runtimeVersion)
            .ifPresent(s -> RUNTIMES.add(new FunctionAppWindowsRuntime(javaMinorVersion)))
        );
        loaded.compareAndSet(null, Boolean.TRUE);
    }

    @SuppressWarnings("DataFlowIssue")
    public static void loadAllFunctionAppWindowsRuntimesFromMap(final List<Map<String, Object>> javaVersions) {
        if (!loaded.compareAndSet(Boolean.FALSE, null)) {
            return;
        }
        final List<Map<String, Object>> javaMinorVersions = javaVersions.stream()
            .flatMap(majorVersion -> Utils.<List<Map<String, Object>>>get(majorVersion, "$.minorVersions").stream())
            .collect(Collectors.toList());

        RUNTIMES.clear();
        javaMinorVersions.forEach(javaMinorVersion -> {
            final String runtimeVersion = Utils.get(javaMinorVersion, "$.stackSettings.windowsRuntimeSettings.runtimeVersion");
            if (StringUtils.isNotBlank(runtimeVersion)) {
                RUNTIMES.add(new FunctionAppWindowsRuntime(javaMinorVersion));
            }
        });
        loaded.compareAndSet(null, Boolean.TRUE);
    }

    public String toString() {
        return String.format("Windows: %s", this.getJavaVersionUserText());
    }
}
