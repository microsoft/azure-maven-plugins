/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.azure.serverless.functions.annotation.TwilioSmsOutput;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TwilioBinding extends BaseBinding {
    private String accountSid = "";

    private String authToken = "";

    private String to = "";

    private String from = "";

    private String body = "";

    public TwilioBinding(final TwilioSmsOutput smsOutput) {
        super(smsOutput.name(), "twilioSms", Direction.OUT);

        accountSid = smsOutput.accountSid();
        authToken = smsOutput.authToken();
        to = smsOutput.to();
        from = smsOutput.from();
        body = smsOutput.body();
    }

    @JsonGetter
    public String getAccountSid() {
        return accountSid;
    }

    @JsonGetter
    public String getAuthToken() {
        return authToken;
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
    public String getBody() {
        return body;
    }
}
