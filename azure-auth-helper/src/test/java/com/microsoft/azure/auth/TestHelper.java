/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.azure.maven.utils.JsonUtils;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class TestHelper {
    private static final String SAMPLE_AUTHENTICATION_JSON = "{\n" +
            "    \"accessTokenType\": \"Bearer\",\n" +
            "    \"idToken\": \"eyJ0eXAi...iOiIxLjAifQ.\",\n" +
            "    \"userInfo\": {\n" +
            "        \"uniqueId\": \"daaaa...3f2\",\n" +
            "        \"displayableId\": \"george@microsoft.com\",\n" +
            "        \"givenName\": \"George\",\n" +
            "        \"familyName\": \"Smith\",\n" +
            "        \"tenantId\": \"72f988bf-86f1-41af-91ab-2d7cd011db47\"\n" +
            "    },\n" +
            "    \"accessToken\": \"eyJ0eXA...jmcnxMnQ\",\n" +
            "    \"refreshToken\": \"AQAB...n5cgAA\",\n" +
            "    \"isMultipleResourceRefreshToken\": true\n" +
            "}";

    public static AuthenticationResult createAuthenticationResult() {
        return JsonUtils.fromJson(SAMPLE_AUTHENTICATION_JSON, AuthenticationResult.class);
    }

    public static Map<String, Object> getAuthenticationMap() {
        return JsonUtils.fromJson(SAMPLE_AUTHENTICATION_JSON, Map.class);
    }

    public static <T> void setField(Class<T> clazz, T instance, String fieldName, Object injectedField)
            throws NoSuchFieldException, IllegalAccessException {
        final Field field = clazz.getDeclaredField(fieldName);
        field.setAccessible(true);
        field.set(instance, injectedField);
    }

    public static Object readField(Object obj, String name)
            throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
        if (obj instanceof Class) {
            final Field fileld = ((Class) obj).getDeclaredField(name);
            fileld.setAccessible(true);
            return fileld.get(null);
        }
        final Field fileld = obj.getClass().getDeclaredField(name);
        fileld.setAccessible(true);
        return fileld.get(obj);
    }

    public static void injectEnvironmentVariable(String name, String val) throws ReflectiveOperationException {
        // dangerous: please use this code only in unit test.
        final Map<String, String> env = new HashMap<>(System.getenv());
        env.put(name, val);
        setEnv(env);
    }

    protected static void setEnv(Map<String, String> newenv) throws ReflectiveOperationException {
        try {
            final Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            final Map<String, String> env = (Map<String, String>) readField(processEnvironmentClass, "theEnvironment");
            env.putAll(newenv);
            final Map<String, String> cienv = (Map<String, String>) readField(processEnvironmentClass, "theCaseInsensitiveEnvironment");
            cienv.putAll(newenv);
        } catch (NoSuchFieldException e) {
            final Class[] classes = Collections.class.getDeclaredClasses();
            final Map<String, String> env = System.getenv();
            for (final Class cl : classes) {
                if ("java.util.Collections$UnmodifiableMap".equals(cl.getName())) {
                    final Field field = cl.getDeclaredField("m");
                    field.setAccessible(true);
                    final Object obj = field.get(env);
                    final Map<String, String> map = (Map<String, String>) obj;
                    map.clear();
                    map.putAll(newenv);
                }
            }
        }
    }
}
