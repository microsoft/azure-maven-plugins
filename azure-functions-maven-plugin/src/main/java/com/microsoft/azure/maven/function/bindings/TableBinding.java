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
    public static final String TABLE = "table";

    private String tableName = "";

    private String partitionKey = "";

    private String rowKey = "";

    private String filter = "";

    private String take = "";

    public TableBinding(final TableInput tableInput) {
        super(tableInput.name(), TABLE, Direction.IN, tableInput.dataType());

        tableName = tableInput.tableName();
        partitionKey = tableInput.partitionKey();
        rowKey = tableInput.rowKey();
        filter = tableInput.filter();
        take = tableInput.take();
        setConnection(tableInput.connection());
    }

    public TableBinding(final TableOutput tableOutput) {
        super(tableOutput.name(), TABLE, Direction.OUT, tableOutput.dataType());

        tableName = tableOutput.tableName();
        partitionKey = tableOutput.partitionKey();
        rowKey = tableOutput.rowKey();
        setConnection(tableOutput.connection());
    }

    @JsonGetter
    public String getTableName() {
        return tableName;
    }

    @JsonGetter
    public String getPartitionKey() {
        return partitionKey;
    }

    @JsonGetter
    public String getRowKey() {
        return rowKey;
    }

    @JsonGetter
    public String getFilter() {
        return filter;
    }

    @JsonGetter
    public String getTake() {
        return take;
    }
}
