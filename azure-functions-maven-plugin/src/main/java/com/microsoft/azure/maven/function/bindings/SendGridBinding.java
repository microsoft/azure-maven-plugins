/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.azure.serverless.functions.annotation.SendGridOutput;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SendGridBinding extends BaseBinding {
    private String apiKey = "";

    private String to = "";

    private String from = "";

    private String subject = "";

    private String text = "";

    public SendGridBinding(final SendGridOutput sendGridOutput) {
        super(sendGridOutput.name(), "sendGrid", Direction.OUT);

        apiKey = sendGridOutput.apiKey();
        to = sendGridOutput.to();
        from = sendGridOutput.from();
        subject = sendGridOutput.subject();
        text = sendGridOutput.text();
    }

    @JsonGetter
    public String getApiKey() {
        return apiKey;
    }

    @JsonGetter
    public String getTo() {
        return to;
    }

    @JsonGetter
    public String getFrom() {
        return from;
    }

    @JsonGetter
    public String getSubject() {
        return subject;
    }

    @JsonGetter
    public String getText() {
        return text;
    }
}
