/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.appservice.function.core;

import com.microsoft.applicationinsights.core.dependencies.apachecommons.lang3.ClassUtils;
import com.microsoft.azure.functions.annotation.*;
import com.microsoft.azure.toolkit.lib.common.exception.AzureToolkitRuntimeException;
import com.microsoft.azure.toolkit.lib.common.utils.JsonUtils;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.Binding;
import com.microsoft.azure.toolkit.lib.legacy.function.bindings.BindingEnum;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.FunctionConfiguration;
import com.microsoft.azure.toolkit.lib.legacy.function.configurations.Retry;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.Function;
@Slf4j
abstract class AbstractFunctionStagingInitializer {
    private static final List<String> CUSTOM_BINDING_RESERVED_PROPERTIES = Arrays.asList("type", "name", "direction");
    private static final String MULTI_RETRY_ANNOTATION = "Fixed delay retry and exponential backoff retry are not compatible, " +
            "please use either of them for one trigger";

    private static final String HTTP_OUTPUT_DEFAULT_NAME = "$return";
    private static final Map<BindingEnum, List<String>> REQUIRED_ATTRIBUTE_MAP = new HashMap<>();

    static {
        //initialize required attributes, which will be saved to function.json even if it equals to its default value
        REQUIRED_ATTRIBUTE_MAP.put(BindingEnum.EventHubTrigger, Collections.singletonList("cardinality"));
        REQUIRED_ATTRIBUTE_MAP.put(BindingEnum.HttpTrigger, Collections.singletonList("authLevel"));
    }

    public FunctionConfiguration generateConfiguration(FunctionProject project, FunctionMethod method) {
        final FunctionConfiguration config = new FunctionConfiguration();
        final List<Binding> bindings = config.getBindings();
        processParameterAnnotations(method, bindings);
        processMethodAnnotations(method, bindings);
        patchStorageBinding(method, bindings);

        config.setRetry(getRetryConfigurationFromMethod(method));
        config.setEntryPoint(method.toString());
        config.setScriptFile("../" + project.getArtifactFile().getName());
        return config;
    }

    protected Map<String, FunctionConfiguration> generateConfigurationsInner(FunctionProject project, List<FunctionMethod> methods) {
        final Map<String, FunctionConfiguration> configMap = new HashMap<>();
        for (final FunctionMethod method : methods) {
            final FunctionAnnotation functionAnnotation = method.getAnnotation(FunctionName.class);
            if (functionAnnotation == null) {
                continue;
            }
            final String functionName = functionAnnotation.getStringValue("value", false);
            validateFunctionName(configMap.keySet(), functionName);
            log.debug("Starting processing function : " + functionName);
            configMap.put(functionName, generateConfiguration(project, method));
        }
        return configMap;
    }

    private void patchStorageBinding(final FunctionMethod method, final List<Binding> bindings) {
        final Optional<FunctionAnnotation> storageAccount = method.getAnnotations().stream()
                .filter(annotation -> annotation.isAnnotationType(StorageAccount.class))
                .findFirst();

        if (storageAccount.isPresent()) {
            log.debug("StorageAccount annotation found.");
            final String connectionString = storageAccount.get().getStringValue("value", true);
            // Replace empty connection string
            bindings.stream().filter(binding -> binding.getBindingEnum().isStorage())
                    .filter(binding -> StringUtils.isEmpty((String) binding.getAttribute("connection")))
                    .forEach(binding -> binding.setAttribute("connection", connectionString));
        } else {
            log.debug("No StorageAccount annotation found.");
        }
    }

    private void processMethodAnnotations(final FunctionMethod method, final List<Binding> bindings) {
        if (!StringUtils.equalsIgnoreCase(method.getReturnTypeName(), "void")) {
            bindings.addAll(parseAnnotations(method.getAnnotations(), this::parseMethodAnnotation));

            if (bindings.stream().anyMatch(b -> b.getBindingEnum() == BindingEnum.HttpTrigger) &&
                    bindings.stream().noneMatch(b -> b.getName().equalsIgnoreCase("$return"))) {
                bindings.add(getHTTPOutBinding());
            }
        }
    }

    private Binding parseMethodAnnotation(final FunctionAnnotation annotation) {
        final Binding ret = parseParameterAnnotation(annotation);
        if (ret != null) {
            ret.setName("$return");
        }
        return ret;
    }

    private void processParameterAnnotations(final FunctionMethod method, final List<Binding> bindings) {
        for (final FunctionAnnotation[] paramAnnotations : method.getParameterAnnotations()) {
            bindings.addAll(parseAnnotations(Arrays.asList(paramAnnotations), this::parseParameterAnnotation));
        }
    }

