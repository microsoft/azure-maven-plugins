/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.utils;

import com.azure.core.exception.AzureException;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.reflect.FieldUtils;

import javax.annotation.Nonnull;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.List;

public class SystemPropertyUtils {
    public static Object injectCommandLineParameter(String prefix, Object obj, @Nonnull Class cls) {
        Object result = obj;
        try {
            if (result == null) {
                try {
                    result = cls.getDeclaredConstructor().newInstance();
                } catch (InstantiationException | InvocationTargetException | NoSuchMethodException e) {
                    throw new AzureException(String.format("Class %s should have a default constructor for inject properties", cls.getName()));
                }
            }
            final List<Field> fields = FieldUtils.getAllFieldsList(cls);
            for (final Field field : fields) {
                if (!Modifier.isStatic(field.getModifiers()) && field.getType().equals(String.class)) {
                    final String propertyValue = System.getProperty(String.format("%s.%s", prefix, field.getName()));
                    if (StringUtils.isNotBlank(propertyValue)) {
                        final String objValue = (String) FieldUtils.readField(result, field.getName(), true);
                        if (StringUtils.isBlank(objValue)) {
                            FieldUtils.writeField(result, field.getName(), propertyValue, true);;
                        }
                    }
                }
            }
        } catch (IllegalAccessException ex) {
            throw new AzureException(ex.getMessage());
        }
        return result;
    }
}
