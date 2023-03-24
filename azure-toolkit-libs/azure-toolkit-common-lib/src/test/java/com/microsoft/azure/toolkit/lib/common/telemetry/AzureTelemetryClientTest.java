/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.telemetry;

import org.apache.commons.lang3.StringUtils;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

public class AzureTelemetryClientTest extends AzureTelemetryClient {
    @Test
    public void anonymizePersonallyIdentifiableInformation() {
        final Map<String, String> map = new HashMap<String, String>() {{
            put("fake-password", "pwd=FAKE");
            put("fake-email", "no-reply@example.com");
            put("fake-token", "token=FAKE");
            put("fake-slack-token", "xoxp-FAKE");
            put("fake-path", "/Users/username/.AzureToolkitforIntelliJ/extensions");
        }};
        AzureTelemetryClientTest.anonymizePersonallyIdentifiableInformation(map);
        assert StringUtils.equals(map.get("fake-password"), "<REDACTED: Generic Secret>");
        assert StringUtils.equals(map.get("fake-email"), "<REDACTED: Email>");
        assert StringUtils.equals(map.get("fake-token"), "<REDACTED: Generic Secret>");
        assert StringUtils.equals(map.get("fake-slack-token"), "<REDACTED: Slack Toke>");
        assert StringUtils.equals(map.get("fake-path"), "<REDACTED: user-file-path>");
    }
}