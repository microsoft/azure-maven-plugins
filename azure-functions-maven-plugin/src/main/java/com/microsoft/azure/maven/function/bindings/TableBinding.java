/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.azure.serverless.functions.annotation.TableInput;
import com.microsoft.azure.serverless.functions.annotation.TableOutput;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class TableBinding extends StorageBaseBinding {
    private String tableName = "";

    private String partitionKey = "";

    private String rowKey = "";

    private String filter = "";

    private String take = "";

    public TableBinding(final TableInput tableInput) {
        setDirection("in");
        setType("table");
        setName(tableInput.name());

        tableName = tableInput.tableName();
        partitionKey = tableInput.partitionKey();
        rowKey = tableInput.rowKey();
        filter = tableInput.filter();
        take = tableInput.take();
        setConnection(tableInput.connection());
    }

    public TableBinding(final TableOutput tableOutput) {
        setDirection("out");
        setType("table");
        setName(tableOutput.name());

        tableName = tableOutput.tableName();
        partitionKey = tableOutput.partitionKey();
        rowKey = tableOutput.rowKey();
        setConnection(tableOutput.connection());
    }

    @JsonGetter("tableName")
    public String getTableName() {
        return tableName;
    }

    @JsonGetter("partitionKey")
    public String getPartitionKey() {
        return partitionKey;
    }

    @JsonGetter("rowKey")
    public String getRowKey() {
        return rowKey;
    }

    @JsonGetter("filter")
    public String getFilter() {
        return filter;
    }

    @JsonGetter("take")
    public String getTake() {
        return take;
    }
}
