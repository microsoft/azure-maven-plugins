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
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Getter
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class FunctionAppLinuxRuntime implements FunctionAppRuntime {
    public static final FunctionAppLinuxRuntime FUNCTION_JAVA21 = new FunctionAppLinuxRuntime("Java|21");
    public static final FunctionAppLinuxRuntime FUNCTION_JAVA17 = new FunctionAppLinuxRuntime("Java|17");
    public static final FunctionAppLinuxRuntime FUNCTION_JAVA11 = new FunctionAppLinuxRuntime("Java|11");
    public static final FunctionAppLinuxRuntime FUNCTION_JAVA8 = new FunctionAppLinuxRuntime("Java|8");
    private static final LinkedHashSet<FunctionAppLinuxRuntime> RUNTIMES = new LinkedHashSet<>(Arrays.asList(FUNCTION_JAVA17, FUNCTION_JAVA11, FUNCTION_JAVA8, FUNCTION_JAVA21));

    private static final AtomicReference<Boolean> loaded = new AtomicReference<>(Boolean.FALSE);
    private final String fxString;
    @EqualsAndHashCode.Include
    private final OperatingSystem operatingSystem = OperatingSystem.LINUX;
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

    private FunctionAppLinuxRuntime(@Nonnull FunctionAppMinorVersion javaVersion) {
        final FunctionAppRuntimeSettings settings = javaVersion.stackSettings().linuxRuntimeSettings();
        this.fxString = settings.runtimeVersion();
        this.javaVersionNumber = Runtime.extractAndFormalizeJavaVersionNumber(this.fxString.split("\\|", 2)[1]);
        this.deprecated = BooleanUtils.isTrue(settings.isDeprecated());
        this.hidden = BooleanUtils.isTrue(settings.isHidden());
        this.earlyAccess = BooleanUtils.isTrue(settings.isEarlyAccess());
        this.autoUpdate = BooleanUtils.isTrue(settings.isAutoUpdate());
        this.preview = BooleanUtils.isTrue(settings.isPreview());
        this.endOfLifeDate = settings.endOfLifeDate();
    }

    private FunctionAppLinuxRuntime(final Map<String, Object> javaVersion) {
        final Map<String, Object> settings = Utils.get(javaVersion, "$.stackSettings.linuxRuntimeSettings");
        this.fxString = Utils.get(settings, "$.runtimeVersion");
        this.javaVersionNumber = Runtime.extractAndFormalizeJavaVersionNumber(Objects.requireNonNull(this.fxString).split("\\|", 2)[1]);
        this.deprecated = BooleanUtils.isTrue(Utils.get(settings, "$.isDeprecated"));
        this.hidden = BooleanUtils.isTrue(Utils.get(settings, "$.isHidden"));
        this.earlyAccess = BooleanUtils.isTrue(Utils.get(settings, "$.isEarlyAccess"));
        this.autoUpdate = BooleanUtils.isTrue(Utils.get(settings, "$.isAutoUpdate"));
        this.preview = BooleanUtils.isTrue(Utils.get(settings, "$.isPreview"));
        final CharSequence endOfLifeDateStr = Utils.get(settings, "$.endOfLifeDate");
        this.endOfLifeDate = StringUtils.isBlank(endOfLifeDateStr) ? null : OffsetDateTime.parse(endOfLifeDateStr, DateTimeFormatter.ISO_ZONED_DATE_TIME);
    }

    private FunctionAppLinuxRuntime(@Nonnull String fxString) {
        this.fxString = fxString;
        this.javaVersionNumber = Runtime.extractAndFormalizeJavaVersionNumber(this.fxString.split("\\|", 2)[1]);
        this.deprecated = false;
        this.hidden = false;
        this.earlyAccess = false;
        this.autoUpdate = false;
        this.preview = false;
        this.endOfLifeDate = null;
    }

    public FunctionRuntimeStack toFunctionRuntimeStack(String funcExtensionVersion) {
        return new FunctionRuntimeStack("java", funcExtensionVersion, this.fxString.toLowerCase());
    }

    @Nullable
    public static FunctionAppLinuxRuntime fromFxString(final String fxString) {
        return getAllRuntimes().stream().filter(runtime -> StringUtils.equalsIgnoreCase(fxString, runtime.fxString)).findFirst().orElse(null);
    }

    @Nullable
    public static FunctionAppLinuxRuntime fromJavaVersionUserText(String v) {
        if (StringUtils.isBlank(v)) {
            v = DEFAULT_JAVA;
            AzureMessager.getMessager().warning(AzureString.format("The java version is not specified, use default version '%s'", DEFAULT_JAVA));
        }
        final String javaVersionNumber = Runtime.extractAndFormalizeJavaVersionNumber(v);
        return getAllRuntimes().stream()
            .filter(runtime -> StringUtils.equalsIgnoreCase(runtime.javaVersionNumber, javaVersionNumber))
            .findFirst().orElse(null);
    }

    public static List<FunctionAppLinuxRuntime> getAllRuntimes() {
        FunctionAppRuntime.tryLoadingAllRuntimes();
        return new ArrayList<>(RUNTIMES);
    }

    @Nonnull
    public static List<FunctionAppLinuxRuntime> getMajorRuntimes() {
        return getAllRuntimes().stream().filter(r -> !r.isDeprecated() && !r.isHidden() && r.isMajorVersion()).collect(Collectors.toList());
    }

    public static boolean isLoaded() {
        return loaded.get() == Boolean.TRUE;
    }

    public static boolean isLoading() {
        return loaded.get() == null;
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
        return String.format("Linux | %s", this.getJavaVersionUserText());
    }
}
