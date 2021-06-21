/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.legacy.appservice.DeploymentSlotSetting;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.spy;

@RunWith(MockitoJUnitRunner.class)
public class DeployMojoTest extends MojoTestBase {
    private DeployMojo mojo = null;
    private DeployMojo mojoSpy = null;

    @Before
    public void setUp() throws Exception {
        mojo = getMojoFromPom();
        mojoSpy = spy(mojo);
    }

    @Test
    public void getConfiguration() {
        assertEquals("resourceGroupName", mojo.getResourceGroup());

        assertEquals("appName", mojo.getAppName());

        assertEquals("westeurope", mojo.getRegion());
    }

    @Ignore
    @Test(expected = AzureExecutionException.class)
    public void testDeploymentSlotThrowExceptionIfFunctionNotExists() throws AzureExecutionException {
        final DeploymentSlotSetting slotSetting = new DeploymentSlotSetting();
        slotSetting.setName("Exception");
        doReturn(slotSetting).when(mojoSpy).getDeploymentSlotSetting();
        doReturn(null).when(mojoSpy).getFunctionApp();
        mojoSpy.doExecute();
    }

    private DeployMojo getMojoFromPom() throws Exception {
        final DeployMojo mojoFromPom = (DeployMojo) getMojoFromPom("/pom.xml", "deploy");
        assertNotNull(mojoFromPom);
        return mojoFromPom;
    }
}
