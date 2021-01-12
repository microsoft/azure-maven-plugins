/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.webapp.utils;

import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.beryx.textio.GenericInputReader;
import org.beryx.textio.InputReader;
import org.beryx.textio.TextTerminal;

import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.StringUtils;
import com.microsoft.azure.common.utils.TextUtils;

/**
 * A custom InputReader for handing the custom list prompt text rather than "Enter your choice" which is not changeable in Text.IO.
 */
public class CustomTextIoStringListReader<T> extends GenericInputReader<T> {
    private String customPrompt;

    public CustomTextIoStringListReader(Supplier<TextTerminal<?>> textTerminalSupplier,
            Function<String, ParseResult<T>> parser) {
        super(textTerminalSupplier, parser);
    }

    public InputReader<T, GenericInputReader<T>> withCustomPrompt(String customPrompt) {
        this.customPrompt = customPrompt;
        return this;
    }

    @Override
    protected void printPrompt(List<String> prompt, TextTerminal<?> textTerminal) {
        textTerminal.print(prompt);
        boolean useColon = false;
        if (promptAdjustments && prompt != null && !prompt.isEmpty()) {
            final String lastLine = prompt.get(prompt.size() - 1);
            useColon = anotherShouldAppendColon(lastLine);
        }
        if (possibleValues == null) {
            if (promptAdjustments && defaultValue != null) {
                textTerminal.print(" [" + valueFormatter.apply(defaultValue) + "]: ");
            } else {
                textTerminal.print(useColon ? ": " : " ");
            }
        } else if (promptAdjustments) {
            final int optionCount = possibleValues.size();
            if (inlinePossibleValues) {
                final String strValues = IntStream.range(0, optionCount)
                        .mapToObj(i -> possibleValues.get(i))
                        .map(option -> {
                            final boolean isDefault = (defaultValue != null) && equalsFunc.apply(defaultValue, option);
                            return (isDefault ? "*" : "") + valueFormatter.apply(option);
                        })
                        .collect(Collectors.joining(", ", " (", "): "));
                textTerminal.print(strValues);
            } else {
                textTerminal.println(useColon ? ":" : "");
                String defaultOptionText = null;
                for (int i = 0; i < optionCount; i++) {
                    final T option = possibleValues.get(i);
                    final boolean isDefault = (defaultValue != null) && equalsFunc.apply(defaultValue, option);
                    String optionId = "";
                    String optionText = valueFormatter.apply(option);
                    if (numberedPossibleValues) {
                        final int digits = ("" + optionCount).length();
                        optionId = String.format("%" + digits + "d: ", i + 1);
                        final String[] textLines = optionText.split("\\R", -1);
                        if (textLines.length > 1) {
                            final String delimiter = String.format("\n%" + (digits + 4) + "s", "");
                            optionText = Arrays.stream(textLines).collect(Collectors.joining(delimiter));
                        }
                    }

                    if (isDefault) {
                        defaultOptionText = optionId + optionText;
                        textTerminal.println(TextUtils.blue("* " + optionId + optionText));
                    } else {
                        textTerminal.println("  " + optionId + optionText);
                    }
                }
                if (StringUtils.isNotBlank(customPrompt)) {
                    textTerminal.print(customPrompt);
                } else {
                    textTerminal.print(String.format("Enter your choice%s: ", StringUtils.isNotBlank(defaultOptionText) ? String.format("[%s]",
                        TextUtils.blue(defaultOptionText)) : ""));
                }
            }
        }
    }

    /**
     * shouldappendColon in InputReader is private, we need to call it in printPrompt, so we make a copied version which is exact same as shouldappendColon
     */
    private static boolean anotherShouldAppendColon(String s) {
        if (s == null || s.isEmpty()) {
            return false;
        }
        final char lastChar = s.charAt(s.length() - 1);
        return "()[]{}".indexOf(lastChar) > 0 || Character.isJavaIdentifierPart(lastChar);
    }
}
