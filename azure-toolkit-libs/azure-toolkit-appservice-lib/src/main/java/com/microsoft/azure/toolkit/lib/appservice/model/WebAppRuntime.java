package com.microsoft.azure.toolkit.lib.appservice.model;

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
    String DEFAULT_JAVA = "Java 17";

    WebContainer JAVA_SE = WebContainer.fromString("Java SE");

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

    default WebContainer getWebContainer() {
        if (this.isDocker()) {
            return WebContainer.fromString("docker");
        }
        return WebContainer.fromString(String.format("%s %s", getContainerName().toLowerCase(), getContainerVersionNumber()));
    }

    @Override
    default boolean isMajorVersion() {
        if (this.isDocker()) {
            return true;
        }
        final Pattern EXCLUDE_PATTERN = Pattern.compile("\\..*\\.");
        return !EXCLUDE_PATTERN.matcher(this.getJavaVersionNumber()).matches() && !EXCLUDE_PATTERN.matcher(this.getContainerVersionNumber()).matches();
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

    default String getDisplayName() {
        if (this.isDocker()) {
            return "Docker";
        }
        return String.format("%s-%s-%s", this.getOperatingSystem().toString(), this.getJavaVersionUserText(), this.getContainerUserText());
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

    static WebAppRuntime getDefaultTomcatRuntime() {
        // use static method instead of constant to avoid cyclic dependency
        // https://stackoverflow.com/questions/41016957/java-interface-static-variable-is-not-initialized
        return WebAppLinuxRuntime.TOMCAT10_JAVA17;
    }

    static WebAppRuntime getDefaultJavaseRuntime() {
        // use static method instead of constant to avoid cyclic dependency
        // https://stackoverflow.com/questions/41016957/java-interface-static-variable-is-not-initialized
        return WebAppLinuxRuntime.JAVASE_JAVA17;
    }

    static WebAppRuntime getDefaultJbossRuntime() {
        // use static method instead of constant to avoid cyclic dependency
        // https://stackoverflow.com/questions/41016957/java-interface-static-variable-is-not-initialized
        return WebAppLinuxRuntime.JBOSS7_JAVA17;
    }

    static List<WebAppRuntime> getMajorRuntimes() {
        return Stream.concat(
            Stream.concat(
                WebAppLinuxRuntime.getMajorRuntimes().stream(),
                WebAppWindowsRuntime.getMajorRuntimes().stream()),
            Stream.of(WebAppDockerRuntime.INSTANCE)
        ).collect(Collectors.toList());
    }

    static List<WebAppRuntime> getAllRuntimes() {
        return Stream.concat(
            Stream.concat(
                WebAppLinuxRuntime.getAllRuntimes().stream(),
                WebAppWindowsRuntime.getAllRuntimes().stream()),
            Stream.of(WebAppDockerRuntime.INSTANCE)
        ).collect(Collectors.toList());
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
