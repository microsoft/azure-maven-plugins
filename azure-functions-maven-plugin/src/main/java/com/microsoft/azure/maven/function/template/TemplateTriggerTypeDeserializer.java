/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.template;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;

public class TemplateTriggerTypeDeserializer extends StdDeserializer<String> {

    protected TemplateTriggerTypeDeserializer() {
        this(null);
    }

    protected TemplateTriggerTypeDeserializer(Class<?> vc) {
        super(vc);
    }

    @Override
    public String deserialize(JsonParser jsonParser,
                              DeserializationContext deserializationContext) {
        JsonNode node = null;
        try {
            node = jsonParser.getCodec().readTree(jsonParser);
            final JsonNode bindingsNode = node.get("bindings");
            for (int i = 0; i < bindingsNode.size(); i++) {
                if (bindingsNode.get(i).get("direction").asText().equals("in")) {
                    return bindingsNode.get(i).get("type").asText();
                }
            }
        } catch (IOException e) {
            // swallow it
        }
        return null;
    }
}
