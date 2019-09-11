/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.plugin.login;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.azure.AzureEnvironment;
import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.auth.AzureCredential;
import com.microsoft.azure.auth.exception.DesktopNotSupportedException;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.maven.common.telemetry.AppInsightHelper;
import org.apache.commons.io.FileUtils;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.nio.file.Files;

import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.times;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({ AzureAuthHelper.class, AppInsightHelper.class, TelemetryClient.class, LoginMojo.class, Azure.class })
public class LoginMojoTest {
    @Rule
    private MojoRule rule = new MojoRule();

    private LoginMojo mojo;

    @Before
    public void setUp() throws Exception {

        TestHelper.mockAppInsightHelper();

        final File pom = new File(this.getClass().getResource("/maven/projects/simple/pom.xml").getFile());
        assertNotNull(pom);
        assertTrue(pom.exists());
        mojo = (LoginMojo) rule.lookupMojo("login", pom);
        final MojoExecution execution = rule.newMojoExecution("login");
        assertNotNull(mojo);
        mojo.plugin = execution.getMojoDescriptor().getPluginDescriptor();

    }

    @Test
    public void testFirstLogin() throws Exception {
        final AzureCredential credExpected = AzureCredential.fromAuthenticationResult(TestHelper.createAuthenticationResult());
        final File tempDirectory = Files.createTempDirectory("azure-auth-helper-test").toFile();
        final File secretFile = new File(tempDirectory, "azure-secret.json");
        mockStatic(AzureAuthHelper.class);

        when(AzureAuthHelper.getAzureEnvironment(null)).thenReturn(AzureEnvironment.AZURE);
        when(AzureAuthHelper.getAzureSecretFile()).thenReturn(secretFile);
        when(AzureAuthHelper.existsAzureSecretFile()).thenReturn(false);

        when(AzureAuthHelper.oAuthLogin(AzureEnvironment.AZURE)).thenReturn(credExpected);
        mojo.isTelemetryAllowed = false;
        mojo.execute();
        tempDirectory.delete();

        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.getAzureEnvironment(null);
        verifyStatic(AzureAuthHelper.class, times(2));
        AzureAuthHelper.existsAzureSecretFile();
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.oAuthLogin(any());
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.getAzureSecretFile();
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.writeAzureCredentials(credExpected, secretFile);

        verifyNoMoreInteractions(AzureAuthHelper.class);
    }

    @Test
    public void testSecondTimeLogin() throws Exception {
        final AzureCredential credExpected = AzureCredential.fromAuthenticationResult(TestHelper.createAuthenticationResult());
        credExpected.setDefaultSubscription("default_subs_for_test");
        credExpected.setEnvironment("azure_china");
        final File tempDirectory = Files.createTempDirectory("azure-auth-helper-test").toFile();
        final File secretFile = new File(tempDirectory, "azure-secret.json");
        FileUtils.copyFile(new File(this.getClass().getClassLoader().getResource("auth/azure-secret.json").getFile()), secretFile);
        mockStatic(AzureAuthHelper.class);
        when(AzureAuthHelper.readAzureCredentials(any())).thenReturn(credExpected);
        when(AzureAuthHelper.existsAzureSecretFile()).thenReturn(true);
        when(AzureAuthHelper.getAzureEnvironment("azure_china")).thenReturn(AzureEnvironment.AZURE_CHINA);
        when(AzureAuthHelper.getAzureSecretFile()).thenReturn(secretFile);
        when(AzureAuthHelper.oAuthLogin(AzureEnvironment.AZURE_CHINA)).thenReturn(credExpected);
        final AzureTokenCredentials mockTokenCred = mock(AzureTokenCredentials.class);
        when(AzureAuthHelper.getMavenAzureLoginCredentials(credExpected, AzureEnvironment.AZURE_CHINA)).thenReturn(mockTokenCred);

        TestHelper.mockAzureWithSubs(TestHelper.createOneMockSubscriptions());

        mojo.isTelemetryAllowed = false;
        mojo.environment = "azure_china";
        mojo.execute();
        secretFile.delete();
        tempDirectory.delete();

        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.getAzureEnvironment("azure_china");
        verifyStatic(AzureAuthHelper.class, times(2));
        AzureAuthHelper.existsAzureSecretFile();
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.oAuthLogin(any());
        verifyStatic(AzureAuthHelper.class, atLeast(1));
        AzureAuthHelper.getAzureSecretFile();
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.writeAzureCredentials(any(), eq(secretFile));
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.readAzureCredentials(any());
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.getMavenAzureLoginCredentials(any(), any());
        verifyNoMoreInteractions(AzureAuthHelper.class);
    }

