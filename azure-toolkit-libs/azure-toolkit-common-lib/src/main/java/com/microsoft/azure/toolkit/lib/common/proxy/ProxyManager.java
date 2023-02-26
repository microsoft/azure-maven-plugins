/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.proxy;

import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import lombok.SneakyThrows;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;

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
import java.util.Optional;
import java.util.StringJoiner;
import java.util.regex.Pattern;

import static java.util.regex.Pattern.quote;

public class ProxyManager {
    private static final String PROPERTY_USE_SYSTEM_PROXY = "java.net.useSystemProxies";
    // isSystemProxyUnset shows whether user specify the proxy through -Djava.net.useSystemProxies
    // see: https://docs.oracle.com/javase/8/docs/technotes/guides/net/proxies.html
    private static final boolean isSystemProxyUnset = StringUtils.isBlank(System.getProperty(PROPERTY_USE_SYSTEM_PROXY));
    private final ProxySelector defaultProxySelector = ProxySelector.getDefault();

    public boolean isProxyEnabled() {
        return StringUtils.isNotBlank(Azure.az().config().getProxySource());
    }

    private static class ProxyManagerHolder {
        private static final ProxyManager INSTANCE = new ProxyManager();
    }

    public static ProxyManager getInstance() {
        return ProxyManagerHolder.INSTANCE;
    }

    public void applyProxy() {
        final AzureConfiguration config = Azure.az().config();
        String source = config.getProxySource();
        if (StringUtils.isBlank(source)) {
            ProxyInfo proxy = ObjectUtils.firstNonNull(
                getProxyFromProgramArgument("http"),
                getProxyFromProgramArgument("https"),
                getSystemProxy()
            );

            if (proxy != null) {
                config.setProxyInfo(proxy);
                source = config.getProxySource();
            }
        }
        if (StringUtils.isNotBlank(source)) {
            final String proxyHost = config.getHttpProxyHost();
            final int proxyPort = config.getHttpProxyPort();
            AzureMessager.getMessager().info(AzureString.format("Use %s proxy: %s", source, proxyHost + ":" + proxyPort));
            if (!StringUtils.equals(source, "system") && !StringUtils.equals(source, "intellij")) {
                final Proxy proxy = createHttpProxy(proxyHost, proxyPort);
                ProxySelector.setDefault(new ProxySelector() {
                    @Override
                    public List<Proxy> select(URI uri) {
                        if (uri == null) {
                            throw new IllegalArgumentException("URI can't be null.");
                        }
                        String protocol = uri.getScheme();
                        String host = uri.getHost();
                        if (protocol == null || host == null) {
                            throw new IllegalArgumentException("protocol = "+protocol+" host = "+host);
                        }
                        if (shouldNotUseProxyFor(toPattern(Optional.ofNullable(config.getNonProxyHosts()).orElse(StringUtils.EMPTY)),
                                host.toLowerCase())) {
                            return Collections.singletonList(Proxy.NO_PROXY);
                        }
                        return Collections.singletonList(proxy);
                    }

                    @Override
                    public void connectFailed(URI uri, SocketAddress sa, IOException ioe) {
                    }
                });
                // Java ignores http.proxyUser. Here comes the workaround.
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
        }
    }

    public void resetProxy() {
        ProxySelector.setDefault(this.defaultProxySelector);
    }

    private ProxyInfo getSystemProxy() {
        // we need to init at the program start before any internet access
        if (isSystemProxyUnset) {
            // to make ProxySelector return the system proxy, we need to set java.net.useSystemProxies = true
            System.setProperty(PROPERTY_USE_SYSTEM_PROXY, "true");
        }
        final ProxyInfo proxy = getSystemProxyInner();
        if (isSystemProxyUnset) {
            System.clearProperty(PROPERTY_USE_SYSTEM_PROXY);
        }
        return proxy;
    }

    private static ProxyInfo getProxyFromProgramArgument(String prefix) {
        final String proxyHost = System.getProperty(prefix + ".proxyHost");
        final String proxyPort = System.getProperty(prefix + ".proxyPort");
        final String proxyUser = System.getProperty(prefix + ".proxyUser");
        final String proxyPassword = System.getProperty(prefix + ".proxyPassword");
        final String nonProxyHosts = System.getProperty(prefix + ".nonProxyHosts");

        if (StringUtils.isNoneBlank(proxyHost, proxyPort) && NumberUtils.isCreatable(proxyPort)) {
            return ProxyInfo.builder().source(String.format("${%s}", prefix + ".proxyHost"))
                .host(proxyHost)
                .port(Integer.parseInt(proxyPort))
                .username(proxyUser)
                .password(proxyPassword)
                .nonProxyHosts(nonProxyHosts)
                .build();
        }
        return null;
    }

    private static Proxy createHttpProxy(String httpProxyHost, Integer httpProxyPort) {
        return StringUtils.isNotBlank(httpProxyHost) ? new Proxy(Proxy.Type.HTTP, new InetSocketAddress(httpProxyHost,
            httpProxyPort)) : null;
    }

    @SneakyThrows
    private static ProxyInfo getSystemProxyInner() {
        final URI uri = new URI("https://login.microsoft.com");
        // modified version of find system proxy at
        // https://stackoverflow.com/questions/4933677/detecting-windows-ie-proxy-setting-using-java/4933746#4933746
        for (final Proxy proxy : ProxySelector.getDefault().select(uri)) {
            if (!(proxy.address() instanceof InetSocketAddress)) {
                continue;
            }
            final InetSocketAddress address = (InetSocketAddress) proxy.address();
            return ProxyInfo.builder().source("system").host(address.getHostName()).port(address.getPort()).build();
        }
        return null;
    }

    /**
     * refer sun.net.spi.DefaultProxySelector
     * @return {@code true} if given this pattern for non-proxy hosts and this
     *         urlhost the proxy should NOT be used to access this urlhost
     */
    private static boolean shouldNotUseProxyFor(Pattern pattern, String urlhost) {
        if (pattern == null || urlhost.isEmpty()) {
            return false;
        }
        boolean matches = pattern.matcher(urlhost).matches();
        return matches;
    }

    /**
     * refer to sun.net.spi.DefaultProxySelector
     * @param mask non-null mask
     * @return {@link java.util.regex.Pattern} corresponding to this mask
     *         or {@code null} in case mask should not match anything
     */
    private static Pattern toPattern(String mask) {
        boolean disjunctionEmpty = true;
        StringJoiner joiner = new StringJoiner("|");
        for (String disjunct : mask.split("\\|")) {
            if (disjunct.isEmpty()) {
                continue;
            }
            disjunctionEmpty = false;
            String regex = disjunctToRegex(disjunct.toLowerCase());
            joiner.add(regex);
        }
        return disjunctionEmpty ? null : Pattern.compile(joiner.toString());
    }

    /**
     * refer to sun.net.spi.DefaultProxySelector
     * @param disjunct non-null mask disjunct
     * @return java regex string corresponding to this mask
     */
    private static String disjunctToRegex(String disjunct) {
        String regex;
        if ("*".equals(disjunct)) {
            regex = ".*";
        } else if (disjunct.startsWith("*") && disjunct.endsWith("*")) {
            regex = ".*" + quote(disjunct.substring(1, disjunct.length() - 1)) + ".*";
        } else if (disjunct.startsWith("*")) {
            regex = ".*" + quote(disjunct.substring(1));
        } else if (disjunct.endsWith("*")) {
            regex = quote(disjunct.substring(0, disjunct.length() - 1)) + ".*";
        } else {
            regex = quote(disjunct);
        }
        return regex;
    }

}
