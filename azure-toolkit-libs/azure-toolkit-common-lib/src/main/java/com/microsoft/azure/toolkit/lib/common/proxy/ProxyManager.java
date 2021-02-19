/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.proxy;

import com.google.common.base.Preconditions;
import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

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

public class ProxyManager {
    private static final String PROPERTY_USE_SYSTEM_PROXY = "java.net.useSystemProxies";
    private static final int MAX_PORT_NUMBER = 65535;
    // isSystemProxyUnset shows whether user specify the proxy through -Djava.net.useSystemProxies
    // see: https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html
    private static final boolean isSystemProxyUnset = StringUtils.isBlank(System.getProperty(PROPERTY_USE_SYSTEM_PROXY));
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
            final InetSocketAddress address = (InetSocketAddress) proxy.address();
            return address.getHostString();
        }
        return null;
    }

    public int getHttpProxyPort() {
        if (Objects.nonNull(this.proxy) && proxy.address() instanceof InetSocketAddress) {
            final InetSocketAddress address = (InetSocketAddress) proxy.address();
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
        // proxy conflicting
        if (forceUseSystemProxy()) {
            throw new IllegalArgumentException("Cannot set the proxy second time when user has specified the proxy through " +
                "vm arguments: -Djava.net.useSystemProxies=true.");
        }
        this.proxy = createHttpProxy(httpProxyHost, httpProxyPort);
        replaceDefaultProxySelector();
    }

    public void init() {
        // we need to init at the program start before any internet access
        if (isSystemProxyUnset) {
            // to make ProxySelector return the system proxy, we need to set java.net.useSystemProxies = true
            System.setProperty(PROPERTY_USE_SYSTEM_PROXY, "true");
        }
        this.proxy = getSystemProxyInner();
        if (isSystemProxyUnset) {
            System.clearProperty(PROPERTY_USE_SYSTEM_PROXY);
        }
    }

    public boolean forceUseSystemProxy() {
        return !isSystemProxyUnset && this.proxy != null;
    }

    private void replaceDefaultProxySelector() {
        ProxySelector.setDefault(new ProxySelector() {
            @Override
            public List<Proxy> select(URI uri) {
                return Collections.singletonList(ProxyManager.this.proxy);
            }

            @Override
            public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
            }
        });
    }

    private static Proxy createHttpProxy(String httpProxyHost, Integer httpProxyPort) {
        return StringUtils.isNotBlank(httpProxyHost) ? new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpProxyHost,
            httpProxyPort)) : null;
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
