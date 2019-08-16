/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth.configuration;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AuthConfigurationTest {
    @Test
    public void testValidate() {
        AuthConfiguration auth = new AuthConfiguration();
        auth.setClient("client1");
        assertTrue(auth.validate().size() == 2);

        auth.setTenant("tenant1");
        assertTrue(auth.validate().size() == 1);

        auth.setKey("key1");
        assertTrue(auth.validate().size() == 0);

        auth.setCertificate("certificate1");
        assertTrue(auth.validate().size() == 1);

        auth.setKey("");
        assertTrue(auth.validate().size() == 0);

        auth = new AuthConfiguration();
        auth.setTenant("tenant1");
        assertTrue(auth.validate().size() == 2);

        auth.setClient("client1");
        auth.setKey("key1");
        assertTrue(auth.validate().size() == 0);

        auth.setEnvironment("hello");
        assertTrue(auth.validate().size() == 1);

        auth.setEnvironment("azure");
        assertTrue(auth.validate().size() == 0);

        auth.setHttpProxyPort("port");
        assertTrue(auth.validate().size() == 2);

        auth.setHttpProxyHost("localhost");
        assertTrue(auth.validate().size() == 1);

        auth.setHttpProxyPort("10000000");
        assertTrue(auth.validate().size() == 1);

        auth.setHttpProxyPort("0");
        assertTrue(auth.validate().size() == 1);

        auth.setHttpProxyPort("1000");
        assertTrue(auth.validate().size() == 0);
    }

    @Test
    public void testSetClient() {
        final AuthConfiguration auth = new AuthConfiguration();
        auth.setClient("client1");
        assertEquals("client1", auth.getClient());
    }

    @Test
    public void testSetTenant() {
        final AuthConfiguration auth = new AuthConfiguration();
        auth.setTenant("tenant1");
        assertEquals("tenant1", auth.getTenant());
    }

    @Test
    public void testSetKey() {
        final AuthConfiguration auth = new AuthConfiguration();
        auth.setKey("key1");
        assertEquals("key1", auth.getKey());
    }

    @Test
    public void testSetCertificate() {
        final AuthConfiguration auth = new AuthConfiguration();
        auth.setCertificate("certificate1");
        assertEquals("certificate1", auth.getCertificate());
    }

    @Test
    public void testSetCertificatePassword() {
        final AuthConfiguration auth = new AuthConfiguration();
        auth.setCertificatePassword("certificatePassword1");
        assertEquals("certificatePassword1", auth.getCertificatePassword());
    }

    @Test
    public void testSetEnvironment() {
        final AuthConfiguration auth = new AuthConfiguration();
        auth.setEnvironment("environment1");
        assertEquals("environment1", auth.getEnvironment());
    }

    @Test
    public void testSetServerId() {
        final AuthConfiguration auth = new AuthConfiguration();
        auth.setServerId("serverId1");
        assertEquals("serverId1", auth.getServerId());
    }

    @Test
    public void testSetHttpProxyHost() {
        final AuthConfiguration auth = new AuthConfiguration();
        auth.setHttpProxyHost("httpProxyHost1");
        assertEquals("httpProxyHost1", auth.getHttpProxyHost());
    }

    @Test
    public void testSetHttpProxyPort() {
        final AuthConfiguration auth = new AuthConfiguration();
        auth.setHttpProxyPort("8080");
        assertEquals("8080", auth.getHttpProxyPort());
    }
}
