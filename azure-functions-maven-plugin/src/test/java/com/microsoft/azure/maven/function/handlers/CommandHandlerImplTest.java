/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class CommandHandlerImplTest {
    @Test
    public void buildCommand() {
        final CommandHandlerImpl handler = new CommandHandlerImpl();
        assertEquals(3, handler.buildCommand("cmd").length);
    }

    @Test
    public void getStdoutRedirect() {
        final CommandHandlerImpl handler = new CommandHandlerImpl();

        assertEquals(ProcessBuilder.Redirect.INHERIT, handler.getStdoutRedirect(true));
        assertEquals(ProcessBuilder.Redirect.PIPE, handler.getStdoutRedirect(false));
    }

    @Test(expected = Exception.class)
    public void handleExitValue() throws Exception {
        final CommandHandlerImpl handler = new CommandHandlerImpl();
        handler.handleExitValue(1, Arrays.asList(0L), "", null);
    }
}
