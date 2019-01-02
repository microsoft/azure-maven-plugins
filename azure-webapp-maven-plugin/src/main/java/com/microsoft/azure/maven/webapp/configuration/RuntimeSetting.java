/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.configuration;

import com.microsoft.azure.management.appservice.JavaVersion;
import com.microsoft.azure.management.appservice.RuntimeStack;
import com.microsoft.azure.management.appservice.WebContainer;
import org.apache.commons.collections4.BidiMap;
import org.apache.commons.collections4.bidimap.DualHashBidiMap;
import org.apache.maven.plugin.MojoExecutionException;
import org.codehaus.plexus.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime Setting
 */
public class RuntimeSetting {
    private static final String JRE_8 = "jre8";
    private static final String TOMCAT_8_5 = "tomcat 8.5";
    private static final String TOMCAT_9_0 = "tomcat 9.0";
    private static final String WILDFLY_14 = "wildfly 14";

    private static final BidiMap<String, RuntimeStack> runtimeStackMap = new DualHashBidiMap<>();

    protected String os;
    protected String javaVersion;
    protected String webContainer;
    protected String image;
    protected String serverId;
    protected String registryUrl;

    // init map from webContainer to runtime stack
    static {
        runtimeStackMap.put(JRE_8, RuntimeStack.JAVA_8_JRE8);
        runtimeStackMap.put(TOMCAT_8_5, RuntimeStack.TOMCAT_8_5_JRE8);
        runtimeStackMap.put(TOMCAT_9_0, RuntimeStack.TOMCAT_9_0_JRE8);
        runtimeStackMap.put(WILDFLY_14, RuntimeStack.WILDFLY_14_JRE8);
    }

    public String getOs() {
        return this.os;
    }

    public JavaVersion getJavaVersion() {
        return StringUtils.isEmpty(javaVersion) ? null : JavaVersion.fromString(javaVersion);
    }

    public RuntimeStack getLinuxRuntime() throws MojoExecutionException {
        // todo: add unit tests
        if (StringUtils.equalsIgnoreCase(javaVersion, JRE_8)) {
            final String fixWebContainer = StringUtils.isEmpty(webContainer) ? JRE_8 : webContainer;
            if (runtimeStackMap.containsKey(fixWebContainer)) {
                return runtimeStackMap.get(fixWebContainer);
            } else {
                throw new MojoExecutionException(
                    String.format("Unknown value of <webContainer>. Supported values are %s.",
                        StringUtils.join(runtimeStackMap.keySet().toArray(), ",")));
            }
        }
        throw new MojoExecutionException(String.format(
            "Unknown value of <javaVersion>. Supported values is %s", JRE_8));
    }

    public WebContainer getWebContainer() {
        if (StringUtils.isEmpty(webContainer)) {
            return WebContainer.TOMCAT_8_5_NEWEST;
        }
        return WebContainer.fromString(webContainer);
    }

    public static List<String> getValidLinuxRuntime() {
        return new ArrayList<>(runtimeStackMap.keySet());
    }

    public static String getDefaultLinuxRuntimeStack(){
        return JRE_8;
    }

    public static String getLinuxJavaVersionByRuntimeStack(RuntimeStack runtimeStack) {
        return runtimeStackMap.getKey(runtimeStack);
    }

    public static RuntimeStack getLinuxRuntimeStackByJavaVersion(String javaVersion){
        return runtimeStackMap.get(javaVersion);
    }

    public String getImage() {
        return this.image;
    }

    public String getServerId() {
        return this.serverId;
    }

    public String getRegistryUrl() {
        return this.registryUrl;
    }

    public boolean isEmpty() {
        return StringUtils.isEmpty(this.os) && StringUtils.isEmpty(this.javaVersion) &&
            StringUtils.isEmpty(this.webContainer) && StringUtils.isEmpty(image) &&
            StringUtils.isEmpty(this.serverId) && StringUtils.isEmpty(this.registryUrl);
    }
}
