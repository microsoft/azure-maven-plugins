/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.common.proxy;

import com.google.common.base.Preconditions;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.annotation.Nonnull;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import lombok.Getter;
import lombok.SneakyThrows;

public class ProxyManager {
    private static final String PROPERTY_USE_SYSTEM_PROXY = "java.net.useSystemProxies";
    private static final int MAX_PORT_NUMBER = 65535;

    @Getter
    private Proxy proxy;

    private static class ProxyManagerHolder {
        private static final ProxyManager INSTANCE = new ProxyManager();
    }

    public static ProxyManager getInstance() {
        return ProxyManagerHolder.INSTANCE;
    }

    public String getHttpProxyHost() {
        if (Objects.nonNull(this.proxy) && proxy.address() instanceof InetSocketAddress) {
            InetSocketAddress address = (InetSocketAddress) proxy.address();
            return address.getHostString();
        }
        return null;
    }

    public int getHttpProxyPort() {
        if (Objects.nonNull(this.proxy) && proxy.address() instanceof InetSocketAddress) {
            InetSocketAddress address = (InetSocketAddress) proxy.address();
            return address.getPort();
        }
        return 0;
    }

    public void configure(@Nonnull String httpProxyHost, @Nonnull Integer httpProxyPort) {
        Preconditions.checkNotNull(httpProxyHost, "httpProxyHost must not be null.");
        Preconditions.checkNotNull(httpProxyHost, "httpProxyPort must not be null.");
        if (httpProxyPort <= 0 || httpProxyPort > MAX_PORT_NUMBER) {
            throw new IllegalArgumentException(
                    String.format("Invalid range of httpProxyPort: '%s', it should be a number between %d and %d", httpProxyPort, 1, MAX_PORT_NUMBER));
        }
        this.proxy = createHttpProxy(httpProxyHost, httpProxyPort);
    }

    public void init() {
        // if user wants to use or not use system proxy explicitly
        Proxy systemProxy = getSystemProxy();
        if (systemProxy != null) {
            this.proxy = systemProxy;
        }
        this.replaceSystemProxySelector();
    }

    private void replaceSystemProxySelector() {
        final ProxySelector ds = ProxySelector.getDefault();
        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                if (Objects.nonNull(ProxyManager.this.proxy)) {
                    return Collections.singletonList(ProxyManager.this.proxy);
                }
                return ds.select(uri);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                ds.connectFailed(uri, sa, ioe);
            }
        });
    }

    private static Proxy createHttpProxy(String httpProxyHost, Integer httpProxyPort) {
        return StringUtils.isNotBlank(httpProxyHost) ? new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpProxyHost,
                httpProxyPort)) : null;
    }

    private static Proxy getSystemProxy() {
        boolean isSystemProxyUnset = StringUtils.isBlank(System.getProperty(PROPERTY_USE_SYSTEM_PROXY));
        try {
            if (isSystemProxyUnset) {
                System.setProperty(PROPERTY_USE_SYSTEM_PROXY, "true");
            }
            return getSystemProxyInner();
        } finally {
            try {
                if (isSystemProxyUnset) {
                    // revert the influence of System.setProperty(PROPERTY_USE_SYSTEM_PROXY, "true")
                    FieldUtils.writeStaticField(ProxySelector.getDefault().getClass(), "hasSystemProxies", false, true);
                    System.clearProperty(PROPERTY_USE_SYSTEM_PROXY);
                }
            } catch (NullPointerException | IllegalAccessException ex) {
                // ignore
            }
        }
    }

    @SneakyThrows
    private static Proxy getSystemProxyInner() {
        final URI uri = new URI("https://login.microsoft.com");
        // modified version of find system proxy at
        // https://stackoverflow.com/questions/4933677/detecting-windows-ie-proxy-setting-using-java/4933746#4933746
        for (final Proxy proxy : ProxySelector.getDefault().select(uri)) {
            if (!(proxy.address() instanceof InetSocketAddress)) {
                continue;
            }
            return proxy;
        }
        return null;
    }
}
