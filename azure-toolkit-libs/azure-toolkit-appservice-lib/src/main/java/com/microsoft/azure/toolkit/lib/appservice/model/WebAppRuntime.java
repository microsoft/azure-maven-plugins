package com.microsoft.azure.toolkit.lib.appservice.model;

import com.azure.resourcemanager.appservice.models.JavaVersion;
import com.azure.resourcemanager.appservice.models.WebContainer;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public interface WebAppRuntime extends Runtime {

    WebAppRuntime DOCKER = WebAppDockerRuntime.INSTANCE;
    WebAppRuntime DEFAULT_TOMCAT_RUNTIME = WebAppLinuxRuntime.TOMCAT10_JAVA17;
    WebAppRuntime DEFAULT_JAVASE_RUNTIME = WebAppLinuxRuntime.JAVASE_JAVA17;
    WebAppRuntime DEFAULT_JBOSS_RUNTIME = WebAppLinuxRuntime.JBOSS7_JAVA17;
    String CONTAINER_JAVA_SE = "Java SE";

    /**
     * @return container name in upper case, e.g. 'JAVA', 'TOMCAT', 'JBOSS', 'JETTY'
     */
    @Nonnull
    String getContainerName();

    /**
     * @return container version number, e.g. '17', '17.0.4', '1.8.202', be careful of `SE`
     */
    @Nonnull
    String getContainerVersionNumber();

    /**
     * @return java version number, e.g. '17', '17.0.4', '1.8.0_202', '1.8.0_202_ZULU'
     */
    @Nonnull
    String getJavaVersionNumber();

    /**
     * @return java major version number, e.g. '7', '8', '11', '17'
     */
    default int getJavaMajorVersionNumber() {
        if (this.isDocker()) {
            return 0;
        }
        final String v = this.getJavaVersionNumber();
        return Integer.parseInt(StringUtils.startsWithIgnoreCase(v, "1.") ? String.valueOf(v.charAt(2)) : v);
    }

    default WebContainer getWebContainer() {
        if (this.isDocker()) {
            return WebContainer.fromString("docker");
        }
        return WebContainer.fromString(String.format("%s %s", getContainerName().toLowerCase(), getContainerVersionNumber()));
    }

    default JavaVersion getJavaVersion() {
        if (this.isDocker()) {
            return JavaVersion.fromString("docker");
        }
        return JavaVersion.fromString(this.getJavaVersionNumber());
    }

    @Override
    default boolean isMinorVersion() {
        if (this.isDocker()) {
            return false;
        }
        final Pattern EXCLUDE_PATTERN = Pattern.compile("\\..*\\.");
        return EXCLUDE_PATTERN.matcher(this.getJavaVersionNumber()).matches() || EXCLUDE_PATTERN.matcher(this.getContainerVersionNumber()).matches();
    }

    default boolean isJavaSE() {
        return StringUtils.startsWithIgnoreCase(this.getContainerName(), "java");
    }

    default boolean isJBoss() {
        return StringUtils.startsWithIgnoreCase(this.getContainerName(), "jboss");
    }

    default boolean isTomcat() {
        return StringUtils.startsWithIgnoreCase(this.getContainerName(), "tomcat");
    }

    default String getJavaVersionUserText() {
        if (this.isDocker()) {
            return "";
        }
        if (StringUtils.equalsAnyIgnoreCase(getJavaVersionNumber(), "1.8", "8")) {
            return "Java 8";
        }
        return String.format("Java %s", getJavaVersionNumber());
    }

    default String getContainerUserText() {
        if (this.isDocker()) {
            return "Docker";
        }
        return String.format("%s %s", StringUtils.capitalize(getContainerName().toLowerCase()), getContainerVersionNumber());
    }

    default List<PricingTier> getPricingTiers() {
        return getPricingTiers(this.getOperatingSystem(), this.getContainerUserText());
    }

    static WebAppRuntime fromUserText(final String os, final String containerUserText, String javaVersionUserText) {
        if (StringUtils.equalsIgnoreCase(os, "docker")) {
            return WebAppDockerRuntime.INSTANCE;
        }
        if (StringUtils.equalsIgnoreCase(os, "windows")) {
            return WebAppWindowsRuntime.fromContainerAndJavaVersionUserText(containerUserText, javaVersionUserText);
        }
        return WebAppLinuxRuntime.fromContainerAndJavaVersionUserText(containerUserText, javaVersionUserText);
    }

    static List<WebAppRuntime> getMajorRuntimes() {
        return Stream.concat(
                Stream.of(WebAppDockerRuntime.INSTANCE), Stream.concat(
                    WebAppWindowsRuntime.getMajorRuntimes().stream(),
                    WebAppLinuxRuntime.getMajorRuntimes().stream()))
            .collect(Collectors.toList());
    }

    static List<WebAppRuntime> getAllRuntimes() {
        return Stream.concat(
                Stream.of(WebAppDockerRuntime.INSTANCE), Stream.concat(
                    WebAppWindowsRuntime.getAllRuntimes().stream(),
                    WebAppLinuxRuntime.getAllRuntimes().stream()))
            .collect(Collectors.toList());
    }

    static List<PricingTier> getPricingTiers(OperatingSystem os, String containerUserText) {
        if (StringUtils.startsWithIgnoreCase(containerUserText, "jboss")) {
            return Arrays.asList(PricingTier.PREMIUM_P1V3, PricingTier.PREMIUM_P2V3, PricingTier.PREMIUM_P3V3);
        }
        // Linux and docker app service uses linux as the os of app service plan.
        // This is a workaround for https://github.com/Azure/azure-libraries-for-java/issues/660
        // Linux app service didn't support P1,P2,P3 pricing tier.
        final ArrayList<PricingTier> tiers = new ArrayList<>(PricingTier.values());
        if (os == OperatingSystem.LINUX || os == OperatingSystem.DOCKER) {
            tiers.remove(PricingTier.PREMIUM_P1);
            tiers.remove(PricingTier.PREMIUM_P2);
            tiers.remove(PricingTier.PREMIUM_P3);
        }
        return tiers;
    }
}
