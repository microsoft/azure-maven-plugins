/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.common.logging;

public interface ILogger {
    void debug(String content);

    void debug(String content, Throwable error);

    void debug(Throwable error);

    void info(String content);

    void info(String content, Throwable error);

    void info(Throwable error);

    void warn(String content);

    void warn(String content, Throwable error);

    void warn(Throwable error);

    void error(String content);

    void error(String content, Throwable error);

    void error(Throwable error);
}
