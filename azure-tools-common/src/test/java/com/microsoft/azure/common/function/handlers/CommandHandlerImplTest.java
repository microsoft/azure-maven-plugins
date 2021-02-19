/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.function.handlers;

import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class CommandHandlerImplTest {
    @Test
    public void buildCommand() {
        assertEquals(3, CommandHandlerImpl.buildCommand("cmd").length);
    }

    @Test
    public void getStdoutRedirect() {
        assertEquals(ProcessBuilder.Redirect.INHERIT, CommandHandlerImpl.getStdoutRedirect(true));
        assertEquals(ProcessBuilder.Redirect.PIPE, CommandHandlerImpl.getStdoutRedirect(false));
    }

    @Test(expected = Exception.class)
    public void handleExitValue() throws Exception {
        final CommandHandlerImpl handler = new CommandHandlerImpl();
        handler.handleExitValue(1, Arrays.asList(0L), "", null);
    }
}
