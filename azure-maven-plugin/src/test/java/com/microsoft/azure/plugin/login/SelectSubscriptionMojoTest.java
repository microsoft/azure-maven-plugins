/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.plugin.login;

import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.azure.auth.AzureAuthHelper;
import com.microsoft.azure.auth.AzureCredential;
import com.microsoft.azure.auth.Constants;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.resources.Subscriptions;
import com.microsoft.azure.maven.common.telemetry.AppInsightHelper;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.plugin.testing.MojoRule;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Scanner;

import static junit.framework.TestCase.assertFalse;
import static junit.framework.TestCase.assertNotNull;
import static junit.framework.TestCase.assertTrue;
import static junit.framework.TestCase.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.verifyNoMoreInteractions;
import static org.powermock.api.mockito.PowerMockito.verifyStatic;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.powermock.api.mockito.PowerMockito.whenNew;

@RunWith(PowerMockRunner.class)
@PowerMockIgnore({"javax.net.ssl.*","javax.security.*"})
@PrepareForTest({ AzureAuthHelper.class, AppInsightHelper.class, TelemetryClient.class, SelectSubscriptionMojo.class, Azure.class, Scanner.class })
public class SelectSubscriptionMojoTest {
    @Rule
    private MojoRule rule = new MojoRule();

    private SelectSubscriptionMojo mojo;

    @Before
    public void setUp() throws Exception {
        TestHelper.mockAppInsightHelper();

        final File pom = new File(this.getClass().getResource("/maven/projects/simple/pom.xml").getFile());
        assertNotNull(pom);
        assertTrue(pom.exists());
        mojo = (SelectSubscriptionMojo) rule.lookupMojo("select-subscription", pom);
        final MojoExecution execution = rule.newMojoExecution("select-subscription");
        assertNotNull(mojo);
        mojo.plugin = execution.getMojoDescriptor().getPluginDescriptor();
    }

    @Test
    public void testSelectSubsBeforeLogin() throws Exception {
        TestHelper.injectEnvironmentVariable(Constants.AZURE_CONFIG_DIR, "non-exist-config-root");
        assertFalse(AzureAuthHelper.existsAzureSecretFile());
        mojo.isTelemetryAllowed = false;
        try {
            mojo.execute();
            fail("Should fail when the user is not logged in");
        } catch (MojoFailureException ex) {
            // expected
        }
    }

    @Test
    public void testSelectTheOnlySubs() throws Exception {
        final File tempDirectory = Files.createTempDirectory("azure-auth-helper-test").toFile();
        final File secretFile = new File(tempDirectory, "azure-secret.json");
        mockStatic(AzureAuthHelper.class);
        TestHelper.mockAzureWithSubs(TestHelper.createOneMockSubscriptions());
        final AzureTokenCredentials mockTokenCred = mock(AzureTokenCredentials.class);
        when(AzureAuthHelper.getMavenAzureLoginCredentials()).thenReturn(mockTokenCred);
        final AzureCredential credExpected = AzureCredential.fromAuthenticationResult(TestHelper.createAuthenticationResult());
        credExpected.setDefaultSubscription("old_subs_id");
        credExpected.setEnvironment("azure_china");
        when(AzureAuthHelper.existsAzureSecretFile()).thenReturn(true);
        when(AzureAuthHelper.getAzureSecretFile()).thenReturn(secretFile);
        when(AzureAuthHelper.readAzureCredentials()).thenReturn(credExpected);
        mojo.execute();
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.getAzureSecretFile();
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.existsAzureSecretFile();
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.writeAzureCredentials(any(), eq(secretFile));
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.getMavenAzureLoginCredentials();
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.readAzureCredentials();
        tempDirectory.delete();
        verifyNoMoreInteractions(AzureAuthHelper.class);

    }

    @Test
    public void testSelectNoSubs() throws Exception {
        final File tempDirectory = Files.createTempDirectory("azure-auth-helper-test").toFile();
        final File secretFile = new File(tempDirectory, "azure-secret.json");

        mockStatic(AzureAuthHelper.class);
        TestHelper.mockAzureWithSubs(TestHelper.createEmptyMockSubscriptions());
        when(AzureAuthHelper.existsAzureSecretFile()).thenReturn(true);

        final AzureTokenCredentials mockTokenCred = mock(AzureTokenCredentials.class);
        when(mockTokenCred.defaultSubscriptionId()).thenReturn("old_subs_id");
        when(AzureAuthHelper.getMavenAzureLoginCredentials()).thenReturn(mockTokenCred);
        final AzureCredential credExpected = AzureCredential.fromAuthenticationResult(TestHelper.createAuthenticationResult());
        credExpected.setDefaultSubscription("old_subs_id");
        credExpected.setEnvironment("azure_china");
        when(AzureAuthHelper.getAzureSecretFile()).thenReturn(secretFile);

        try {
            mojo.execute();
            fail("should throw exception when there is no subscriptions.");
        } catch (MojoExecutionException ex) {
            // expected
        }

        tempDirectory.delete();
    }

