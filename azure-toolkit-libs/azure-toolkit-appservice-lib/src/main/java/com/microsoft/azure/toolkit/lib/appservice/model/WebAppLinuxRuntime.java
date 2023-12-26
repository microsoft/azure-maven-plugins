/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.model;

import com.azure.resourcemanager.appservice.models.LinuxJavaContainerSettings;
import com.azure.resourcemanager.appservice.models.RuntimeStack;
import com.azure.resourcemanager.appservice.models.WebAppMajorVersion;
import com.azure.resourcemanager.appservice.models.WebAppMinorVersion;
import com.google.common.collect.Sets;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import com.microsoft.azure.toolkit.lib.common.utils.Utils;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.MethodUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@EqualsAndHashCode(onlyExplicitlyIncluded = true)
public class WebAppLinuxRuntime implements WebAppRuntime {
    public static final WebAppLinuxRuntime JAVASE_JAVA17 = new WebAppLinuxRuntime("JAVA|17-java17", "Java 17");
    public static final WebAppLinuxRuntime JAVASE_JAVA11 = new WebAppLinuxRuntime("JAVA|11-java11", "Java 11");
    public static final WebAppLinuxRuntime JAVASE_JAVA8 = new WebAppLinuxRuntime("JAVA|8-jre8", "Java 1.8");
    public static final WebAppLinuxRuntime TOMCAT10_JAVA17 = new WebAppLinuxRuntime("TOMCAT|10.0-java17", "Java 17");
    public static final WebAppLinuxRuntime TOMCAT10_JAVA11 = new WebAppLinuxRuntime("TOMCAT|10.0-java11", "Java 11");
    public static final WebAppLinuxRuntime TOMCAT10_JAVA8 = new WebAppLinuxRuntime("TOMCAT|10.0-jre8", "Java 1.8");
    public static final WebAppLinuxRuntime TOMCAT9_JAVA17 = new WebAppLinuxRuntime("TOMCAT|9.0-java17", "Java 17");
    public static final WebAppLinuxRuntime TOMCAT9_JAVA11 = new WebAppLinuxRuntime("TOMCAT|9.0-java11", "Java 11");
    public static final WebAppLinuxRuntime TOMCAT9_JAVA8 = new WebAppLinuxRuntime("TOMCAT|9.0-jre8", "Java 1.8");
    public static final WebAppLinuxRuntime TOMCAT85_JAVA11 = new WebAppLinuxRuntime("TOMCAT|8.5-java11", "Java 11");
    public static final WebAppLinuxRuntime TOMCAT85_JAVA8 = new WebAppLinuxRuntime("TOMCAT|8.5-jre8", "Java 1.8");
    public static final WebAppLinuxRuntime JBOSS7_JAVA17 = new WebAppLinuxRuntime("JBOSSEAP|7-java17", "Java 17");
    public static final WebAppLinuxRuntime JBOSS7_JAVA11 = new WebAppLinuxRuntime("JBOSSEAP|7-java11", "Java 11");
    public static final WebAppLinuxRuntime JBOSS7_JAVA8 = new WebAppLinuxRuntime("JBOSSEAP|7-java8", "Java 1.8");

    private static final AtomicReference<Boolean> loaded = new AtomicReference<>(Boolean.FALSE);
    private static final LinkedHashSet<WebAppLinuxRuntime> RUNTIMES = Sets.newLinkedHashSet(Arrays.asList(
        JAVASE_JAVA17, JAVASE_JAVA11, JAVASE_JAVA8,
        TOMCAT10_JAVA17, TOMCAT10_JAVA11, TOMCAT10_JAVA8,
        TOMCAT9_JAVA17, TOMCAT9_JAVA11, TOMCAT9_JAVA8,
        TOMCAT85_JAVA11, TOMCAT85_JAVA8,
        JBOSS7_JAVA17, JBOSS7_JAVA11, JBOSS7_JAVA8
    ));

