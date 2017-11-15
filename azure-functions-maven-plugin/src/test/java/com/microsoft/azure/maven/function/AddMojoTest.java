/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

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
        ReflectionUtils.setVariableValueInObject(mojo, "basedir", new File("target/test"));
        mojo.setFunctionTemplate("HttpTrigger");
        mojo.setFunctionName("NewFunction");
        mojo.setFunctionPackageName("com.microsoft.azure");

        final File newFunctionFile = new File("target/test/src/main/java/com/microsoft/azure/NewFunction.java");
        newFunctionFile.delete();

        mojo.doExecute();

        assertTrue(newFunctionFile.exists());
    }

    @Test
    public void assureInputFromUserInteractively() throws Exception {
        final AddMojo mojo = getMojoFromPom();
        final AddMojo mojoSpy = spy(mojo);
        final Scanner scanner = mock(Scanner.class);
        final Settings settings = mock(Settings.class);
        doReturn("2").when(scanner).nextLine();
        doReturn(scanner).when(mojoSpy).getScanner();
        doReturn(settings).when(mojoSpy).getSettings();
        doReturn(true).when(settings).isInteractiveMode();

        final Set<String> set = new HashSet<>();
        mojoSpy.assureInputFromUser("property", "", Arrays.asList("a0", "a1", "a2"), set::add, true);

        assertTrue(set.contains("a2"));
    }

    @Test(expected = MojoFailureException.class)
    public void assureInputFromUserNonInteractively() throws Exception{
        final AddMojo mojo = getMojoFromPom();
        final AddMojo mojoSpy = spy(mojo);
        final Settings settings = mock(Settings.class);
        doReturn(settings).when(mojoSpy).getSettings();
        doReturn(false).when(settings).isInteractiveMode();

        final Set<String> set = new HashSet<>();
        mojoSpy.assureInputFromUser("property", "", Arrays.asList("a0", "a1", "a2"), set::add, true);
    }

    private AddMojo getMojoFromPom() throws Exception {
        final AddMojo mojo = (AddMojo) getMojoFromPom("/pom.xml", "add");
        assertNotNull(mojo);
        return mojo;
    }
}
