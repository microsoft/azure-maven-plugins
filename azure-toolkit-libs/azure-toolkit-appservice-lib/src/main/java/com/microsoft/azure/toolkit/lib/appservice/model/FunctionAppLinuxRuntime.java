/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import com.azure.resourcemanager.appservice.models.FunctionAppMajorVersion;
import com.azure.resourcemanager.appservice.models.FunctionAppMinorVersion;
import com.azure.resourcemanager.appservice.models.FunctionAppRuntimeSettings;
import com.azure.resourcemanager.appservice.models.FunctionAppRuntimes;
import com.azure.resourcemanager.appservice.models.FunctionRuntimeStack;
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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FunctionAppLinuxRuntime implements FunctionAppRuntime {
    public static final FunctionAppLinuxRuntime FUNCTION_JAVA17 = new FunctionAppLinuxRuntime("Java 17", "Java|17");
    public static final FunctionAppLinuxRuntime FUNCTION_JAVA11 = new FunctionAppLinuxRuntime("Java 11", "Java|11");
    public static final FunctionAppLinuxRuntime FUNCTION_JAVA8 = new FunctionAppLinuxRuntime("Java 8", "Java|8");
    private static final LinkedHashSet<FunctionAppLinuxRuntime> RUNTIMES = new LinkedHashSet<>(Arrays.asList(FUNCTION_JAVA17, FUNCTION_JAVA11, FUNCTION_JAVA8));

    private static final AtomicReference<Boolean> loaded = new AtomicReference<>(Boolean.FALSE);
    private final String fxString;
    @Getter
    @EqualsAndHashCode.Include
    private final OperatingSystem operatingSystem = OperatingSystem.LINUX;
    /**
     * java version number, e.g. '8', '11', '17'
     */
    @Getter
    @Nonnull
    @EqualsAndHashCode.Include
    private final String javaVersionNumber;
    @Getter
    private final boolean deprecatedOrHidden;
    private final String javaVersionDisplayText;

    private FunctionAppLinuxRuntime(@Nonnull FunctionAppMinorVersion javaVersion) {
        final FunctionAppRuntimeSettings settings = javaVersion.stackSettings().linuxRuntimeSettings();
        this.fxString = settings.runtimeVersion();
        this.javaVersionNumber = this.fxString.split("\\|", 2)[1];
        this.javaVersionDisplayText = javaVersion.displayText();
        this.deprecatedOrHidden = BooleanUtils.isTrue(settings.isDeprecated()) || BooleanUtils.isTrue(settings.isHidden());
    }

    public FunctionAppLinuxRuntime(final Map<String, Object> javaVersion) {
        final Map<String, Object> settings = Utils.get(javaVersion, "$.stackSettings.linuxRuntimeSettings");
        this.fxString = Utils.get(settings, "$.runtimeVersion");
        this.javaVersionNumber = Objects.requireNonNull(this.fxString).split("\\|", 2)[1];
        this.javaVersionDisplayText = Utils.get(javaVersion, "$.displayText");
        this.deprecatedOrHidden = BooleanUtils.isTrue(Utils.get(settings, "$.isDeprecated")) || BooleanUtils.isTrue(Utils.get(settings, "$.isHidden"));
    }

    FunctionAppLinuxRuntime(@Nonnull String javaVersionUserText, @Nonnull String fxString) {
        this.fxString = fxString;
        this.javaVersionNumber = this.fxString.split("\\|", 2)[1];
        this.javaVersionDisplayText = javaVersionUserText;
        this.deprecatedOrHidden = false;
    }

    public FunctionRuntimeStack toFunctionRuntimeStack(String funcExtensionVersion) {
        return new FunctionRuntimeStack("java", funcExtensionVersion, this.fxString.toLowerCase());
    }

    @Nullable
    public static FunctionAppLinuxRuntime fromFxString(final String fxString) {
        return RUNTIMES.stream().filter(runtime -> StringUtils.equalsIgnoreCase(fxString, runtime.fxString)).findFirst().orElse(null);
    }

    @Nullable
    public static FunctionAppLinuxRuntime fromJavaVersionUserText(final String version) {
        return RUNTIMES.stream().filter(runtime -> {
            final String javaVersionUserText = String.format("Java %s", runtime.getJavaVersionNumber());
            return StringUtils.equalsAnyIgnoreCase(version, javaVersionUserText, runtime.javaVersionDisplayText);
        }).findFirst().orElse(null);
    }

    public static List<FunctionAppLinuxRuntime> getAllRuntimes() {
        return new ArrayList<>(RUNTIMES);
    }

    @Nonnull
    public static List<FunctionAppLinuxRuntime> getMajorRuntimes() {
        return RUNTIMES.stream().filter(r -> !r.isDeprecatedOrHidden() && !r.isMinorVersion()).collect(Collectors.toList());
    }

    public static boolean isLoaded() {
        return loaded.get() == Boolean.FALSE;
    }

    public static void loadAllFunctionAppLinuxRuntimes(List<FunctionAppMajorVersion> javaVersions) {
        if (!loaded.compareAndSet(Boolean.FALSE, null)) {
            return;
        }
        final Pattern EXCLUDE_PATTERN = Pattern.compile("\\..*\\.");
        final List<FunctionAppMinorVersion> javaMinorVersions = javaVersions.stream()
            .flatMap(majorVersion -> majorVersion.minorVersions().stream()
                .filter(minorVersion -> !EXCLUDE_PATTERN.matcher(minorVersion.value()).matches()))
            .collect(Collectors.toList());

        RUNTIMES.clear();
        javaMinorVersions.forEach(javaMinorVersion -> Optional.ofNullable(javaMinorVersion)
            .map(FunctionAppMinorVersion::stackSettings)
            .map(FunctionAppRuntimes::linuxRuntimeSettings)
            .map(FunctionAppRuntimeSettings::runtimeVersion)
            .ifPresent(s -> RUNTIMES.add(new FunctionAppLinuxRuntime(javaMinorVersion)))
        );
        loaded.compareAndSet(null, Boolean.TRUE);
    }

    @SuppressWarnings("DataFlowIssue")
    public static void loadAllFunctionAppLinuxRuntimesFromMap(final List<Map<String, Object>> javaVersions) {
        if (!loaded.compareAndSet(Boolean.FALSE, null)) {
            return;
        }
        final Pattern EXCLUDE_PATTERN = Pattern.compile("\\..*\\.");
        final List<Map<String, Object>> javaMinorVersions = javaVersions.stream()
            .flatMap(majorVersion -> Utils.<List<Map<String, Object>>>get(majorVersion, "$.minorVersions").stream()
                .filter(minorVersion -> !EXCLUDE_PATTERN.matcher(Utils.get(minorVersion, "$.value")).matches()))
            .collect(Collectors.toList());

        RUNTIMES.clear();
        javaMinorVersions.forEach(javaMinorVersion -> {
            final String runtimeVersion = Utils.get(javaMinorVersion, "$.stackSettings.linuxRuntimeSettings.runtimeVersion");
            if (StringUtils.isNotBlank(runtimeVersion)) {
                RUNTIMES.add(new FunctionAppLinuxRuntime(javaMinorVersion));
            }
        });
        loaded.compareAndSet(null, Boolean.TRUE);
    }

    public String toString() {
        return String.format("Linux: %s (%s)", this.getJavaVersionUserText(), this.fxString);
    }
}
