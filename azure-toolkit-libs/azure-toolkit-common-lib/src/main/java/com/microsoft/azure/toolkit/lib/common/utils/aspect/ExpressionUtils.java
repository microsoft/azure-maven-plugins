/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.utils.aspect;

import com.azure.resourcemanager.resources.fluentcore.arm.ResourceUtils;
import com.google.common.collect.ImmutableMap;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import groovy.text.SimpleTemplateEngine;
import groovy.text.Template;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Triple;
import org.codehaus.groovy.runtime.MethodClosure;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.lang.reflect.Parameter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Slf4j
public class ExpressionUtils {
    private static final ImmutableMap<String, Boolean> valueMap = ImmutableMap.of("true", true, "false", false);
    private static final SimpleTemplateEngine engine = new SimpleTemplateEngine();
    private static final String INVALID_TEMPLATE = "error occurs when evaluate template(%s) with bindings(%s)";

    public static boolean evaluate(@Nonnull final String expression, @Nonnull final MethodInvocation invocation, boolean defaultVal) {
        final String result = interpret(expression, invocation);
        return Boolean.TRUE.equals(valueMap.getOrDefault(Optional.ofNullable(result).map(String::toLowerCase).orElse(null), defaultVal));
    }

    public static String interpret(@Nonnull final String expression, @Nonnull final MethodInvocation invocation) {
        return render(String.format("${%s}", expression), invocation);
    }

    @Nullable
    public static Object evaluate(@Nonnull final String expression, @Nonnull final MethodInvocation invocation) {
        if (StringUtils.isBlank(expression)) { // no groovy expression, just return
            return null;
        }
        final Map<String, Object> bindings = initBindings(invocation);
        final String fixed = expression.replaceAll("(\\W)this(\\.)", "$1_this_$2"); // resolve `this`
        try {
            final GroovyShell shell = new GroovyShell(ExpressionUtils.class.getClassLoader(), new Binding(bindings));
            return shell.evaluate(fixed);
        } catch (final Throwable e) { // swallow all exceptions during render
            log.warn(String.format(INVALID_TEMPLATE, expression, bindings), e);
        }
        return null;
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
        } catch (final Throwable e) { // swallow all exceptions during render
            log.warn(String.format(INVALID_TEMPLATE, template, bindings), e);
        }
        return template;
    }

    @Nonnull
    private static Map<String, Object> initBindings(@Nonnull final MethodInvocation invocation) {
        final List<Triple<String, Parameter, Object>> args = invocation.getArgs();
        final Map<String, Object> bindings = new HashMap<>();
        for (final Triple<String, Parameter, Object> arg : args) {
            bindings.put(arg.getLeft(), arg.getRight());
        }
        bindings.put("_this_", invocation.getInstance());
        bindPredefinedFunctions(bindings);
        return bindings;
    }

    private static void bindPredefinedFunctions(@Nonnull Map<String, Object> bindings) {
        bindings.put("nameFromResourceId", new MethodClosure(ResourceUtils.class, "nameFromResourceId"));
    }
}
