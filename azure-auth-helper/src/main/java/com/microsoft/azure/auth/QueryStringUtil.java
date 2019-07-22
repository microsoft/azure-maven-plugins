/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.auth;

import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;

import java.io.UnsupportedEncodingException;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class QueryStringUtil {
    public static Map<String, String> queryToMap(String query) throws UnsupportedEncodingException {
        final Map<String, String> queryMap = new LinkedHashMap<>();
        final List<NameValuePair> params = URLEncodedUtils.parse(query, Constants.UTF8);
        for (final NameValuePair param : params) {
            queryMap.put(param.getName(), param.getValue());
        }

        return queryMap;
    }

    private QueryStringUtil() {

    }
}
