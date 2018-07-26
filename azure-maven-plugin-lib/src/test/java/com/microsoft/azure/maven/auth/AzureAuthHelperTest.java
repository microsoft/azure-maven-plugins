/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.auth;

import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.credentials.ApplicationTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.maven.AbstractAzureMojo;
import com.microsoft.rest.LogLevel;
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.settings.Server;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;

import static com.microsoft.azure.maven.auth.AzureAuthHelper.CERTIFICATE;
import static com.microsoft.azure.maven.auth.AzureAuthHelper.CERTIFICATE_PASSWORD;
import static com.microsoft.azure.maven.auth.AzureAuthHelper.CLIENT_ID;
import static com.microsoft.azure.maven.auth.AzureAuthHelper.KEY;
import static com.microsoft.azure.maven.auth.AzureAuthHelper.TENANT_ID;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class AzureAuthHelperTest {
    @Mock
    AbstractAzureMojo mojo;

    @Mock
    Log log;

    @Mock
    Settings settings;

    @Mock
    AuthenticationSetting authentication;

    @Mock
    Server server;

    @Mock
    ApplicationTokenCredentials credentials;

    @Mock
    Azure.Configurable configurable;

    @Mock
    Azure.Authenticated authenticated;

    @Mock
    File file;

    @Mock
    Xpp3Dom configuration;

    @Mock
    Xpp3Dom clientIdNode;

    @Mock
    Xpp3Dom tenantIdNode;

    @Mock
    Xpp3Dom keyNode;

    @Mock
    Xpp3Dom certificateNode;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        when(mojo.getLog()).thenReturn(log);
        when(log.isDebugEnabled()).thenReturn(false);
    }

    @Test
    public void testConstructor() throws Exception {
        NullPointerException exception = null;
        try {
            new AzureAuthHelper(null);
        } catch (NullPointerException npe) {
            exception = npe;
        }

        assertNotNull(exception);
    }

    @Test
    public void testGetAzureClient() throws Exception {
        final AzureAuthHelper helper = new AzureAuthHelper(mojo);
        final AzureAuthHelper helperSpy = spy(helper);

        // authObj is null
        doReturn(null).when(helperSpy).getAuthObj();
        assertNull(helperSpy.getAzureClient());

        // authObj is not null
        doReturn(authenticated).when(helperSpy).getAuthObj();

        // <subscriptionId> is not null
        when(mojo.getSubscriptionId()).thenReturn("SubscriptionId");
        helperSpy.getAzureClient();
        verify(authenticated, times(1)).withSubscription(any(String.class));
        verify(authenticated, never()).withDefaultSubscription();
        clearInvocations(authenticated);

        // <subscriptionId> is null
        when(mojo.getSubscriptionId()).thenReturn(null);
        helperSpy.getAzureClient();
        verify(authenticated, never()).withSubscription(any(String.class));
        verify(authenticated, times(1)).withDefaultSubscription();
    }

    @Test
    public void testGetLogLevel() {
        final AzureAuthHelper helper = new AzureAuthHelper(mojo);

        when(log.isDebugEnabled()).thenReturn(false);
        assertSame(LogLevel.NONE, helper.getLogLevel());

        when(log.isDebugEnabled()).thenReturn(true);
        assertSame(LogLevel.BODY_AND_HEADERS, helper.getLogLevel());
    }

    @Test
    public void testGetAzureEnvironment() {
        final AzureAuthHelper helper = new AzureAuthHelper(mojo);
        assertEquals(AzureEnvironment.AZURE, helper.getAzureEnvironment(null));
        assertEquals(AzureEnvironment.AZURE, helper.getAzureEnvironment(""));
        assertEquals(AzureEnvironment.AZURE, helper.getAzureEnvironment("default"));
        assertEquals(AzureEnvironment.AZURE_CHINA, helper.getAzureEnvironment("AZURE_CHINA"));
        assertEquals(AzureEnvironment.AZURE_CHINA, helper.getAzureEnvironment("azure_china"));
        assertEquals(AzureEnvironment.AZURE_GERMANY, helper.getAzureEnvironment("AZURE_GERMANY"));
        assertEquals(AzureEnvironment.AZURE_GERMANY, helper.getAzureEnvironment("azure_germany"));
        assertEquals(AzureEnvironment.AZURE_US_GOVERNMENT, helper.getAzureEnvironment("AZURE_US_GOVERNMENT"));
        assertEquals(AzureEnvironment.AZURE_US_GOVERNMENT, helper.getAzureEnvironment("azure_us_government"));
    }

    @Test
    public void testAzureConfigure() {
        final AzureAuthHelper helper = new AzureAuthHelper(mojo);
        assertTrue(helper.azureConfigure() instanceof Azure.Configurable);
    }

    @Test
    public void testGetAuthObjWithAzureCli() {
        final AzureAuthHelper helper = new AzureAuthHelper(mojo);
        final AzureAuthHelper helperSpy = spy(helper);
        doReturn(authenticated).when(helperSpy).getAuthObjFromAzureCli();

        assertSame(authenticated, helperSpy.getAuthObj());
        verify(helperSpy, never()).getAuthObjFromServerId(null, null);
        verify(helperSpy, never()).getAuthObjFromFile(null);
        verify(helperSpy, times(1)).getAuthObjFromAzureCli();
    }

    @Test
    public void testGetAuthObjWithServerId() {
        final AzureAuthHelper helper = new AzureAuthHelper(mojo);
        final AzureAuthHelper helperSpy = spy(helper);
        when(mojo.getAuthenticationSetting()).thenReturn(authentication);
        doReturn(authenticated).when(helperSpy).getAuthObjFromServerId(null, null);

        assertSame(authenticated, helperSpy.getAuthObj());
        verify(helperSpy, times(1)).getAuthObjFromServerId(null, null);
        verify(helperSpy, never()).getAuthObjFromFile(null);
        verify(helperSpy, never()).getAuthObjFromAzureCli();
    }

    @Test
    public void testGetAuthObjWithFile() {
        final AzureAuthHelper helper = new AzureAuthHelper(mojo);
        final AzureAuthHelper helperSpy = spy(helper);
        when(mojo.getAuthenticationSetting()).thenReturn(authentication);
        doReturn(authenticated).when(helperSpy).getAuthObjFromFile(any(File.class));
        when(authentication.getFile()).thenReturn(mock(File.class));

        assertSame(authenticated, helperSpy.getAuthObj());
        verify(helperSpy, times(1)).getAuthObjFromServerId(null, null);
        verify(helperSpy, times(1)).getAuthObjFromFile(any(File.class));
        verify(helperSpy, never()).getAuthObjFromAzureCli();
    }

    @Test
    public void testGetAuthObjFromServerId() {
        final AzureAuthHelper helper = new AzureAuthHelper(mojo);

        /**
         * serverId is null
         */
        Azure.Authenticated auth = helper.getAuthObjFromServerId(null, null);

        assertNull(auth);
        clearInvocations(mojo);

        /**
         * settings is null
         */
        auth = helper.getAuthObjFromServerId(null, "serverId");

        assertNull(auth);
        clearInvocations(mojo);

        // Setup
        final AzureAuthHelper helperSpy = spy(helper);
        when(settings.getServer(any(String.class))).thenReturn(server);

        /**
         * ApplicationTokenCredentials is null
         */
        doReturn(null).when(helperSpy).getAppTokenCredentialsFromServer(server);

        auth = helper.getAuthObjFromServerId(settings, "serverId");

        assertNull(auth);
        verify(settings, times(1)).getServer(any(String.class));
        verifyNoMoreInteractions(settings);
        clearInvocations(settings);

        /**
         * success
         */
        doReturn(credentials).when(helperSpy).getAppTokenCredentialsFromServer(server);
        doReturn(configurable).when(helperSpy).azureConfigure();
        when(configurable.authenticate(any(ApplicationTokenCredentials.class))).thenReturn(authenticated);

        auth = helperSpy.getAuthObjFromServerId(settings, "serverId");
        assertSame(authenticated, auth);
    }

    @Test
    public void testGetAuthObjFromFile() throws Exception {
        final AzureAuthHelper helper = new AzureAuthHelper(mojo);

        /**
         * authFile is null
         */
        Azure.Authenticated auth = helper.getAuthObjFromFile(null);

        assertNull(auth);
        clearInvocations(mojo);

        /**
         * authFile does not exist
         */
        when(file.exists()).thenReturn(false);

        auth = helper.getAuthObjFromFile(file);

        assertNull(auth);
        clearInvocations(mojo);

        // Setup
        final AzureAuthHelper helperSpy = spy(helper);

        /**
         * read authFile exception
         */
        when(file.exists()).thenReturn(true);
        doReturn(configurable).when(helperSpy).azureConfigure();
        when(configurable.authenticate(any(File.class))).thenThrow(new IOException("Fail to read file."));

        auth = helperSpy.getAuthObjFromFile(file);
        assertNull(auth);

        /**
         * success
         */
        when(file.exists()).thenReturn(true);
        doReturn(configurable).when(helperSpy).azureConfigure();
        when(configurable.authenticate(any(File.class))).thenReturn(authenticated);

        auth = helperSpy.getAuthObjFromFile(file);
        assertSame(authenticated, auth);
    }

    @Test
    public void testGetAppTokenCredentialsFromServer() {
        final AzureAuthHelper helper = new AzureAuthHelper(mojo);
        /**
         * server is not configured
         */
        assertNull(helper.getAppTokenCredentialsFromServer(null));

        /**
         * client Id is not configured
         */
        when(server.getConfiguration()).thenReturn(configuration);
        assertNull(helper.getAppTokenCredentialsFromServer(server));
        verify(configuration, times(1)).getChild(CLIENT_ID);
        clearInvocations(configuration);

        /**
         * tenant Id is not configured
         */
        when(configuration.getChild(CLIENT_ID)).thenReturn(clientIdNode);
        when(clientIdNode.getValue()).thenReturn(CLIENT_ID);
        assertNull(helper.getAppTokenCredentialsFromServer(server));
        verify(configuration, times(1)).getChild(TENANT_ID);
        clearInvocations(configuration);

        /**
         * key is configured
         */
        when(configuration.getChild(CLIENT_ID)).thenReturn(clientIdNode);
        when(clientIdNode.getValue()).thenReturn(CLIENT_ID);
        when(configuration.getChild(TENANT_ID)).thenReturn(tenantIdNode);
        when(tenantIdNode.getValue()).thenReturn(TENANT_ID);
        when(configuration.getChild(KEY)).thenReturn(keyNode);
        when(keyNode.getValue()).thenReturn(KEY);
        assertTrue(helper.getAppTokenCredentialsFromServer(server) instanceof ApplicationTokenCredentials);
        verify(configuration, times(1)).getChild(KEY);
        clearInvocations(configuration);

        /**
         * key and certificate are not configured
         */
        when(configuration.getChild(CLIENT_ID)).thenReturn(clientIdNode);
        when(clientIdNode.getValue()).thenReturn(CLIENT_ID);
        when(configuration.getChild(TENANT_ID)).thenReturn(tenantIdNode);
        when(tenantIdNode.getValue()).thenReturn(TENANT_ID);
        when(configuration.getChild(KEY)).thenReturn(null);
        assertNull(helper.getAppTokenCredentialsFromServer(server));
        verify(configuration, times(1)).getChild(CERTIFICATE);
        clearInvocations(configuration);

        /**
         * certificate does not exist
         */
        when(configuration.getChild(CLIENT_ID)).thenReturn(clientIdNode);
        when(clientIdNode.getValue()).thenReturn(CLIENT_ID);
        when(configuration.getChild(TENANT_ID)).thenReturn(tenantIdNode);
        when(tenantIdNode.getValue()).thenReturn(TENANT_ID);
        when(configuration.getChild(KEY)).thenReturn(null);
        when(configuration.getChild(CERTIFICATE)).thenReturn(certificateNode);
        when(certificateNode.getValue()).thenReturn("/non-existed.txt");
        assertNull(helper.getAppTokenCredentialsFromServer(server));
        verify(configuration, times(1)).getChild(CERTIFICATE_PASSWORD);
        clearInvocations(configuration);

        /**
         * certificate exists
         */
        when(configuration.getChild(CLIENT_ID)).thenReturn(clientIdNode);
        when(clientIdNode.getValue()).thenReturn(CLIENT_ID);
        when(configuration.getChild(TENANT_ID)).thenReturn(tenantIdNode);
        when(tenantIdNode.getValue()).thenReturn(TENANT_ID);
        when(configuration.getChild(KEY)).thenReturn(null);
        when(configuration.getChild(CERTIFICATE)).thenReturn(certificateNode);
        when(certificateNode.getValue()).thenReturn("/certificate.txt");
//        assertTrue(helper.getAppTokenCredentialsFromServer(server) instanceof ApplicationTokenCredentials);
        clearInvocations(configuration);
    }
}
