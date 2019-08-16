/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.azure.auth.exception.AzureLoginFailureException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class LocalAuthServerTest {
    private LocalAuthServer localAuthServer;

    @Before
    public void setUp() throws Exception {
        localAuthServer = new LocalAuthServer();
        localAuthServer.start();
    }

    @After
    public void tearDown() throws Exception {
        localAuthServer.stop();
    }

    @Test
    public void testGetCode() throws Exception {
        final String url = localAuthServer.getURI().toString();
        final String token = "test_token";
        final String queryString = String.format("code=%s&session_state=1105e0b8-d398-410e-ac12-79c47cabda4f", token);
        final URLConnection conn = new URL(url + "?" + queryString).openConnection();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            final String html = reader.lines().collect(Collectors.joining("\n"));
            assertTrue(html.contains("Login successfully"));
            assertEquals(token, localAuthServer.waitForCode());
        }
    }

    @Test
    public void testErrorResult() throws Exception {
        final String url = localAuthServer.getURI().toString();
        final String queryString = "access_denied&error_description=the+user+canceled+the+authentication";
        final URLConnection conn = new URL(url + "?" + queryString).openConnection();
        final Runnable runnable = () -> {
            try {
                localAuthServer.waitForCode();
                fail();
            } catch (AzureLoginFailureException ex) {

            } catch (InterruptedException e) {
                fail("Unexpected InterruptedException:" + e.getMessage());
            }
        };
        final Thread t = new Thread(runnable);
        t.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
            final String html = reader.lines().collect(Collectors.joining("\n"));
            assertTrue(html.contains("Login failed"));
        }
    }

    @Test
    public void testStart() throws IOException {

        // should be able to rest
        localAuthServer.stop();
        try {
            localAuthServer.start();
        } catch (Exception ex) {
            fail("Should not fail on after start.");
        }
    }

    @Test
    public void testStop() throws IOException {
        localAuthServer.stop();
        // should not throw exception on second stop
        localAuthServer.stop();
    }
}
