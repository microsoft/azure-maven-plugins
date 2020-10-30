/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.plugin.login;

import com.microsoft.aad.adal4j.AuthenticationResult;
import com.microsoft.applicationinsights.TelemetryClient;
import com.microsoft.azure.Page;
import com.microsoft.azure.PagedList;
import com.microsoft.azure.common.utils.JsonUtils;
import com.microsoft.azure.credentials.AzureTokenCredentials;
import com.microsoft.azure.management.Azure;
import com.microsoft.azure.management.Azure.Authenticated;
import com.microsoft.azure.management.resources.Subscription;
import com.microsoft.azure.management.resources.Subscriptions;
import com.microsoft.azure.maven.telemetry.AppInsightHelper;
import com.microsoft.rest.RestException;
import org.apache.commons.lang3.SystemUtils;
import org.mockito.ArgumentMatchers;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.reflect.Whitebox;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.ConcurrentModificationException;
import java.util.Map;
import java.util.stream.Stream;

import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

class TestHelper {
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

    public static void mockAppInsightHelper() throws Exception {
        // mock for AppInsightHelper.INSTANCE
        final TelemetryClient client = mock(TelemetryClient.class);
        PowerMockito.whenNew(TelemetryClient.class).withAnyArguments().thenReturn(client);
        final AppInsightHelper mockAppInsightHelper = mock(AppInsightHelper.class);
        Whitebox.setInternalState(AppInsightHelper.class, "INSTANCE", mockAppInsightHelper);

    }

    public static Subscriptions createTwoMockSubscriptions() {
        final Subscriptions subs = mock(Subscriptions.class);
        final PagedList<Subscription> list = new PagedList<Subscription>() {

            @Override
            public Page<Subscription> nextPage(String nextPageLink) throws RestException {
                return null;
            }
        };

        final Subscription mockSub = mock(Subscription.class);
        when(mockSub.subscriptionId()).thenReturn("new_subs_id");
        when(mockSub.displayName()).thenReturn("new_subs_name");
        final Subscription mockSub2 = mock(Subscription.class);
        when(mockSub2.subscriptionId()).thenReturn("new_subs_id2");
        when(mockSub2.displayName()).thenReturn("new_subs_name2");
        list.add(mockSub);
        list.add(mockSub2);
        when(subs.list()).thenReturn(list);
        return subs;
    }

    public static Subscriptions createOneMockSubscriptions() {
        final Subscriptions subs = mock(Subscriptions.class);
        final PagedList<Subscription> list = new PagedList<Subscription>() {

            @Override
            public Page<Subscription> nextPage(String nextPageLink) throws RestException {
                return null;
            }
        };

        final Subscription mockSub = mock(Subscription.class);
        when(mockSub.subscriptionId()).thenReturn("new_subs_id");
        when(mockSub.displayName()).thenReturn("new_subs_name");
        list.add(mockSub);
        when(subs.list()).thenReturn(list);
        return subs;
    }

    public static Subscriptions createEmptyMockSubscriptions() {
        final Subscriptions subs = mock(Subscriptions.class);
        final PagedList<Subscription> list = new PagedList<Subscription>() {

            @Override
            public Page<Subscription> nextPage(String nextPageLink) throws RestException {
                return null;
            }
        };

        when(subs.list()).thenReturn(list);
        return subs;
    }

    public static void mockAzureWithSubs(Subscriptions subs) {
        mockStatic(Azure.class);
        final Azure.Configurable configuration = mock(Azure.Configurable.class);
        final Authenticated azureClient = mock(Authenticated.class);
        when(configuration.authenticate(ArgumentMatchers.any(AzureTokenCredentials.class))).thenReturn(azureClient);
        when(Azure.configure()).thenReturn(configuration);
        when(azureClient.subscriptions()).thenReturn(subs);
    }


