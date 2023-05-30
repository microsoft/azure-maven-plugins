/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils;

import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.messager.AzureMessager;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.http.client.utils.URIBuilder;
import reactor.core.publisher.Flux;

import javax.annotation.Nonnull;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public interface StreamingLogSupport {
    default Flux<String> streamingLogs(boolean follow) {
        return streamingLogs(follow, Collections.emptyMap());
    }

    default Flux<String> streamingLogs(boolean follow, int tailLines) {
        return streamingLogs(follow, Collections.singletonMap("tailLines", String.valueOf(tailLines)));
    }

    default Flux<String> streamingLogs(boolean follow, @Nonnull Map<String, String> p) {
        try {
            final Map<String, String> params = new HashMap<>();
            params.put("sinceSeconds", String.valueOf(300));
            params.put("tailLines", String.valueOf(300));
            params.put("limitBytes", String.valueOf(1024 * 1024));
            params.putAll(p);
            params.put("follow", String.valueOf(follow));

            final HttpURLConnection connection = createLogStreamConnection(params);
            connection.connect();
            return Flux.create((fluxSink) -> {
                try {
                    final InputStream is = connection.getInputStream();
                    final BufferedReader rd = new BufferedReader(new InputStreamReader(is));
                    String line;
                    while ((line = rd.readLine()) != null) {
                        fluxSink.next(line);
                    }
                    rd.close();
                } catch (final FileNotFoundException e) {
                    AzureMessager.getMessager().error("app/instance may be deactivated, please refresh and try again later.");
                } catch (final IOException e) {
                    throw new AzureToolkitRuntimeException(e);
                }
            });
        } catch (final Exception e) {
            throw new AzureToolkitRuntimeException(e);
        }
    }

    @Nonnull
    default HttpURLConnection createLogStreamConnection(Map<String, String> params) throws IOException, URISyntaxException {
        final URIBuilder uriBuilder = new URIBuilder(getLogStreamEndpoint());
        params.forEach(uriBuilder::addParameter);
        final HttpURLConnection connection = (HttpURLConnection) uriBuilder.build().toURL().openConnection();
        connection.setRequestProperty("Authorization", getLogStreamAuthorization());
        connection.setReadTimeout(600000);
        connection.setConnectTimeout(3000);
        connection.setRequestMethod("GET");
        return connection;
    }

    default String getLogStreamEndpoint() {
        throw new NotImplementedException();
    }

    default String getLogStreamAuthorization() {
        throw new NotImplementedException();
    }
}
