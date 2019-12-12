/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import org.apache.maven.plugin.logging.Log;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;

public class CommandHandlerImplTest {
    @Test
    public void buildCommand() {
        final Log log = mock(Log.class);
        final CommandHandlerImpl handler = new CommandHandlerImpl();
        assertEquals(3, handler.buildCommand("cmd").length);
    }

    @Test
    public void getStdoutRedirect() {
        final Log log = mock(Log.class);
        final CommandHandlerImpl handler = new CommandHandlerImpl();

        assertEquals(ProcessBuilder.Redirect.INHERIT, handler.getStdoutRedirect(true));
        assertEquals(ProcessBuilder.Redirect.PIPE, handler.getStdoutRedirect(false));
    }

    @Test(expected = Exception.class)
    public void handleExitValue() throws Exception {
        final Log log = mock(Log.class);
        final CommandHandlerImpl handler = new CommandHandlerImpl();
        doNothing().when(log).error(anyString());
        doNothing().when(log).debug(anyString());

        handler.handleExitValue(1, Arrays.asList(0L), "", null);
    }
}
