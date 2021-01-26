/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.tools.common.util;

import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.URI;

public class ProxyUtils {
    private static final String PROPERTY_USE_SYSTEM_PROXY = "java.net.useSystemProxies";

    public static Proxy createHttpProxy(String httpProxyHost, String httpProxyPort) {
        return StringUtils.isNotBlank(httpProxyHost) ? new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpProxyHost,
                NumberUtils.toInt(httpProxyPort))) : null;
    }

    public static InetSocketAddress getSystemProxy() {
        final String useSystemProxy = System.getProperty(PROPERTY_USE_SYSTEM_PROXY);
        // if user doesn't want to use system proxy
        if (StringUtils.isNotBlank(useSystemProxy) && !BooleanUtils.toBoolean(useSystemProxy)) {
            return null;
        }
        InetSocketAddress systemProxy = null;
        try {
            System.setProperty(PROPERTY_USE_SYSTEM_PROXY, "true");
            // modified version of find system proxy at
            // https://stackoverflow.com/questions/4933677/detecting-windows-ie-proxy-setting-using-java/4933746#4933746
            for (final Proxy proxy : ProxySelector.getDefault().select(new URI("https://foo/bar"))) {
                if (!(proxy.address() instanceof InetSocketAddress)) {
                    continue;
                }
                systemProxy = (InetSocketAddress) proxy.address();
            }
        } catch (Exception e) {
            // swallow error

        } finally {
            if (StringUtils.isBlank(useSystemProxy)) {
                System.clearProperty(PROPERTY_USE_SYSTEM_PROXY);
            } else {
                System.setProperty(PROPERTY_USE_SYSTEM_PROXY, useSystemProxy);
            }
        }
        return systemProxy;
    }
}
