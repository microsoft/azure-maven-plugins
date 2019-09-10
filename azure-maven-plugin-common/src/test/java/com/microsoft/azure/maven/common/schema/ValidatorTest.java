/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.common.schema;

import com.google.gson.annotations.SerializedName;

public class ValidatorTest {

    private static class SimpleTypeSchema {
        private String description;
        private String type;
        @SerializedName("default")
        private String defaultObject;
        private int minimum;
        private int maximum;
        private String pattern;
        @SerializedName("enum")
        private String[] enumObj;

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }

        public String getDefault() {
            return defaultObject;
        }

        public void setDefault(String defaultObject) {
            this.defaultObject = defaultObject;
        }

        public int getMinimum() {
            return minimum;
        }

        public void setMinimum(int minimum) {
            this.minimum = minimum;
        }

        public int getMaximum() {
            return maximum;
        }

        public void setMaximum(int maximum) {
            this.maximum = maximum;
        }

        public String getPattern() {
            return pattern;
        }

        public void setPattern(String pattern) {
            this.pattern = pattern;
        }

        public String[] getEnum() {
            return enumObj;
        }

        public void setEnum(String[] enumObj) {
            this.enumObj = enumObj;
        }
    }
}
