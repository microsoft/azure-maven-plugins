/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.configurations;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.HashMap;
import java.util.Map;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class LocalSettings {
    private boolean isEncrypted = false;

    private Map<String, String> values;

    private HostSettings host;

    private Map<String, String> connectionStrings;

    public LocalSettings() {
        values = new HashMap<>();
        values.put("AzureWebJobsStorage", "UseDevelopmentStorage=true");
        values.put("AzureWebJobsDashboard", "UseDevelopmentStorage=true");

        connectionStrings = new HashMap<>();
    }

    @JsonGetter("IsEncrypted")
    public boolean isEncrypted() {
        return isEncrypted;
    }

    public void setEncrypted(boolean encrypted) {
        isEncrypted = encrypted;
    }

    @JsonGetter("Values")
    public Map<String, String> getValues() {
        return values;
    }

    @JsonGetter("Host")
    public HostSettings getHost() {
        return host;
    }

    public void setHost(HostSettings host) {
        this.host = host;
    }

    @JsonGetter("ConnectionStrings")
    public Map<String, String> getConnectionStrings() {
        return connectionStrings;
    }

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public static class HostSettings {
        private int localHttpPort;

        private String cors;

        @JsonGetter("localHttpPort")
        public int getLocalHttpPort() {
            return localHttpPort;
        }

        public void setLocalHttpPort(int localHttpPort) {
            this.localHttpPort = localHttpPort;
        }

        @JsonGetter("cors")
        public String getCors() {
            return cors;
        }

        public void setCors(String cors) {
            this.cors = cors;
        }
    }
}
