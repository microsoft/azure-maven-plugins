/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.azure.serverless.functions.annotation.ApiHubFileInput;
import com.microsoft.azure.serverless.functions.annotation.ApiHubFileOutput;
import com.microsoft.azure.serverless.functions.annotation.ApiHubFileTrigger;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class ApiHubFileBinding extends BaseBinding {
    private String path = "";

    private String connection = "";

    public ApiHubFileBinding(final ApiHubFileTrigger fileTrigger) {
        setDirection("in");
        setType("apiHubFileTrigger");
        setName(fileTrigger.name());

        path = fileTrigger.path();
        connection = fileTrigger.connection();
    }

    public ApiHubFileBinding(final ApiHubFileInput fileInput) {
        setDirection("in");
        setType("apiHubFile");
        setName(fileInput.name());

        path = fileInput.path();
        connection = fileInput.connection();
    }

    public ApiHubFileBinding(final ApiHubFileOutput fileOutput) {
        setDirection("out");
        setType("apiHubFile");
        setName(fileOutput.name());

        path = fileOutput.path();
        connection = fileOutput.connection();
    }

    @JsonGetter
    public String getPath() {
        return path;
    }

    @JsonGetter
    public String getConnection() {
        return connection;
    }
}