    @Getter
    @EqualsAndHashCode.Include
    private final OperatingSystem operatingSystem = OperatingSystem.LINUX;
    @Getter
    @EqualsAndHashCode.Include
    private final String containerName;
    /**
     * container version number, e.g. 'SE', '8.5', '9.0', '10.0', '7'
     * be careful of `SE`; container version value used instead of linux fx string,
     */
    @Getter
    @EqualsAndHashCode.Include
    private final String containerVersionNumber;
    /**
     * java major version number only, e.g. '7', '8', '11', '17'
     */
    @Getter
    @EqualsAndHashCode.Include
    private final String javaVersionNumber;
    @Nonnull
    private final String fxString;
    @Getter
    private final boolean deprecatedOrHidden;

    private WebAppLinuxRuntime(@Nonnull WebAppMinorVersion container, @Nonnull WebAppMajorVersion javaVersion, @Nonnull String fxString) {
        final LinuxJavaContainerSettings containerSettings = container.stackSettings().linuxContainerSettings();
        final String[] parts = fxString.split("\\|", 2);
        this.fxString = fxString;
        this.deprecatedOrHidden = BooleanUtils.isTrue(containerSettings.isHidden())
            || BooleanUtils.isTrue(containerSettings.isDeprecated());
        this.containerName = parts[0].toUpperCase();
        this.containerVersionNumber = StringUtils.equalsIgnoreCase(this.containerName, "Java") ? "SE" : container.value().toUpperCase();
        // it's major version if container value is "SE", minor version otherwise, when container name is "Java"
        this.javaVersionNumber = Runtime.extractAndFormalizeJavaVersionNumber(StringUtils.equalsIgnoreCase(this.containerName, "Java") && !StringUtils.equalsIgnoreCase(container.value(), "SE") ?
            parts[1] : javaVersion.value().toUpperCase());
    }

    @SuppressWarnings("DataFlowIssue")
    private WebAppLinuxRuntime(final Map<String, Object> container, final Map<String, Object> javaVersion, final String fxString) {
        final Map<String, Object> containerSettings = Utils.get(container, "$.stackSettings.linuxContainerSettings");
        final String[] parts = fxString.split("\\|", 2);
        this.fxString = fxString;
        this.deprecatedOrHidden = BooleanUtils.isTrue(Utils.get(containerSettings, "$.isHidden"))
            || BooleanUtils.isTrue(Utils.get(containerSettings, "$.isDeprecated"));
        this.containerName = parts[0].toUpperCase();
        this.containerVersionNumber = StringUtils.equalsIgnoreCase(this.containerName, "Java") ? "SE" : ((String) Utils.get(container, "$.value")).toUpperCase();
        this.javaVersionNumber = Runtime.extractAndFormalizeJavaVersionNumber(StringUtils.equalsIgnoreCase(this.containerName, "Java") && !StringUtils.equalsIgnoreCase((String) container.get("value"), "SE") ?
            parts[1] : ((String) javaVersion.get("value")).toUpperCase()); // minor version in fx string may be 8u202 (-> 1.8.0_202), 17.0.4, etc.
    }

    private WebAppLinuxRuntime(final String fxString, final String javaVersionUserText) {
        this.fxString = fxString;
        final String[] fxStringParts = fxString.split("[|-]", 3);
        final String[] javaParts = javaVersionUserText.split(" ");
        this.deprecatedOrHidden = false;
        this.containerName = fxStringParts[0].toUpperCase();
        this.containerVersionNumber = StringUtils.equalsIgnoreCase(this.containerName, "Java") ? "SE" : fxStringParts[1].toUpperCase();
        this.javaVersionNumber = Runtime.extractAndFormalizeJavaVersionNumber(javaParts[1].toUpperCase());
    }

    public RuntimeStack toRuntimeStack() {
        final String[] parts = this.fxString.split("\\|", 2);
        return new RuntimeStack(parts[0], parts[1]);
    }

    @Nullable
    public static WebAppLinuxRuntime fromFxString(final String fxString) {
        return RUNTIMES.stream().filter(runtime -> StringUtils.equals(runtime.fxString, fxString)).findFirst().orElse(null);
    }

