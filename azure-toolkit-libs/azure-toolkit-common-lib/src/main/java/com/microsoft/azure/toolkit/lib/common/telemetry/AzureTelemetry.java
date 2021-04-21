/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.common.telemetry;

import com.microsoft.azure.toolkit.lib.common.operation.IAzureOperation;
import lombok.AccessLevel;
import lombok.Getter;

import javax.annotation.Nonnull;
import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class AzureTelemetry {
    static final String OP_CREATE_AT = "op_create_at";
    static final String OP_ENTER_AT = "op_enter_at";
    static final String OP_EXIT_AT = "op_exit_at";

    public enum Type {
        OP_START,
        OP_END,
        STEP,
        INFO,
        WARNING,
        ERROR
    }

    @Nonnull
    public static Context getContext() {
        return Optional.ofNullable(IAzureOperation.current())
                .map(o -> o.get(AzureTelemetry.Context.class, new AzureTelemetry.Context()))
                .orElse(new AzureTelemetry.Context());
    }

    @Nonnull
    public static Context getActionContext() {
        return Optional.ofNullable(IAzureOperation.current())
                .map(IAzureOperation::getActionParent)
                .map(o -> o.get(AzureTelemetry.Context.class, new AzureTelemetry.Context()))
                .orElse(new AzureTelemetry.Context());
    }

    @Getter
    public static class Context {
        @Getter(AccessLevel.PACKAGE)
        private final Map<String, String> properties = new ConcurrentHashMap<>();

        public void setCreateAt(Instant createAt) {
            this.properties.put(OP_CREATE_AT, createAt.toString());
        }

        public void setEnterAt(Instant enterAt) {
            this.properties.put(OP_ENTER_AT, enterAt.toString());
        }

        public void setExitAt(Instant exitAt) {
            this.properties.put(OP_EXIT_AT, exitAt.toString());
        }

        public void setProperty(String key, String val) {
            this.properties.put(key, val);
        }

        public String getProperty(String key) {
            return this.properties.get(key);
        }
    }
}
