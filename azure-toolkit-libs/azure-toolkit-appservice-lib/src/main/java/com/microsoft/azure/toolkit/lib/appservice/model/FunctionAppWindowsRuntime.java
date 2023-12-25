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
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
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
public class FunctionAppWindowsRuntime implements FunctionAppRuntime {
    public static final FunctionAppWindowsRuntime FUNCTION_JAVA17 = new FunctionAppWindowsRuntime("Java 17");
    public static final FunctionAppWindowsRuntime FUNCTION_JAVA11 = new FunctionAppWindowsRuntime("Java 11");
    public static final FunctionAppWindowsRuntime FUNCTION_JAVA8 = new FunctionAppWindowsRuntime("Java 8");
    private static final LinkedHashSet<FunctionAppWindowsRuntime> RUNTIMES = Sets.newLinkedHashSet(Arrays.asList(FUNCTION_JAVA17, FUNCTION_JAVA11, FUNCTION_JAVA8));

    private static final AtomicReference<Boolean> loaded = new AtomicReference<>(Boolean.FALSE);
    @Getter
    @EqualsAndHashCode.Include
    private final OperatingSystem operatingSystem = OperatingSystem.WINDOWS;
    /**
     * java version number, e.g. '1.8', '11', '17'
     */
    @Getter
    @Nonnull
    @EqualsAndHashCode.Include
    private final String javaVersionNumber;
    @Getter
    private final boolean deprecatedOrHidden;
    private final String javaVersionDisplayText;

    private FunctionAppWindowsRuntime(@Nonnull FunctionAppMinorVersion javaVersion) {
        final FunctionAppRuntimeSettings settings = javaVersion.stackSettings().windowsRuntimeSettings();
        this.javaVersionNumber = settings.runtimeVersion();
        this.javaVersionDisplayText = javaVersion.displayText();
        this.deprecatedOrHidden = BooleanUtils.isTrue(settings.isDeprecated()) || BooleanUtils.isTrue(settings.isHidden());
    }

    private FunctionAppWindowsRuntime(@Nonnull Map<String, Object> javaVersion) {
        final Map<String, Object> settings = Utils.get(javaVersion, "$.stackSettings.windowsRuntimeSettings");
        this.javaVersionNumber = Objects.requireNonNull(Utils.get(settings, "$.runtimeVersion"));
        this.javaVersionDisplayText = Utils.get(javaVersion, "$.displayText");
        this.deprecatedOrHidden = BooleanUtils.isTrue(Utils.get(settings, "$.isDeprecated")) || BooleanUtils.isTrue(Utils.get(settings, "$.isHidden"));
    }

    private FunctionAppWindowsRuntime(@Nonnull String javaVersionUserText) {
        final String[] javaParts = javaVersionUserText.split(" ");
        this.javaVersionNumber = javaParts[1];
        this.javaVersionDisplayText = javaVersionUserText;
        this.deprecatedOrHidden = false;
    }

    @Nullable
    public static FunctionAppWindowsRuntime fromJavaVersion(final JavaVersion javaVersion) {
        return fromJavaVersionUserText(String.format("Java %s", javaVersion.toString()));
    }

    @Nullable
    public static FunctionAppWindowsRuntime fromJavaVersionUserText(String v) {
        if (StringUtils.isBlank(v)) {
            v = DEFAULT_JAVA;
            AzureMessager.getMessager().warning("The java version is not specified, use default version '%s'", DEFAULT_JAVA);
        }
        final String version = StringUtils.startsWithIgnoreCase(v, "Java") ? v : String.format("Java %s", v);
        return RUNTIMES.stream().filter(runtime -> {
            final String javaVersionUserText = String.format("Java %s", runtime.getJavaVersionNumber());
            return StringUtils.equalsAnyIgnoreCase(version, javaVersionUserText, runtime.javaVersionDisplayText);
        }).findFirst().orElse(null);
    }

    public static List<FunctionAppWindowsRuntime> getAllRuntimes() {
        return new ArrayList<>(RUNTIMES);
    }

    @Nonnull
    public static List<FunctionAppWindowsRuntime> getMajorRuntimes() {
        return RUNTIMES.stream().filter(r -> !r.isDeprecatedOrHidden() && !r.isMinorVersion()).collect(Collectors.toList());
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