    @Test
    public void testSelectBySubsId() throws Exception {
        final File tempDirectory = Files.createTempDirectory("azure-auth-helper-test").toFile();
        final File secretFile = new File(tempDirectory, "azure-secret.json");
        mockStatic(AzureAuthHelper.class);
        final Subscriptions subs = TestHelper.createTwoMockSubscriptions();
        TestHelper.mockAzureWithSubs(subs);
        when(AzureAuthHelper.existsAzureSecretFile()).thenReturn(true);
        final AzureTokenCredentials mockTokenCred = mock(AzureTokenCredentials.class);
        when(mockTokenCred.defaultSubscriptionId()).thenReturn("old_subs_id");
        when(AzureAuthHelper.getMavenAzureLoginCredentials()).thenReturn(mockTokenCred);
        final AzureCredential credExpected = AzureCredential.fromAuthenticationResult(TestHelper.createAuthenticationResult());
        credExpected.setDefaultSubscription("old_subs_id");
        credExpected.setEnvironment("azure_china");
        when(AzureAuthHelper.getAzureSecretFile()).thenReturn(secretFile);
        when(AzureAuthHelper.readAzureCredentials()).thenReturn(credExpected);
        FieldUtils.writeField(mojo, "subscription", "new_subs_id2", true);
        mojo.execute();
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.getAzureSecretFile();
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.existsAzureSecretFile();
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.writeAzureCredentials(any(), eq(secretFile));
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.getMavenAzureLoginCredentials();
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.readAzureCredentials();
        tempDirectory.delete();
        verifyNoMoreInteractions(AzureAuthHelper.class);
    }

    @Test
    public void testSelectByUser() throws Exception {
        final File tempDirectory = Files.createTempDirectory("azure-auth-helper-test").toFile();
        final File secretFile = new File(tempDirectory, "azure-secret.json");
        mockStatic(AzureAuthHelper.class);
        final Subscriptions subs = TestHelper.createTwoMockSubscriptions();
        TestHelper.mockAzureWithSubs(subs);
        when(AzureAuthHelper.existsAzureSecretFile()).thenReturn(true);
        final AzureTokenCredentials mockTokenCred = mock(AzureTokenCredentials.class);
        when(mockTokenCred.defaultSubscriptionId()).thenReturn("old_subs_id");
        when(AzureAuthHelper.getMavenAzureLoginCredentials()).thenReturn(mockTokenCred);
        final AzureCredential credExpected = AzureCredential.fromAuthenticationResult(TestHelper.createAuthenticationResult());
        credExpected.setDefaultSubscription("old_subs_id");
        credExpected.setEnvironment("azure_china");
        when(AzureAuthHelper.getAzureSecretFile()).thenReturn(secretFile);
        when(AzureAuthHelper.readAzureCredentials()).thenReturn(credExpected);
        final Scanner mockScanner = mock(Scanner.class);
        when(mockScanner.nextLine()).thenReturn("2");
        whenNew(Scanner.class).withAnyArguments().thenReturn(mockScanner);
        mojo.execute();
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.getAzureSecretFile();
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.existsAzureSecretFile();
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.writeAzureCredentials(any(), eq(secretFile));
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.getMavenAzureLoginCredentials();
        verifyStatic(AzureAuthHelper.class);
        AzureAuthHelper.readAzureCredentials();
        tempDirectory.delete();
        verifyNoMoreInteractions(AzureAuthHelper.class);
    }

    @Test
    public void testIOExceptionReadingCredentials() throws Exception {
        mockStatic(AzureAuthHelper.class);
        when(AzureAuthHelper.existsAzureSecretFile()).thenReturn(true);
        when(AzureAuthHelper.getMavenAzureLoginCredentials()).thenThrow(IOException.class);

        try {
            mojo.execute();
            fail("Should throw exception when there is an IOException during reading credentials .");
        } catch (MojoFailureException ex) {
            //expected
        }
    }

    @Test
    public void testIOExceptionWritingCredentials() throws Exception {
        final AzureCredential azureCredential = mock(AzureCredential.class);
        final File azureFile = mock(File.class);

        mockStatic(AzureAuthHelper.class);
        PowerMockito.doThrow(new IOException()).when(AzureAuthHelper.class);
        AzureAuthHelper.writeAzureCredentials(azureCredential, azureFile);
        when(AzureAuthHelper.existsAzureSecretFile()).thenReturn(true);

        TestHelper.mockAzureWithSubs(TestHelper.createOneMockSubscriptions());
        final AzureTokenCredentials mockTokenCred = mock(AzureTokenCredentials.class);
        when(AzureAuthHelper.getMavenAzureLoginCredentials()).thenReturn(mockTokenCred);
        when(azureCredential.getDefaultSubscription()).thenReturn("old_subs_id");
        when(azureCredential.getEnvironment()).thenReturn("azure_china");
        when(AzureAuthHelper.existsAzureSecretFile()).thenReturn(true);
        when(AzureAuthHelper.getAzureSecretFile()).thenReturn(azureFile);
        when(AzureAuthHelper.readAzureCredentials()).thenReturn(azureCredential);

        try {
            mojo.execute();
            fail("Should throw exception when there is an IOException during reading credentials .");
        } catch (MojoExecutionException ex) {
            //expected
        }
    }

    @Test
    public void testBadSubscriptionArgument() throws Exception {
        mockStatic(AzureAuthHelper.class);
        when(AzureAuthHelper.existsAzureSecretFile()).thenReturn(true);
        final AzureTokenCredentials mockTokenCred = mock(AzureTokenCredentials.class);
        when(mockTokenCred.defaultSubscriptionId()).thenReturn("old_subs_id");
        when(AzureAuthHelper.getMavenAzureLoginCredentials()).thenReturn(mockTokenCred);
        final Subscriptions subs = TestHelper.createTwoMockSubscriptions();
        TestHelper.mockAzureWithSubs(subs);
        FieldUtils.writeField(mojo, "subscription", "bad-subscription-id", true);
        try {
            mojo.execute();
            fail("Should throw exception when there is an IOException during reading credentials .");
        } catch (MojoFailureException ex) {
            //expected
        }
    }
}