    private List<Binding> parseAnnotations(List<FunctionAnnotation> annotationBindings,
                                             Function<FunctionAnnotation, Binding> annotationParser) {
        final List<Binding> bindings = new ArrayList<>();

        for (final FunctionAnnotation annotation : annotationBindings) {
            final Binding binding = annotationParser.apply(annotation);
            if (binding != null) {
                log.debug("Adding binding: " + binding);
                bindings.add(binding);
            }
        }

        return bindings;
    }

    private Binding createBinding(BindingEnum bindingEnum, FunctionAnnotation annotationBinding) {
        final Binding binding = new Binding(bindingEnum);
        annotationBinding.getPropertiesWithRequiredProperties(REQUIRED_ATTRIBUTE_MAP.get(bindingEnum))
                .forEach(binding::setAttribute);
        return binding;
    }

    private Binding parseParameterAnnotation(final FunctionAnnotation annotation) {
        return getBinding(annotation);
    }

    private void validateFunctionName(final Set<String> nameSet, final String functionName) {
        if (StringUtils.isEmpty(functionName)) {
            throw new AzureToolkitRuntimeException("Azure Functions name cannot be empty.");
        }
        if (nameSet.stream().anyMatch(n -> StringUtils.equalsIgnoreCase(n, functionName))) {
            throw new AzureToolkitRuntimeException("Found duplicate Azure Function: " + functionName);
        }
    }

    private Binding getBinding(final FunctionAnnotation annotation) {
        String fqn = annotation.getAnnotationTypeName();
        final BindingEnum annotationEnum =
                Arrays.stream(BindingEnum.values())
                        .filter(bindingEnum -> StringUtils.equalsIgnoreCase(bindingEnum.name(),
                                ClassUtils.getShortClassName(fqn)))
                        .findFirst()
                        .orElse(null);
        FunctionAnnotation customBindingAnnotation = annotation.getAnnotationType().getAnnotation(CustomBinding.class);
        if (customBindingAnnotation != null) {
            Map<String, Object> annotationProperties = customBindingAnnotation.getPropertiesWithRequiredProperties(CUSTOM_BINDING_RESERVED_PROPERTIES);
            Map<String, Object> customBindingProperties = annotation.getPropertiesWithRequiredProperties(CUSTOM_BINDING_RESERVED_PROPERTIES);
            customBindingProperties.putAll(annotationProperties);
            Map<String, Object> userDefined = annotation.getDeclaredAnnotationProperties();
            return createCustomBinding(customBindingProperties, userDefined);
        } else if (annotation.isAnnotationType(CustomBinding.class)) {
            Map<String, Object> customBindingProperties = annotation.getPropertiesWithRequiredProperties(CUSTOM_BINDING_RESERVED_PROPERTIES);
            return createCustomBinding(customBindingProperties, null);
        } else if (annotationEnum != null) {
            return createBinding(annotationEnum, annotation);
        }
        return null;
    }

    private Binding createCustomBinding(Map<String, Object> map1, Map<String, Object> map2) {
        final Map<String, Object> mergedMap = new HashMap<>(map1);
        if (map2 != null) {
            mergedMap.putAll(map2);
        }
        final Binding extendBinding = new ExtendedCustomBinding((String) mergedMap.get("name"),
                (String) mergedMap.get("direction"), (String) mergedMap.get("type")
        );

        mergedMap.forEach((name, value) -> {
            if (!CUSTOM_BINDING_RESERVED_PROPERTIES.contains(name)) {
                extendBinding.setAttribute(name, value);
            }
        });
        return extendBinding;
    }

    private Binding getHTTPOutBinding() {
        final Binding result = new Binding(BindingEnum.HttpOutput);
        result.setName(HTTP_OUTPUT_DEFAULT_NAME);
        return result;
    }

    private Retry getRetryConfigurationFromMethod(FunctionMethod method) {
        final FunctionAnnotation fixedDelayRetry = method.getAnnotation(FixedDelayRetry.class);
        final FunctionAnnotation exponentialBackoffRetry = method.getAnnotation(ExponentialBackoffRetry.class);
        if (fixedDelayRetry != null && exponentialBackoffRetry != null) {
            throw new AzureToolkitRuntimeException(MULTI_RETRY_ANNOTATION);
        }
        if (fixedDelayRetry != null) {
            return createRetryFromMap(fixedDelayRetry.getAllAnnotationProperties());
        }
        if (exponentialBackoffRetry != null) {
            return createRetryFromMap(exponentialBackoffRetry.getAllAnnotationProperties());
        }
        return null;
    }

    private Retry createRetryFromMap(Map<String, Object> map) {
        return JsonUtils.fromJson(JsonUtils.toJson(map), Retry.class);
    }
}
