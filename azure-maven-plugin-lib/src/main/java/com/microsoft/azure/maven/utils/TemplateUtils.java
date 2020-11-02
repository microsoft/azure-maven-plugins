/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.utils;

import com.microsoft.azure.common.utils.TextUtils;
import org.jtwig.JtwigModel;
import org.jtwig.JtwigTemplate;

import java.util.Map;
import java.util.Objects;

public class TemplateUtils {
    /**
     * Evaluate the template expression to boolean using a variable map.
     * @param expr the expression
     * @param variableMap the variable map contains all the variables referenced in express
     * @return whether or not the evaluated text is "true" or "false", default to false
     */
    public static Boolean evalBoolean(String expr, Map<String, Object> variableMap) {
        final String text = evalPlainText(expr, variableMap);

        if (text == null) {
            return Boolean.FALSE;
        }
        return Boolean.valueOf(text);
    }

    /**
     * Evaluate the template expression using a variable map.
     * @param expr the expression
     * @param variableMap the variable map contains all the variables referenced in express
     * @return the evaluated text with color applied.
     */
    public static String evalText(String expr, Map<String, Object> variableMap) {
        // convert *** to blue color
        return evalPlainText(expr, variableMap).replaceAll("\\*\\*\\*(.*?)\\*\\*\\*", TextUtils.blue("$1"));
    }

    /**
     * Evaluate the template expression using a variable map.
     * @param expr the expression
     * @param variableMap the variable map contains all the variables referenced in express
     * @return the evaluated text.
     */

    public static String evalPlainText(String expr, Map<String, Object> variableMap) {
        String text = expr.contains(".") ? evalInline(expr, variableMap) :
                Objects.toString(variableMap.get(expr), null);
        int evalCount = 0;
        while (text != null && text.contains("{{")) {
            text = eval(text, variableMap);
            evalCount++;
            if (evalCount > 5) {
                break;
            }
        }
        return text;

    }

    private static String eval(String expr, Map<String, Object> variableMap) {
        final JtwigTemplate template = JtwigTemplate.inlineTemplate(expr);
        final JtwigModel model = JtwigModel.newModel();
        variableMap.entrySet().forEach((t) -> model.with(t.getKey(), t.getValue()));

        return template.render(model);
    }

    private static String evalInline(String expr, Map<String, Object> variableMap) {
        return eval(String.format("{{%s}}", expr), variableMap);
    }

    private TemplateUtils() {

    }

}
