/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.common.function.handlers;

import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.common.function.bindings.Binding;
import com.microsoft.azure.common.function.bindings.BindingEnum;
import com.microsoft.azure.common.function.bindings.BindingFactory;
import com.microsoft.azure.common.function.configurations.FunctionConfiguration;
import com.microsoft.azure.common.function.configurations.Retry;
import com.microsoft.azure.common.logging.Log;
import com.microsoft.azure.functions.annotation.ExponentialBackoffRetry;
import com.microsoft.azure.functions.annotation.FixedDelayRetry;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.StorageAccount;

import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.MethodAnnotationsScanner;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

public class AnnotationHandlerImpl implements AnnotationHandler {

    private static final String MULTI_RETRY_ANNOTATION = "Fixed delay retry and exponential backoff retry are not compatible, " +
            "please use either of them for one trigger";

    @Override
    public Set<Method> findFunctions(final List<URL> urls) {
        return new Reflections(
                new ConfigurationBuilder()
                        .addUrls(urls)
                        .setScanners(new MethodAnnotationsScanner())
                        .addClassLoader(getClassLoader(urls)))
                .getMethodsAnnotatedWith(FunctionName.class);
    }

    protected ClassLoader getClassLoader(final List<URL> urlList) {
        final URL[] urlArray = urlList.toArray(new URL[urlList.size()]);
        return new URLClassLoader(urlArray, this.getClass().getClassLoader());
    }

    @Override
    public Map<String, FunctionConfiguration> generateConfigurations(final Set<Method> methods) throws AzureExecutionException {
        final Map<String, FunctionConfiguration> configMap = new HashMap<>();
        for (final Method method : methods) {
            final FunctionName functionAnnotation = method.getAnnotation(FunctionName.class);
            final String functionName = functionAnnotation.value();
            validateFunctionName(configMap.keySet(), functionName);
            Log.debug("Starting processing function : " + functionName);
            configMap.put(functionName, generateConfiguration(method));
        }
        return configMap;
    }

    protected void validateFunctionName(final Set<String> nameSet, final String functionName) throws AzureExecutionException {
        if (StringUtils.isEmpty(functionName)) {
            throw new AzureExecutionException("Azure Functions name cannot be empty.");
        }
        if (nameSet.stream().anyMatch(n -> StringUtils.equalsIgnoreCase(n, functionName))) {
            throw new AzureExecutionException("Found duplicate Azure Function: " + functionName);
        }
    }

    @Override
    public FunctionConfiguration generateConfiguration(final Method method) throws AzureExecutionException {
        final FunctionConfiguration config = new FunctionConfiguration();
        final List<Binding> bindings = config.getBindings();

        processParameterAnnotations(method, bindings);

        processMethodAnnotations(method, bindings);

        patchStorageBinding(method, bindings);

        config.setRetry(getRetryConfigurationFromMethod(method));
        config.setEntryPoint(method.getDeclaringClass().getCanonicalName() + "." + method.getName());
        return config;
    }

    private Retry getRetryConfigurationFromMethod(Method method) throws AzureExecutionException {
        final FixedDelayRetry fixedDelayRetry = method.getDeclaredAnnotation(FixedDelayRetry.class);
        final ExponentialBackoffRetry exponentialBackoffRetry = method.getDeclaredAnnotation(ExponentialBackoffRetry.class);
        if (fixedDelayRetry != null && exponentialBackoffRetry != null) {
            throw new AzureExecutionException(MULTI_RETRY_ANNOTATION);
        }
        if (fixedDelayRetry != null) {
            return Retry.createFixedDelayRetryFromAnnotation(fixedDelayRetry);
        }
        if (exponentialBackoffRetry != null) {
            return Retry.createExponentialBackoffRetryFromAnnotation(exponentialBackoffRetry);
        }
        return null;
    }

    protected void processParameterAnnotations(final Method method, final List<Binding> bindings) {
        for (final Parameter param : method.getParameters()) {
            bindings.addAll(parseAnnotations(param::getAnnotations, this::parseParameterAnnotation));
        }
    }

    protected void processMethodAnnotations(final Method method, final List<Binding> bindings) {
        if (!method.getReturnType().equals(Void.TYPE)) {
            bindings.addAll(parseAnnotations(method::getAnnotations, this::parseMethodAnnotation));

            if (bindings.stream().anyMatch(b -> b.getBindingEnum() == BindingEnum.HttpTrigger) &&
                    bindings.stream().noneMatch(b -> b.getName().equalsIgnoreCase("$return"))) {
                bindings.add(BindingFactory.getHTTPOutBinding());
            }
        }
    }

    protected List<Binding> parseAnnotations(Supplier<Annotation[]> annotationProvider,
                                             Function<Annotation, Binding> annotationParser) {
        final List<Binding> bindings = new ArrayList<>();

        for (final Annotation annotation : annotationProvider.get()) {
            final Binding binding = annotationParser.apply(annotation);
            if (binding != null) {
                Log.debug("Adding binding: " + binding.toString());
                bindings.add(binding);
            }
        }

        return bindings;
    }

    protected Binding parseParameterAnnotation(final Annotation annotation) {
        return BindingFactory.getBinding(annotation);
    }

    protected Binding parseMethodAnnotation(final Annotation annotation) {
        final Binding ret = parseParameterAnnotation(annotation);
        if (ret != null) {
            ret.setName("$return");
        }
        return ret;
    }

    protected void patchStorageBinding(final Method method, final List<Binding> bindings) {
        final Optional<Annotation> storageAccount = Arrays.stream(method.getAnnotations())
                .filter(annotation -> annotation instanceof StorageAccount)
                .findFirst();

        if (storageAccount.isPresent()) {
            Log.debug("StorageAccount annotation found.");
            final String connectionString = ((StorageAccount) storageAccount.get()).value();
            // Replace empty connection string
            bindings.stream().filter(binding -> binding.getBindingEnum().isStorage())
                    .filter(binding -> StringUtils.isEmpty((String) binding.getAttribute("connection")))
                    .forEach(binding -> binding.setAttribute("connection", connectionString));
        } else {
            Log.debug("No StorageAccount annotation found.");
        }
    }
}
