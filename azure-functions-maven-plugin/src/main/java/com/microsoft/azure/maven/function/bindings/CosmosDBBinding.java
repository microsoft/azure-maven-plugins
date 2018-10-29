/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.azure.functions.annotation.CosmosDBInput;
import com.microsoft.azure.functions.annotation.CosmosDBOutput;
import com.microsoft.azure.functions.annotation.CosmosDBTrigger;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class CosmosDBBinding extends BaseBinding {
    public static final String COSMOS_DB_TRIGGER = "cosmosDBTrigger";
    public static final String COSMOS_DB = "cosmosDB";

    private int collectionThroughput = -1;

    private String databaseName = "";

    private String collectionName = "";

    private String leaseCollectionName = "";

    private String id = "";

    private String sqlQuery = "";

    private String connectionStringSetting = "";

    private String partitionKey = "";

    private String createIfNotExists = "false";

    private String createLeaseCollectionIfNotExists = "true";

    public CosmosDBBinding(final CosmosDBInput dbInput) {
        super(dbInput.name(), COSMOS_DB, Direction.IN, dbInput.dataType());

        databaseName = dbInput.databaseName();
        collectionName = dbInput.collectionName();
        connectionStringSetting = dbInput.connectionStringSetting();
        id = dbInput.id();
        partitionKey = dbInput.partitionKey();
        sqlQuery = dbInput.sqlQuery();
    }

    public CosmosDBBinding(final CosmosDBOutput dbOutput) {
        super(dbOutput.name(), COSMOS_DB, Direction.OUT, dbOutput.dataType());

        databaseName = dbOutput.databaseName();
        collectionName = dbOutput.collectionName();
        createIfNotExists = dbOutput.createIfNotExists() ? "true" : "false";
        connectionStringSetting = dbOutput.connectionStringSetting();
        partitionKey = dbOutput.partitionKey();
        collectionThroughput = dbOutput.collectionThroughput();
    }

    public CosmosDBBinding(final CosmosDBTrigger dbTrigger) {
        super(dbTrigger.name(), COSMOS_DB_TRIGGER, Direction.IN, dbTrigger.dataType());

        connectionStringSetting = dbTrigger.connectionStringSetting();
        databaseName = dbTrigger.databaseName();
        collectionName = dbTrigger.collectionName();
        leaseCollectionName = dbTrigger.leaseCollectionName();
        createLeaseCollectionIfNotExists = dbTrigger.createLeaseCollectionIfNotExists() ? "true" : "false";
    }

    @JsonGetter
    public String getDatabaseName() {
        return databaseName;
    }

    @JsonGetter
    public String getCollectionName() {
        return collectionName;
    }

    @JsonGetter
    public String getId() {
        return id;
    }

    @JsonGetter
    public String getSqlQuery() {
        return sqlQuery;
    }

    @JsonGetter
    public String getConnectionStringSetting() {
        return connectionStringSetting;
    }

    @JsonGetter
    public String isCreateIfNotExists() {
        return createIfNotExists;
    }

    @JsonGetter
    public String isCreateLeaseCollectionIfNotExists() {
        return createLeaseCollectionIfNotExists;
    }

    @JsonGetter
    public String getLeaseCollectionName() {
        return leaseCollectionName;
    }

    @JsonGetter
    public int getCollectionThroughput() {
        return collectionThroughput;
    }

    @JsonGetter
    public String getPartitionKey() {
        return partitionKey;
    }
}
