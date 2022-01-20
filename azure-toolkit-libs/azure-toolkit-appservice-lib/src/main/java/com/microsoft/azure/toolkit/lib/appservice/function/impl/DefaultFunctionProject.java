/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */
package com.microsoft.azure.toolkit.lib.appservice.function.impl;

import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionAnnotation;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionAnnotationClass;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionMethod;
import com.microsoft.azure.toolkit.lib.appservice.function.core.FunctionProject;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.CommandHandler;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.CommandHandlerImpl;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.FunctionCoreToolsHandler;
import com.microsoft.azure.toolkit.lib.legacy.function.handlers.FunctionCoreToolsHandlerImpl;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import org.reflections.Reflections;
import org.reflections.scanners.Scanners;
import org.reflections.util.ConfigurationBuilder;

import javax.annotation.Nonnull;
import java.lang.annotation.Annotation;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Log4j2
public class DefaultFunctionProject extends FunctionProject {

    @Override
    public List<FunctionMethod> findAnnotatedMethods() {
        Set<Method> methods;
        try {
            try {
                log.debug("ClassPath to resolve: " + getTargetClassUrl());
                final List<URL> dependencyWithTargetClass = getDependencyArtifactUrls();
                dependencyWithTargetClass.add(getTargetClassUrl());
                methods = findFunctions(dependencyWithTargetClass);
            } catch (NoClassDefFoundError e) {
                // fallback to reflect through artifact url, for shaded project(fat jar)
                log.debug("ClassPath to resolve: " + getArtifactUrl());
                methods = findFunctions(Collections.singletonList(getArtifactUrl()));
            }
            return methods.stream().map(DefaultFunctionProject::create).collect(Collectors.toList());
        } catch (MalformedURLException e) {
            throw new AzureToolkitRuntimeException("Invalid URL when resolving functions in class path:" + e.getMessage(), e);
        }
    }

    @SneakyThrows
    @Override
    public void installExtension(String funcPath) {
        final CommandHandler commandHandler = new CommandHandlerImpl();
        final FunctionCoreToolsHandler functionCoreToolsHandler = getFunctionCoreToolsHandler(commandHandler);
        functionCoreToolsHandler.installExtension(getStagingFolder(),
            getBaseDirectory());
    }

    private static FunctionCoreToolsHandler getFunctionCoreToolsHandler(final CommandHandler commandHandler) {
        return new FunctionCoreToolsHandlerImpl(commandHandler);
    }

    private URL getTargetClassUrl() throws MalformedURLException {
        return getClassesOutputDirectory().toURI().toURL();
    }

    /**
     * @return URLs for the classpath with compile scope needed jars
     */
    private List<URL> getDependencyArtifactUrls() {
        final List<URL> urlList = new ArrayList<>();
        getDependencies().forEach(file -> {
            try {
                urlList.add(file.toURI().toURL());
            } catch (MalformedURLException e) {
                log.debug("Failed to get URL for file: " + file);
            }
        });
        return urlList;
    }

    private static Set<Method> findFunctions(final List<URL> urls) {
        return new Reflections(
            new ConfigurationBuilder()
                .addUrls(urls)
                .setScanners(Scanners.MethodsAnnotated)
                .addClassLoaders(getClassLoader(urls)))
            .getMethodsAnnotatedWith(FunctionName.class);
    }

    private static ClassLoader getClassLoader(final List<URL> urlList) {
        final URL[] urlArray = urlList.toArray(new URL[0]);
        return new URLClassLoader(urlArray, DefaultFunctionProject.class.getClassLoader());
    }

    private URL getArtifactUrl() throws MalformedURLException {
        return getArtifactFile().toURI().toURL();
    }

    public static FunctionAnnotation create(@Nonnull Annotation annotation) {
        return create(annotation, true);
    }

    public static FunctionMethod create(Method method) {
        FunctionMethod functionMethod = new FunctionMethod();
        functionMethod.setName(method.getName());
        functionMethod.setReturnTypeName(method.getReturnType().getCanonicalName());
        functionMethod.setAnnotations(method.getAnnotations() == null ? Collections.emptyList() :
            Arrays.stream(method.getAnnotations()).map(DefaultFunctionProject::create).collect(Collectors.toList()));

        List<FunctionAnnotation[]> parameterAnnotations = Arrays.stream(method.getParameters())
            .map(Parameter::getAnnotations).filter(Objects::nonNull)
            .map(a -> Arrays.stream(a)
                .map(DefaultFunctionProject::create)
                .collect(Collectors.toList()).toArray(new FunctionAnnotation[0])).collect(Collectors.toList());

        functionMethod.setParameterAnnotations(parameterAnnotations);
        functionMethod.setDeclaringTypeName(method.getDeclaringClass().getCanonicalName());
        return functionMethod;
    }

    private static FunctionAnnotation create(@Nonnull Annotation annotation, boolean resolveAnnotationType) {
        Map<String, Object> map = new HashMap<>();
        Map<String, Object> defaultMap = new HashMap<>();
        for (Method method : annotation.annotationType().getDeclaredMethods()) {
            try {
                Object value = method.invoke(annotation);
                if (Objects.equals(value, method.getDefaultValue())) {
                    defaultMap.put(method.getName(), value);
                } else {
                    map.put(method.getName(), value);
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                throw new AzureToolkitRuntimeException(String.format("Cannot invoke method '%s' for annotation class '%s'",
                    method.getName(),
                    annotation.getClass().getSimpleName()
                ), e);
            }
        }

        FunctionAnnotation functionAnnotation = new FunctionAnnotation() {
            public boolean isAnnotationType(@Nonnull Class<? extends Annotation> clz) {
                return clz.isInstance(annotation);
            }
        };
        if (resolveAnnotationType) {
            functionAnnotation.setAnnotationClass(toFunctionAnnotationClass(annotation.annotationType()));
        }
        functionAnnotation.setProperties(map);
        functionAnnotation.setDefaultProperties(defaultMap);
        return functionAnnotation;
    }

    private static FunctionAnnotationClass toFunctionAnnotationClass(Class<? extends Annotation> clz) {
        FunctionAnnotationClass type = new FunctionAnnotationClass();
        type.setFullName(clz.getCanonicalName());
        type.setName(clz.getSimpleName());
        type.setAnnotations(Arrays.stream(clz.getAnnotations()).map(a -> create(a, false)).collect(Collectors.toList()));
        return type;
    }
}
