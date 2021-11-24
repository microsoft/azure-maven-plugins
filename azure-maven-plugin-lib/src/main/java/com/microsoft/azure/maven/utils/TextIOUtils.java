/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.utils;

import org.beryx.textio.TextTerminal;
import org.beryx.textio.console.ConsoleTextTerminalProvider;
import org.beryx.textio.jline.JLineTextTerminalProvider;
import org.beryx.textio.swing.SwingTextTerminalProvider;
import org.beryx.textio.system.SystemTextTerminalProvider;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;

public class TextIOUtils {
    private static TextTerminal textTerminal;
    private static List<Supplier<TextTerminal>> terminalSupplierList = Arrays.asList(
            () -> (new JLineTextTerminalProvider()).getTextTerminal(),
            () -> (new ConsoleTextTerminalProvider()).getTextTerminal(),
            () -> (new SwingTextTerminalProvider()).getTextTerminal(),
            () -> (new SystemTextTerminalProvider()).getTextTerminal()
    );

    public static synchronized TextTerminal getTextTerminal() {
        if (textTerminal == null) {
            for (Supplier<TextTerminal> supplier : terminalSupplierList) {
                try {
                    textTerminal = supplier.get();
                } catch (Throwable throwable) {
                    // swallow exception when initialize terminal
                }
                if (textTerminal != null) {
                    break;
                }
            }
        }
        return textTerminal;
    }
}
