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

@JsonInclude(JsonInclude.Include.NON_NULL)
public class CosmosDBBinding extends BaseBinding {
    public static final String COSMOS_DB_TRIGGER = "cosmosDBTrigger";
    public static final String COSMOS_DB = "cosmosDB";

    private Integer collectionThroughput = null;
    private String databaseName = null;
    private String collectionName = null;
    private String leaseCollectionName = null;
    private String id = null;
    private String sqlQuery = null;
    private String connectionStringSetting = null;
    private String partitionKey = null;
    private Boolean createIfNotExists = null;
    private Boolean createLeaseCollectionIfNotExists = null;
    private Boolean useMultipleWriteLocations = null;
    private String preferredLocations = null;
    private Integer leasesCollectionThroughput = null;
    private String leaseCollectionPrefix = null;
    private Integer checkpointInterval = null;
    private Integer checkpointDocumentCount = null;
    private Integer feedPollDelay = null;
    private Integer leaseRenewInterval = null;
    private Integer leaseAcquireInterval = null;
    private Integer leaseExpirationInterval = null;
    private Integer maxItemsPerInvocation = null;
    private Boolean startFromBeginning = null;
    private String leaseDatabaseName = null;
    private String leaseConnectionStringSetting = null;

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
        createIfNotExists = dbOutput.createIfNotExists();
        connectionStringSetting = dbOutput.connectionStringSetting();
        partitionKey = dbOutput.partitionKey();
        collectionThroughput = dbOutput.collectionThroughput();
        useMultipleWriteLocations = dbOutput.useMultipleWriteLocations();
        preferredLocations = dbOutput.preferredLocations();
    }

    public CosmosDBBinding(final CosmosDBTrigger dbTrigger) {
        super(dbTrigger.name(), COSMOS_DB_TRIGGER, Direction.IN, dbTrigger.dataType());

        databaseName = dbTrigger.databaseName();
        collectionName = dbTrigger.collectionName();
        leaseConnectionStringSetting = dbTrigger.leaseConnectionStringSetting();
        leaseCollectionName = dbTrigger.leaseCollectionName();
        leaseDatabaseName = dbTrigger.leaseDatabaseName();
        createLeaseCollectionIfNotExists = dbTrigger.createLeaseCollectionIfNotExists();
        leasesCollectionThroughput = dbTrigger.leasesCollectionThroughput();
        leaseCollectionPrefix = dbTrigger.leaseCollectionPrefix();
        checkpointInterval = dbTrigger.checkpointInterval();
        checkpointDocumentCount = dbTrigger.checkpointDocumentCount();
        feedPollDelay = dbTrigger.feedPollDelay();
        connectionStringSetting = dbTrigger.connectionStringSetting();
        leaseRenewInterval = dbTrigger.leaseRenewInterval();
        leaseAcquireInterval = dbTrigger.leaseAcquireInterval();
        leaseExpirationInterval = dbTrigger.leaseExpirationInterval();
        maxItemsPerInvocation = dbTrigger.maxItemsPerInvocation();
        startFromBeginning = dbTrigger.startFromBeginning();
        preferredLocations = dbTrigger.preferredLocations();
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
    public Boolean isCreateIfNotExists() {
        return createIfNotExists;
    }

    @JsonGetter
    public Boolean isCreateLeaseCollectionIfNotExists() {
        return createLeaseCollectionIfNotExists;
    }

    @JsonGetter
    public String getLeaseCollectionName() {
        return leaseCollectionName;
    }

    @JsonGetter
    public Integer getCollectionThroughput() {
        return collectionThroughput;
    }

    @JsonGetter
    public String getPartitionKey() {
        return partitionKey;
    }

    @JsonGetter
    public Boolean getCreateIfNotExists() {
        return createIfNotExists;
    }

    @JsonGetter
    public Boolean getCreateLeaseCollectionIfNotExists() {
        return createLeaseCollectionIfNotExists;
    }

    @JsonGetter
    public Boolean getUseMultipleWriteLocations() {
        return useMultipleWriteLocations;
    }

    @JsonGetter
    public String getPreferredLocations() {
        return preferredLocations;
    }

    @JsonGetter
    public Integer getLeasesCollectionThroughput() {
        return leasesCollectionThroughput;
    }

    @JsonGetter
    public String getLeaseCollectionPrefix() {
        return leaseCollectionPrefix;
    }

    @JsonGetter
    public Integer getCheckpointInterval() {
        return checkpointInterval;
    }

    @JsonGetter
    public Integer getCheckpointDocumentCount() {
        return checkpointDocumentCount;
    }

    @JsonGetter
    public Integer getFeedPollDelay() {
        return feedPollDelay;
    }

    @JsonGetter
    public Integer getLeaseRenewInterval() {
        return leaseRenewInterval;
    }

    @JsonGetter
    public Integer getLeaseAcquireInterval() {
        return leaseAcquireInterval;
    }

    @JsonGetter
    public Integer getLeaseExpirationInterval() {
        return leaseExpirationInterval;
    }

    @JsonGetter
    public Integer getMaxItemsPerInvocation() {
        return maxItemsPerInvocation;
    }

    @JsonGetter
    public Boolean getStartFromBeginning() {
        return startFromBeginning;
    }

    @JsonGetter
    public String getLeaseDatabaseName() {
        return leaseDatabaseName;
    }

    @JsonGetter
    public String getLeaseConnectionStringSetting() {
        return leaseConnectionStringSetting;
    }
}
