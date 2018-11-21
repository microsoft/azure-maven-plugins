/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.Map;

public class BindingSerializer extends StdSerializer<BaseBinding> {

    public BindingSerializer() {
        this(null);
    }

    public BindingSerializer(Class<BaseBinding> item) {
        super(item);
    }

    @Override
    public void serialize(BaseBinding value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        gen.writeStartObject();
        gen.writeStringField("type", value.getType());
        gen.writeStringField("direction", value.getDirection());
        final Map<String, Object> attributes = value.getBindingAttributes();
        for (final Map.Entry<String, Object> entry : attributes.entrySet()) {
            gen.writeObjectField(entry.getKey(), entry.getValue());
        }
        gen.writeEndObject();
    }
}
