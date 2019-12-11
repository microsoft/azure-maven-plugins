/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import org.apache.commons.lang3.StringUtils;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.settings.Settings;
import org.codehaus.plexus.util.ReflectionUtils;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Scanner;
import java.util.Set;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;

public class AddMojoTest extends MojoTestBase {
    @Test
    public void getConfiguration() throws Exception {
        final AddMojo mojo = getMojoFromPom();

        assertNull(mojo.getFunctionName());

        assertNull(mojo.getFunctionPackageName());

        assertNull(mojo.getFunctionTemplate());
    }

    @Test
    public void doExecute() throws Exception {
        final AddMojo mojo = getMojoFromPom();
        final Settings settings = new Settings();
        settings.setInteractiveMode(false);
        ReflectionUtils.setVariableValueInObject(mojo, "basedir", new File("target/test"));
        ReflectionUtils.setVariableValueInObject(mojo, "settings", settings);
        mojo.setFunctionTemplate("HttpTrigger");
        mojo.setFunctionName("New-Function");
        mojo.setFunctionPackageName("com.microsoft.azure");

        final File newFunctionFile = new File("target/test/src/main/java/com/microsoft/azure/New_Function.java");
        newFunctionFile.delete();

        mojo.doExecute();

        assertTrue(newFunctionFile.exists());
    }

    @Test(expected = MojoFailureException.class)
    public void doExecuteWithInvalidFunctionName() throws Exception {
        final AddMojo mojo = getMojoFromPom();
        final Settings settings = new Settings();
        settings.setInteractiveMode(false);
        ReflectionUtils.setVariableValueInObject(mojo, "basedir", new File("target/test"));
        ReflectionUtils.setVariableValueInObject(mojo, "settings", settings);
        mojo.setFunctionTemplate("HttpTrigger");
        mojo.setFunctionName("$NewFunction");
        mojo.setFunctionPackageName("com.microsoft.azure");

        mojo.doExecute();
    }

    @Test
    public void assureInputFromUser() throws Exception {
        final AddMojo mojo = getMojoFromPom();
        final AddMojo mojoSpy = spy(mojo);
        final Scanner scanner = mock(Scanner.class);
        doReturn("2").when(scanner).nextLine();
        doReturn(scanner).when(mojoSpy).getScanner();

        final Set<String> set = new HashSet<>();
        mojoSpy.assureInputFromUser("property", "", Arrays.asList("a0", "a1", "a2"), set::add);

        assertTrue(set.contains("a2"));
    }

    @Test(expected = MojoFailureException.class)
    public void assureInputInBatchModeWhenRequired() throws Exception{
        final AddMojo mojo = getMojoFromPom();
        final AddMojo mojoSpy = spy(mojo);

        final Set<String> set = new HashSet<>();
        mojoSpy.assureInputInBatchMode("", StringUtils::isNotEmpty, set::add, true);
    }

    @Test
    public void assureInputInBatchModeWhenNotRequired() throws Exception{
        final AddMojo mojo = getMojoFromPom();
        final AddMojo mojoSpy = spy(mojo);

        final Set<String> set = new HashSet<>();
        mojoSpy.assureInputInBatchMode("a0", StringUtils::isNotEmpty, set::add, true);

        assertTrue(set.contains("a0"));
    }

    private AddMojo getMojoFromPom() throws Exception {
        final AddMojo mojo = (AddMojo) getMojoFromPom("/pom.xml", "add");
        assertNotNull(mojo);
        return mojo;
    }
}
