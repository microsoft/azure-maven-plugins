/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.azure.serverless.functions.annotation.CosmosDBInput;
import com.microsoft.azure.serverless.functions.annotation.CosmosDBOutput;
import com.microsoft.azure.serverless.functions.annotation.CosmosDBTrigger;

@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class CosmosDBBinding extends BaseBinding {
    public static final String COSMOS_DB_TRIGGER = "cosmosDBTrigger";
    public static final String COSMOS_DB = "cosmosdb";

    private String databaseName = "";

    private String collectionName = "";

    private String leaseCollectionName = "";

    private String id = "";

    private String sqlQuery = "";

    private String connectionStringSetting = "";

    private boolean createIfNotExists = false;

    private boolean createLeaseCollectionIfNotExists = false;

    public CosmosDBBinding(final CosmosDBInput dbInput) {
        super(dbInput.name(), COSMOS_DB, Direction.IN, dbInput.dataType());

        databaseName = dbInput.databaseName();
        collectionName = dbInput.collectionName();
        id = dbInput.id();
        sqlQuery = dbInput.sqlQuery();
        connectionStringSetting = dbInput.connectionStringSetting();
    }

    public CosmosDBBinding(final CosmosDBOutput dbOutput) {
        super(dbOutput.name(), COSMOS_DB, Direction.OUT, dbOutput.dataType());

        databaseName = dbOutput.databaseName();
        collectionName = dbOutput.collectionName();
        connectionStringSetting = dbOutput.connectionStringSetting();
        createIfNotExists = dbOutput.createIfNotExists();
    }

    public CosmosDBBinding(final CosmosDBTrigger dbTrigger) {
        super(dbTrigger.name(), COSMOS_DB_TRIGGER, Direction.IN, dbTrigger.dataType());

        databaseName = dbTrigger.databaseName();
        collectionName = dbTrigger.collectionName();
        leaseCollectionName = dbTrigger.leaseCollectionName();
        connectionStringSetting = dbTrigger.connectionStringSetting();
        createLeaseCollectionIfNotExists = dbTrigger.createLeaseCollectionIfNotExists();
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
    public boolean isCreateIfNotExists() {
        return createIfNotExists;
    }

    @JsonGetter
    public boolean isCreateLeaseCollectionIfNotExists() {
        return createLeaseCollectionIfNotExists;
    }

    @JsonGetter
    public String getLeaseCollectionName() {
        return leaseCollectionName;
    }
}
