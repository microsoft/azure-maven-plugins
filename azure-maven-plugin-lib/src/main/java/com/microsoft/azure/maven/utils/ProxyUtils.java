/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.utils;

import com.microsoft.azure.toolkit.lib.common.proxy.ProxyManager;
import com.microsoft.azure.tools.auth.exception.InvalidConfigurationException;
import com.microsoft.azure.tools.auth.util.ValidationUtil;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.apache.maven.execution.MavenExecutionRequest;
import org.apache.maven.settings.Proxy;

import java.util.List;

public class ProxyUtils {
    public static void initProxyManager(String httpProxyHost, String httpProxyPort, MavenExecutionRequest request) throws InvalidConfigurationException {
        final ProxyManager proxyManager = ProxyManager.getInstance();
        if (StringUtils.isAllBlank(httpProxyHost, httpProxyPort)) {
            final List<Proxy> mavenProxies = request.getProxies();
            if (CollectionUtils.isNotEmpty(mavenProxies)) {
                final org.apache.maven.settings.Proxy mavenProxy = mavenProxies.stream().filter(
                    proxy -> proxy.isActive() && proxy.getPort() > 0 && StringUtils.isNotBlank(proxy.getHost())).findFirst().orElse(null);
                if (mavenProxy != null) {
                    proxyManager.configure("maven", mavenProxy.getHost(), mavenProxy.getPort());
                }
            }
        } else {
            ValidationUtil.validateHttpProxy(httpProxyHost, httpProxyPort);
            proxyManager.configure("user", httpProxyHost, NumberUtils.toInt(httpProxyPort));
        }
    }
}
