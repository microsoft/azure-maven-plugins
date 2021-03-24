/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils.aspect;

import com.google.common.collect.ImmutableMap;
import com.microsoft.azure.management.resources.fluentcore.arm.ResourceUtils;
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.groovy.runtime.MethodClosure;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;

@Log
public class ExpressionUtils {
    private static final ImmutableMap<String, Boolean> valueMap = ImmutableMap.of("true", true, "false", false);
    private static final SimpleTemplateEngine engine = new SimpleTemplateEngine();
    private static final String INVALID_TEMPLATE = "error occurs when evaluate template(%s) with bindings(%s)";

    public static boolean evaluate(@Nonnull final String expression, @Nonnull final MethodInvocation invocation, boolean defaultVal) {
        final String result = interpret(expression, invocation);
        return valueMap.getOrDefault(Optional.ofNullable(result).map(String::toLowerCase).orElse(null), defaultVal);
    }

    public static String interpret(@Nonnull final String expression, @Nonnull final MethodInvocation invocation) {
        return render(String.format("${%s}", expression), invocation);
    }

    public static String render(@Nullable final String template, @Nonnull final MethodInvocation invocation) {
        if (StringUtils.isBlank(template) || !template.contains("$")) { // no groovy expression, just return
            return template;
        }
        final Map<String, Object> bindings = initBindings(invocation);
        final String fixed = template.replaceAll("(\\W)this(\\.)", "$1_this_$2"); // resolve `this`
        try {
            final Template tpl = engine.createTemplate(fixed);
            return tpl.make(bindings).toString();
        } catch (final ClassNotFoundException | IOException e) {
            log.log(Level.SEVERE, String.format(INVALID_TEMPLATE, template, bindings), e);
        }
        return template;
    }

    @Nonnull
    private static Map<String, Object> initBindings(@Nonnull final MethodInvocation invocation) {
        final String[] paramNames = invocation.getParamNames();
        final Object[] paramValues = invocation.getParamValues();
        final Map<String, Object> bindings = new HashMap<>();
        for (int i = 0; i < paramNames.length; i++) {
            bindings.put(paramNames[i], paramValues[i]);
        }
        bindings.put("_this_", invocation.getInstance());
        bindPredefinedFunctions(bindings);
        return bindings;
    }

    private static void bindPredefinedFunctions(@Nonnull Map<String, Object> bindings) {
        bindings.put("nameFromResourceId", new MethodClosure(ResourceUtils.class, "nameFromResourceId"));
    }
}
