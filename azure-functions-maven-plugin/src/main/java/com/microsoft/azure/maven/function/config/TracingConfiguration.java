/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.config;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TracingConfiguration {
    private String consoleLevel;

    private String fileLoggingMode;

    @JsonGetter("consoleLevel")
    public String getConsoleLevel() {
        return consoleLevel;
    }

    public void setConsoleLevel(String consoleLevel) {
        this.consoleLevel = consoleLevel;
    }

    @JsonGetter("fileLoggingMode")
    public String getFileLoggingMode() {
        return fileLoggingMode;
    }

    public void setFileLoggingMode(String fileLoggingMode) {
        this.fileLoggingMode = fileLoggingMode;
    }
}
