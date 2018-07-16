/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.handlers;

import com.microsoft.azure.maven.function.bindings.BaseBinding;
import com.microsoft.azure.maven.function.bindings.BindingFactory;
import com.microsoft.azure.maven.function.bindings.HttpBinding;
import com.microsoft.azure.maven.function.bindings.StorageBaseBinding;
import com.microsoft.azure.maven.function.configurations.FunctionConfiguration;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;
import com.microsoft.azure.functions.annotation.StorageAccount;
import org.apache.maven.plugin.logging.Log;
import org.codehaus.plexus.util.StringUtils;
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
    protected Log log;

    public AnnotationHandlerImpl(final Log log) {
        this.log = log;
    }

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
    public Map<String, FunctionConfiguration> generateConfigurations(final Set<Method> methods) throws Exception {
        final Map<String, FunctionConfiguration> configMap = new HashMap<>();
        for (final Method method : methods) {
            final FunctionName functionAnnotation = method.getAnnotation(FunctionName.class);
            final String functionName = functionAnnotation.value();
            validateFunctionName(configMap.keySet(), functionName);
            log.debug("Starting processing function : " + functionName);
            configMap.put(functionName, generateConfiguration(method));
        }
        return configMap;
    }

    protected void validateFunctionName(final Set<String> nameSet, final String functionName) throws Exception {
        if (StringUtils.isEmpty(functionName)) {
            throw new Exception("Azure Functions name cannot be empty.");
        }
        if (nameSet.stream().anyMatch(n -> StringUtils.equalsIgnoreCase(n, functionName))) {
            throw new Exception("Found duplicate Azure Function: " + functionName);
        }
    }

    @Override
    public FunctionConfiguration generateConfiguration(final Method method) {
        final FunctionConfiguration config = new FunctionConfiguration();
        final List<BaseBinding> bindings = config.getBindings();

        processParameterAnnotations(method, bindings);

        processMethodAnnotations(method, bindings);

        patchStorageBinding(method, bindings);

        config.setEntryPoint(method.getDeclaringClass().getCanonicalName() + "." + method.getName());
        return config;
    }

    protected void processParameterAnnotations(final Method method, final List<BaseBinding> bindings) {
        for (final Parameter param : method.getParameters()) {
            bindings.addAll(parseAnnotations(param::getAnnotations, this::parseParameterAnnotation));
        }
    }

    protected void processMethodAnnotations(final Method method, final List<BaseBinding> bindings) {
        if (!method.getReturnType().equals(Void.TYPE)) {
            bindings.addAll(parseAnnotations(method::getAnnotations, this::parseMethodAnnotation));

            if (bindings.stream().anyMatch(b -> b.getType().equalsIgnoreCase(HttpTrigger.class.getSimpleName())) &&
                    bindings.stream().noneMatch(b -> b.getName().equalsIgnoreCase("$return"))) {
                bindings.add(new HttpBinding());
            }
        }
    }

    protected List<BaseBinding> parseAnnotations(Supplier<Annotation[]> annotationProvider,
                                                 Function<Annotation, BaseBinding> annotationParser) {
        final List<BaseBinding> bindings = new ArrayList<>();

        for (final Annotation annotation : annotationProvider.get()) {
            final BaseBinding binding = annotationParser.apply(annotation);
            if (binding != null) {
                log.debug("Adding binding: " + binding.toString());
                bindings.add(binding);
            }
        }

        return bindings;
    }

    protected BaseBinding parseParameterAnnotation(final Annotation annotation) {
        return BindingFactory.getBinding(annotation);
    }

    protected BaseBinding parseMethodAnnotation(final Annotation annotation) {
        final BaseBinding ret = parseParameterAnnotation(annotation);
        if (ret != null) {
            ret.setName("$return");
        }
        return ret;
    }

    protected void patchStorageBinding(final Method method, final List<BaseBinding> bindings) {
        final Optional<Annotation> storageAccount = Arrays.stream(method.getAnnotations())
                .filter(a -> a instanceof StorageAccount)
                .findFirst();

        if (storageAccount.isPresent()) {
            log.debug("StorageAccount annotation found.");
            final String connectionString = ((StorageAccount) storageAccount.get()).value();
            bindings.stream().forEach(b -> {
                if (b instanceof StorageBaseBinding) {
                    final StorageBaseBinding sb = (StorageBaseBinding) b;
                    // Override storage bindings with empty connection
                    if (StringUtils.isEmpty(sb.getConnection())) {
                        sb.setConnection(connectionString);
                    }
                }
            });
        } else {
            log.debug("No StorageAccount annotation found.");
        }
    }
}