    @Test
    public void testDeviceLogin() throws Exception {
        final AzureCredential credExpected = AzureCredential.fromAuthenticationResult(TestHelper.createAuthenticationResult());
        final File tempDirectory = Files.createTempDirectory("azure-auth-helper-test").toFile();
        final File secretFile = new File(tempDirectory, "azure-secret.json");
        mockStatic(AzureAuthHelper.class);

        when(AzureAuthHelper.getAzureEnvironment(null)).thenReturn(AzureEnvironment.AZURE);
        when(AzureAuthHelper.getAzureSecretFile()).thenReturn(secretFile);
        when(AzureAuthHelper.existsAzureSecretFile()).thenReturn(false);

        when(AzureAuthHelper.deviceLogin(AzureEnvironment.AZURE)).thenReturn(credExpected);
        mojo.isTelemetryAllowed = false;
        mojo.devicelogin = true;
        mojo.execute();
        tempDirectory.delete();

        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.getAzureEnvironment(null);
        verifyStatic(AzureAuthHelper.class, times(2));
        AzureAuthHelper.existsAzureSecretFile();
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.deviceLogin(any());
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.getAzureSecretFile();
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.writeAzureCredentials(any(), eq(secretFile));

        verifyNoMoreInteractions(AzureAuthHelper.class);
    }

    @Test
    public void testFallbackDeviceLogin() throws Exception {
        final AzureCredential credExpected = AzureCredential.fromAuthenticationResult(TestHelper.createAuthenticationResult());
        final File tempDirectory = Files.createTempDirectory("azure-auth-helper-test").toFile();
        final File secretFile = new File(tempDirectory, "azure-secret.json");
        mockStatic(AzureAuthHelper.class);

        when(AzureAuthHelper.getAzureEnvironment(null)).thenReturn(AzureEnvironment.AZURE);
        when(AzureAuthHelper.getAzureSecretFile()).thenReturn(secretFile);
        when(AzureAuthHelper.existsAzureSecretFile()).thenReturn(false);
        when(AzureAuthHelper.oAuthLogin(AzureEnvironment.AZURE)).thenThrow(DesktopNotSupportedException.class);
        when(AzureAuthHelper.deviceLogin(AzureEnvironment.AZURE)).thenReturn(credExpected);
        mojo.isTelemetryAllowed = false;
        mojo.devicelogin = false;
        mojo.execute();
        tempDirectory.delete();

        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.getAzureEnvironment(null);
        verifyStatic(AzureAuthHelper.class, times(2));
        AzureAuthHelper.existsAzureSecretFile();
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.oAuthLogin(any());
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.deviceLogin(any());
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.getAzureSecretFile();
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.writeAzureCredentials(any(), eq(secretFile));

        verifyNoMoreInteractions(AzureAuthHelper.class);
    }

    @Test
    public void testUncaughtException() throws Exception {
        mockStatic(AzureAuthHelper.class);
        when(AzureAuthHelper.getAzureEnvironment(null)).thenThrow(IllegalArgumentException.class);
        try {
            mojo.execute();
            fail("should throw IAE");
        } catch (IllegalArgumentException e) {
            // expected
        }
    }
}