    @Nullable
    public static WebAppLinuxRuntime fromContainerAndJavaVersionUserText(final String pContainerUserText, String pJavaVersionUserText) {
        if (StringUtils.isBlank(pJavaVersionUserText)) {
            pJavaVersionUserText = DEFAULT_JAVA;
            AzureMessager.getMessager().warning(AzureString.format("The java version is not specified, use default version '%s'", DEFAULT_JAVA));
        }
        final String javaVersionNumber = Runtime.extractAndFormalizeJavaVersionNumber(pJavaVersionUserText);
        final String containerUserText = StringUtils.startsWithIgnoreCase(pContainerUserText, "java ") ? "Java SE" : pContainerUserText;
        return RUNTIMES.stream().filter(r -> StringUtils.equalsAnyIgnoreCase(javaVersionNumber, r.javaVersionNumber) &&
                StringUtils.equalsIgnoreCase(containerUserText, String.format("%s %s", r.containerName, r.containerVersionNumber)))
            .findFirst().orElse(null);
    }

    public static List<WebAppLinuxRuntime> getAllRuntimes() {
        return new ArrayList<>(RUNTIMES);
    }

    @Nonnull
    public static List<WebAppLinuxRuntime> getMajorRuntimes() {
        return RUNTIMES.stream().filter(r -> !r.isDeprecatedOrHidden() && !r.isMinorVersion()).collect(Collectors.toList());
    }

    public static boolean isLoaded() {
        return loaded.get() == Boolean.TRUE;
    }

    public static void loadAllWebAppLinuxRuntimes(List<WebAppMajorVersion> javaMajorVersions, List<WebAppMajorVersion> containerMajorVersions) {
        if (!loaded.compareAndSet(Boolean.FALSE, null)) {
            return;
        }

        RUNTIMES.clear();
        for (final WebAppMajorVersion containerMajorVersion : containerMajorVersions) {
            for (final WebAppMinorVersion containerMinorVersion : containerMajorVersion.minorVersions()) {
                final LinuxJavaContainerSettings containerSettings = containerMinorVersion.stackSettings().linuxContainerSettings();
                if (Objects.nonNull(containerSettings)) {
                    for (final WebAppMajorVersion javaMajorVersion : javaMajorVersions) {
                        if (StringUtils.isNotBlank(javaMajorVersion.value())) {
                            try {
                                final String fxString = (String) MethodUtils.invokeMethod(containerSettings, String.format("java%sRuntime", javaMajorVersion.value()));
                                if (StringUtils.isNotBlank(fxString)) {
                                    RUNTIMES.add(new WebAppLinuxRuntime(containerMinorVersion, javaMajorVersion, fxString));
                                }
                            } catch (final NoSuchMethodException | IllegalAccessException | InvocationTargetException ignored) {
                            }
                        }
                    }
                }
            }
        }
        loaded.compareAndSet(null, Boolean.TRUE);
    }

    @SuppressWarnings("DataFlowIssue")
    public static void loadAllWebAppLinuxRuntimesFromMap(List<Map<String, Object>> javaMajorVersions, List<Map<String, Object>> containerMajorVersions) {
        if (!loaded.compareAndSet(Boolean.FALSE, null)) {
            return;
        }

        RUNTIMES.clear();
        for (final Map<String, Object> containerMajorVersion : containerMajorVersions) {
            for (final Map<String, Object> containerMinorVersion : Utils.<List<Map<String, Object>>>get(containerMajorVersion, "$.minorVersions")) {
                final Map<String, Object> containerSettings = Utils.get(containerMinorVersion, "$.stackSettings.linuxContainerSettings");
                if (Objects.nonNull(containerSettings)) {
                    for (final Map<String, Object> javaMajorVersion : javaMajorVersions) {
                        if (StringUtils.isNotBlank((CharSequence) javaMajorVersion.get("value"))) {
                            final String fxString = (String) containerSettings.get(String.format("java%sRuntime", javaMajorVersion.get("value").toString()));
                            if (StringUtils.isNotBlank(fxString)) {
                                RUNTIMES.add(new WebAppLinuxRuntime(containerMinorVersion, javaMajorVersion, fxString));
                            }
                        }
                    }
                }
            }
        }
        loaded.compareAndSet(null, Boolean.TRUE);
    }

    public String toString() {
        return String.format("Linux: %s - %s (%s)", this.getContainerUserText(), this.getJavaVersionUserText(), this.fxString);
    }
}
