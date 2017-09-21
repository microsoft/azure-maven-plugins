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
    public static final String HUB_FILE_TRIGGER = "apiHubFileTrigger";
    public static final String HUB_FILE = "apiHubFile";

    private String path = "";

    private String connection = "";

    public ApiHubFileBinding(final ApiHubFileTrigger fileTrigger) {
        super(fileTrigger.name(), HUB_FILE_TRIGGER, Direction.IN);

        path = fileTrigger.path();
        connection = fileTrigger.connection();
    }

    public ApiHubFileBinding(final ApiHubFileInput fileInput) {
        super(fileInput.name(), HUB_FILE, Direction.IN);

        path = fileInput.path();
        connection = fileInput.connection();
    }

    public ApiHubFileBinding(final ApiHubFileOutput fileOutput) {
        super(fileOutput.name(), HUB_FILE, Direction.OUT);

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
