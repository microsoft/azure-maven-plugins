/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.maven.springcloud;

import com.microsoft.azure.toolkit.lib.common.utils.TextUtils;
import groovy.lang.MissingPropertyException;
import groovy.text.SimpleTemplateEngine;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class TemplateUtils {
    private static final SimpleTemplateEngine engine = new SimpleTemplateEngine();
    private static final String INVALID_TEMPLATE = "error occurs when evaluating template(%s) with bindings(%s)";
    private static final String MISSING_PROPERTY = "some properties are missing when evaluating template(%s) with bindings(%s)";

    /**
     * Evaluate the template expression to boolean using a variable map.
     *
     * @param expr        the expression
     * @param variableMap the variable map contains all the variables referenced in express
     * @return whether the evaluated text is "true" or "false", default to false
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
     *
     * @param expr        the expression
     * @param variableMap the variable map contains all the variables referenced in express
     * @return the evaluated text with color applied.
     */
    public static String evalText(String expr, Map<String, Object> variableMap) {
        // convert *** to blue color
        return evalPlainText(expr, variableMap).replaceAll("\\*\\*\\*(.*?)\\*\\*\\*", TextUtils.blue("$1"));
    }

    /**
     * Evaluate the template expression using a variable map.
     *
     * @param expr        the expression
     * @param variableMap the variable map contains all the variables referenced in express
     * @return the evaluated text.
     */

    public static String evalPlainText(String expr, Map<String, Object> variableMap) {
        String text = expr.contains(".") ? evalInline(expr, variableMap) :
            Objects.toString(variableMap.get(expr), null);
        int evalCount = 0;
        while (text != null && text.contains("${")) {
            final String prev = text;
            text = eval(text, variableMap);
            evalCount++;
            if (StringUtils.equals(prev, text) || evalCount > 5) {
                break;
            }
        }
        return text;
    }

    private static String eval(String template, Map<String, Object> bindings) {
        try {
            return engine.createTemplate(template).make(bindings).toString();
        } catch (MissingPropertyException e) {
            log.debug(String.format(MISSING_PROPERTY, template, bindings));
        } catch (Exception e) {
            log.warn(String.format(INVALID_TEMPLATE, template, bindings), e);
        }
        return template;
    }

    private static String evalInline(String expr, Map<String, Object> variableMap) {
        return eval(String.format("${%s}", expr), variableMap);
    }

    /**
     * evaluate expression value, returns empty string if there is no correspond value in variable map
     */
    public static String evalExpressionValue(@Nonnull final String expr, @Nonnull final Map<String, Object> variableMap) {
        final String result = evalText(expr, variableMap);
        return StringUtils.equals(result, String.format("${%s}", expr)) ? StringUtils.EMPTY : result;
    }

    private TemplateUtils() {

    }

}
