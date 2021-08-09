/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.proxy;

import com.azure.core.util.Configuration;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import lombok.SneakyThrows;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.net.Proxy;
import java.net.ProxySelector;
import java.net.SocketAddress;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class ProxyManager {
    private static final String PROPERTY_USE_SYSTEM_PROXY = "java.net.useSystemProxies";
    // isSystemProxyUnset shows whether user specify the proxy through -Djava.net.useSystemProxies
    // see: https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html
    private static final boolean isSystemProxyUnset = StringUtils.isBlank(System.getProperty(PROPERTY_USE_SYSTEM_PROXY));

    public boolean isProxyEnabled() {
        return StringUtils.isNotBlank(Azure.az().config().getProxySource());
    }

    private static class ProxyManagerHolder {
        private static final ProxyManager INSTANCE = new ProxyManager();
    }

    public static ProxyManager getInstance() {
        return ProxyManagerHolder.INSTANCE;
    }

    private Proxy getSystemProxy() {
        // we need to init at the program start before any internet access
        if (isSystemProxyUnset) {
            // to make ProxySelector return the system proxy, we need to set java.net.useSystemProxies = true
            System.setProperty(PROPERTY_USE_SYSTEM_PROXY, "true");
        }
        final Proxy proxy = getSystemProxyInner();
        if (isSystemProxyUnset) {
            System.clearProperty(PROPERTY_USE_SYSTEM_PROXY);
        }
        return proxy;
    }

    public void applyProxy() {
        final AzureConfiguration config = Azure.az().config();
        String source = config.getProxySource();
        if (StringUtils.isBlank(source)) {
            Proxy proxy = getSystemProxy();
            if (proxy != null) {
                source = "system";
                config.setProxySource(source);
                config.setHttpProxy((InetSocketAddress) proxy.address());
            }
        }
        if (Objects.nonNull(config.getHttpProxy())) {
            final String proxyHost = config.getHttpProxy().getAddress().getHostAddress();
            final int proxyPort = config.getHttpProxy().getPort();
            AzureMessager.getMessager().info(AzureString.format("Use %s proxy: %s", StringUtils.defaultString(source, ""),
                proxyHost + ":" + proxyPort));
            if (!StringUtils.equals(source, "system")) {
                final Proxy proxy = createHttpProxy(proxyHost, proxyPort);
                ProxySelector.setDefault(new ProxySelector() {
                    @Override
                    public List<Proxy> select(URI uri) {
                        return Collections.singletonList(proxy);
                    }

                    @Override
                    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                    }
                });
                // Java ignores http.proxyUser. Here come's the workaround.
                // see https://stackoverflow.com/questions/1626549/authenticated-http-proxy-with-java
                if (StringUtils.isNoneBlank(config.getProxyUsername(), config.getProxyPassword())) {
                    Authenticator.setDefault(
                        new Authenticator() {
                            @Override
                            public PasswordAuthentication getPasswordAuthentication() {
                                if (getRequestorType() == RequestorType.PROXY) {
                                    if (getRequestingHost().equalsIgnoreCase(proxyHost)) {
                                        return new PasswordAuthentication(config.getProxyUsername(), config.getProxyPassword().toCharArray());
                                    }
                                }
                                return null;
                            }
                        }
                    );
                }
            }
            // set proxy for azure-identity according to https://docs.microsoft.com/en-us/azure/developer/java/sdk/proxying
            String proxyAuthPrefix = StringUtils.EMPTY;
            if (StringUtils.isNoneBlank(config.getProxyUsername(), config.getProxyPassword())) {
                proxyAuthPrefix = config.getProxyUsername() + ":" + config.getProxyPassword() + "@";
            }
            final String proxyUrl = String.format("http://%s%s:%d", proxyAuthPrefix,
                proxyHost, proxyPort);
            Configuration.getGlobalConfiguration().put(Configuration.PROPERTY_HTTP_PROXY, proxyUrl);
            Configuration.getGlobalConfiguration().put(Configuration.PROPERTY_HTTPS_PROXY, proxyUrl);
        }
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
