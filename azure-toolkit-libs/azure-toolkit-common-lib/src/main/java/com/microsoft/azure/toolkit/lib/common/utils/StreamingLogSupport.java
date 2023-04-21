/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import reactor.core.publisher.Flux;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.util.Map;
import java.util.Objects;

public interface StreamingLogSupport {
    default Flux<String> streamingLogs(String endPoint, Map<String, String> params) {
        if (Objects.isNull(endPoint)) {
            return Flux.empty();
        }
        try {
            final URIBuilder uriBuilder = new URIBuilder(endPoint);
            params.forEach(uriBuilder::addParameter);
            final HttpURLConnection connection = (HttpURLConnection) uriBuilder.build().toURL().openConnection();
            connection.setRequestProperty("Authorization", getAuthorizationValue());
            connection.setReadTimeout(600000);
            connection.setConnectTimeout(3000);
            connection.setRequestMethod("GET");
            connection.connect();
            return Flux.create((fluxSink) -> {
                try {
                    final InputStream is = connection.getInputStream();
                    BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = rd.readLine()) != null) {
                        fluxSink.next(line);
                    }
                    rd.close();
                } catch (final Exception e) {
                    throw new AzureToolkitRuntimeException(e);
                }
            });
        } catch (final Exception e) {
            throw new AzureToolkitRuntimeException(e);
        }
    }

    default String getAuthorizationValue() {
        return StringUtils.EMPTY;
    }
}
