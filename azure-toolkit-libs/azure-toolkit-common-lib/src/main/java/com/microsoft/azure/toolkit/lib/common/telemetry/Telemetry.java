/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.toolkit.lib.common.telemetry;

public class Telemetry {
    public enum Type {
        OP_START,
        OP_END,
        STEP,
        INFO,
        WARNING,
        ERROR
    }
}