    /**
     * Code copied from https://stackoverflow.com/questions/318239/how-do-i-set-environment-variables-from-java/496849.
     *
     * Sets an environment variable FOR THE CURRENT RUN OF THE JVM Does not actually modify the system's environment
     * variables, but rather only the copy of the variables that java has taken, and hence should only be used for testing
     * purposes!
     *
     * @param key   The Name of the variable to set
     * @param value The value of the variable to set
     */
    @SuppressWarnings("unchecked")
    public static <K, V> void injectEnvironmentVariable(final String key, final String value) {
        try {
            /// we obtain the actual environment
            final Class<?> processEnvironmentClass = Class.forName("java.lang.ProcessEnvironment");
            final Field theEnvironmentField = processEnvironmentClass.getDeclaredField("theEnvironment");
            final boolean environmentAccessibility = theEnvironmentField.isAccessible();
            theEnvironmentField.setAccessible(true);

            final Map<K, V> env = (Map<K, V>) theEnvironmentField.get(null);

            if (SystemUtils.IS_OS_WINDOWS) {
                // This is all that is needed on windows running java jdk 1.8.0_92
                if (value == null) {
                    env.remove(key);
                } else {
                    env.put((K) key, (V) value);
                }
            } else {
                // This is triggered to work on openjdk 1.8.0_91
                // The ProcessEnvironment$Variable is the key of the map
                final Class<K> variableClass = (Class<K>) Class.forName("java.lang.ProcessEnvironment$Variable");
                final Method convertToVariable = variableClass.getMethod("valueOf", String.class);
                final boolean conversionVariableAccessibility = convertToVariable.isAccessible();
                convertToVariable.setAccessible(true);

                // The ProcessEnvironment$Value is the value fo the map
                final Class<V> valueClass = (Class<V>) Class.forName("java.lang.ProcessEnvironment$Value");
                final Method convertToValue = valueClass.getMethod("valueOf", String.class);
                final boolean conversionValueAccessibility = convertToValue.isAccessible();
                convertToValue.setAccessible(true);

                if (value == null) {
                    env.remove(convertToVariable.invoke(null, key));
                } else {
                    // we place the new value inside the map after conversion so as to
                    // avoid class cast exceptions when rerunning this code
                    env.put((K) convertToVariable.invoke(null, key), (V) convertToValue.invoke(null, value));

                    // reset accessibility to what they were
                    convertToValue.setAccessible(conversionValueAccessibility);
                    convertToVariable.setAccessible(conversionVariableAccessibility);
                }
            }
            // reset environment accessibility
            theEnvironmentField.setAccessible(environmentAccessibility);

            // we apply the same to the case insensitive environment
            final Field theCaseInsensitiveEnvironmentField = processEnvironmentClass.getDeclaredField("theCaseInsensitiveEnvironment");
            final boolean insensitiveAccessibility = theCaseInsensitiveEnvironmentField.isAccessible();
            theCaseInsensitiveEnvironmentField.setAccessible(true);
            // Not entirely sure if this needs to be casted to ProcessEnvironment$Variable and $Value as well
            final Map<String, String> cienv = (Map<String, String>) theCaseInsensitiveEnvironmentField.get(null);
            if (value == null) {
                // remove if null
                cienv.remove(key);
            } else {
                cienv.put(key, value);
            }
            theCaseInsensitiveEnvironmentField.setAccessible(insensitiveAccessibility);
        } catch (final ClassNotFoundException | NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            throw new IllegalStateException("Failed setting environment variable <" + key + "> to <" + value + ">", e);
        } catch (final NoSuchFieldException e) {
            // we could not find theEnvironment
            final Map<String, String> env = System.getenv();
            Stream.of(Collections.class.getDeclaredClasses())
                    // obtain the declared classes of type $UnmodifiableMap
                    .filter(c1 -> "java.util.Collections$UnmodifiableMap".equals(c1.getName())).map(c1 -> {
                        try {
                            return c1.getDeclaredField("m");
                        } catch (final NoSuchFieldException e1) {
                            throw new IllegalStateException("Failed setting environment variable <" +
                                    key +
                                    "> to <" +
                                    value +
                                    "> when locating in-class memory map of environment", e1);
                        }
                    }).forEach(field -> {
                        try {
                            final boolean fieldAccessibility = field.isAccessible();
                            field.setAccessible(true);
                            // we obtain the environment
                            final Map<String, String> map = (Map<String, String>) field.get(env);
                            if (value == null) {
                                // remove if null
                                map.remove(key);
                            } else {
                                map.put(key, value);
                            }
                            // reset accessibility
                            field.setAccessible(fieldAccessibility);
                        } catch (final ConcurrentModificationException e1) {
                            // This may happen if we keep backups of the environment before calling this method
                            // as the map that we kept as a backup may be picked up inside this block.
                            // So we simply skip this attempt and continue adjusting the other maps
                            // To avoid this one should always keep individual keys/value backups not the entire map
                            System.out.println("Attempted to modify source map: " + field.getDeclaringClass() + "#" + field.getName() + " at the same time.");
                        } catch (final IllegalAccessException e1) {
                            throw new IllegalStateException(
                                    "Failed setting environment variable <" + key + "> to <" + value + ">. Unable to access field!", e1);
                        }
                    });
        }
    }
}
