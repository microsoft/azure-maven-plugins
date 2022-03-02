/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.legacy.function.handlers;

import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionAnnotation;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionMethod;
import com.microsoft.azure.toolkit.lib.appservice.function.impl.DefaultFunctionProject;
import com.microsoft.azure.toolkit.lib.common.exception.AzureExecutionException;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.Binding;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.BindingEnum;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.BindingFactory;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionConfiguration;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.Retry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.ClassUtils;
import org.apache.commons.lang3.StringUtils;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.microsoft.azure.toolkit.lib.appservice.function.core.AzureFunctionsAnnotationConstants.EXPONENTIAL_BACKOFF_RETRY;
import static com.microsoft.azure.toolkit.lib.appservice.function.core.AzureFunctionsAnnotationConstants.FIXED_DELAY_RETRY;
import static com.microsoft.azure.toolkit.lib.appservice.function.core.AzureFunctionsAnnotationConstants.FUNCTION_NAME;
import static com.microsoft.azure.toolkit.lib.appservice.function.core.AzureFunctionsAnnotationConstants.STORAGE_ACCOUNT;

@Slf4j
@Deprecated
public class AnnotationHandlerImpl implements AnnotationHandler {

    private static final String MULTI_RETRY_ANNOTATION = "Fixed delay retry and exponential backoff retry are not compatible, " +
        "please use either of them for one trigger";

    @Override
    public Set<Method> findFunctions(final List<URL> urls) {
        try {
            final Class<?> functionNameAnnotation = ClassUtils.getClass(FUNCTION_NAME);
            return new Reflections(
                    new ConfigurationBuilder()
                            .addUrls(urls)
                            .setScanners(Scanners.MethodsAnnotated)
                            .addClassLoaders(getClassLoader(urls)))
                    .getMethodsAnnotatedWith((Class<? extends Annotation>) functionNameAnnotation);
        } catch (ClassNotFoundException e) {
            throw new AzureToolkitRuntimeException(e);
        }
    }

    protected ClassLoader getClassLoader(final List<URL> urlList) {
        final URL[] urlArray = urlList.toArray(new URL[0]);
        return new URLClassLoader(urlArray, this.getClass().getClassLoader());
    }

    @Override
    public Map<String, FunctionConfiguration> generateConfigurations(final Set<Method> methods) throws AzureExecutionException {
        final Map<String, FunctionConfiguration> configMap = new HashMap<>();
        for (final Method method : methods) {
            final FunctionMethod functionMethod = DefaultFunctionProject.create(method);
            final FunctionAnnotation functionNameAnnotation = functionMethod.getAnnotation(FUNCTION_NAME);
            if (functionNameAnnotation == null) {
                continue;
            }
            final String functionName = functionNameAnnotation.getStringValue("value", true);
            validateFunctionName(configMap.keySet(), functionName);
            log.debug("Starting processing function : " + functionName);
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
        final FunctionMethod functionMethod = DefaultFunctionProject.create(method);
        final FunctionAnnotation fixedDelayRetry = functionMethod.getAnnotation(FIXED_DELAY_RETRY);
        final FunctionAnnotation exponentialBackoffRetry = functionMethod.getAnnotation(EXPONENTIAL_BACKOFF_RETRY);
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
                log.debug("Adding binding: " + binding);
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
        final FunctionMethod functionMethod = DefaultFunctionProject.create(method);
        final FunctionAnnotation storageAccount = functionMethod.getAnnotation(STORAGE_ACCOUNT);

        if (storageAccount != null) {
            log.debug("StorageAccount annotation found.");
            final String connectionString = storageAccount.getStringValue("value", true);
            // Replace empty connection string
            bindings.stream().filter(binding -> binding.getBindingEnum().isStorage())
                .filter(binding -> StringUtils.isEmpty((String) binding.getAttribute("connection")))
                .forEach(binding -> binding.setAttribute("connection", connectionString));
        } else {
            log.debug("No StorageAccount annotation found.");
        }
    }
}
