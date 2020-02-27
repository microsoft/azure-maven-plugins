/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function;

import com.microsoft.azure.common.logging.Log;

import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.MockitoAnnotations;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import java.io.PrintStream;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@RunWith(PowerMockRunner.class)
@PrepareForTest({System.class, ListMojo.class, Log.class, IOUtils.class})
public class ListMojoTest {

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        PowerMockito.mockStatic(Log.class);
    }

    @Test
    public void doExecute() throws Exception {
        PowerMockito.doNothing().when(Log.class);
        Log.info(any(String.class));
        final PrintStream out = mock(PrintStream.class);
        System.setOut(out);
        final ListMojo mojo = new ListMojo();
        mojo.doExecute();
        verify(out, atLeastOnce()).write(any(byte[].class), any(int.class), any(int.class));

    }
}
