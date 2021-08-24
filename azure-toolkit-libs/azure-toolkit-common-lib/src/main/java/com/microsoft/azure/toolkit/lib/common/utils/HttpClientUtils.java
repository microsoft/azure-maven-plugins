/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils;

import com.azure.core.http.HttpClient;
import com.azure.core.http.ProxyOptions;
import com.azure.core.http.okhttp.OkHttpAsyncHttpClientBuilder;
import com.microsoft.azure.toolkit.lib.Azure;
import com.microsoft.azure.toolkit.lib.AzureConfiguration;
import org.apache.commons.lang3.StringUtils;

import java.net.InetSocketAddress;

public class HttpClientUtils {
    public static HttpClient build() {
        OkHttpAsyncHttpClientBuilder builder = new OkHttpAsyncHttpClientBuilder();
        final AzureConfiguration config = Azure.az().config();
        if (StringUtils.isNotBlank(config.getProxySource())) {
            final ProxyOptions proxyOptions = new ProxyOptions(ProxyOptions.Type.HTTP,
                new InetSocketAddress(config.getHttpProxyHost(), config.getHttpProxyPort())
            );
            if (StringUtils.isNoneBlank(config.getProxyUsername(), config.getProxyPassword())) {
                proxyOptions.setCredentials(config.getProxyUsername(), config.getProxyPassword());
            }
            builder.proxy(proxyOptions);
        }
        return builder.build();
    }
}
