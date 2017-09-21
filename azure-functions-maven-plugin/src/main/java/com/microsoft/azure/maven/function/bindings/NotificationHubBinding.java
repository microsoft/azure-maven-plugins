/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.azure.serverless.functions.annotation.NotificationHubOutput;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class NotificationHubBinding extends BaseBinding {
    public static final String NOTIFICATION_HUB = "notificationHub";

    private String tagExpression = "";

    private String hubName = "";

    private String connection = "";

    private String platform = "";

    public NotificationHubBinding(final NotificationHubOutput hubOutput) {
        super(hubOutput.name(), NOTIFICATION_HUB, Direction.OUT);

        tagExpression = hubOutput.tagExpression();
        hubName = hubOutput.hubName();
        connection = hubOutput.connection();
        platform = hubOutput.platform();
    }

    @JsonGetter
    public String getTagExpression() {
        return tagExpression;
    }

    @JsonGetter
    public String getHubName() {
        return hubName;
    }

    @JsonGetter
    public String getConnection() {
        return connection;
    }

    @JsonGetter
    public String getPlatform() {
        return platform;
    }
}
