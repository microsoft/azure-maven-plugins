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

import static com.microsoft.azure.maven.function.utils.AnnotationUtils.getNotDefaultValueFromAnnotation;

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
        super(dbInput.name(), COSMOS_DB, Direction.IN,
                getNotDefaultValueFromAnnotation(dbInput.dataType(), "dataType", dbInput));
        databaseName = getNotDefaultValueFromAnnotation(dbInput.databaseName(), "databaseName", dbInput);
        collectionName = getNotDefaultValueFromAnnotation(dbInput.collectionName(), "collectionName", dbInput);
        connectionStringSetting = getNotDefaultValueFromAnnotation(dbInput.connectionStringSetting(),
                "connectionStringSetting", dbInput);
        id = getNotDefaultValueFromAnnotation(dbInput.id(), "id", dbInput);
        partitionKey = getNotDefaultValueFromAnnotation(dbInput.partitionKey(), "partitionKey", dbInput);
        sqlQuery = getNotDefaultValueFromAnnotation(dbInput.sqlQuery(), "sqlQuery", dbInput);
    }

    public CosmosDBBinding(final CosmosDBOutput dbOutput) {
        super(dbOutput.name(), COSMOS_DB, Direction.IN,
                getNotDefaultValueFromAnnotation(dbOutput.dataType(), "dataType", dbOutput));

        databaseName = getNotDefaultValueFromAnnotation(dbOutput.databaseName(), "databaseName", dbOutput);
        collectionName = getNotDefaultValueFromAnnotation(dbOutput.collectionName(), "collectionName", dbOutput);
        createIfNotExists = getNotDefaultValueFromAnnotation(dbOutput.createIfNotExists(),
                "createIfNotExists", dbOutput);
        connectionStringSetting = getNotDefaultValueFromAnnotation(dbOutput.connectionStringSetting(),
                "connectionStringSetting", dbOutput);
        partitionKey = getNotDefaultValueFromAnnotation(dbOutput.partitionKey(), "partitionKey", dbOutput);
        collectionThroughput = getNotDefaultValueFromAnnotation(dbOutput.collectionThroughput(),
                "collectionThroughput", dbOutput);
        useMultipleWriteLocations = getNotDefaultValueFromAnnotation(dbOutput.useMultipleWriteLocations(),
                "useMultipleWriteLocations", dbOutput);
        preferredLocations = getNotDefaultValueFromAnnotation(dbOutput.preferredLocations(),
                "preferredLocations", dbOutput);
    }

    public CosmosDBBinding(final CosmosDBTrigger dbTrigger) {
        super(dbTrigger.name(), COSMOS_DB_TRIGGER, Direction.IN,
                getNotDefaultValueFromAnnotation(dbTrigger.dataType(), "dataType", dbTrigger));

        databaseName = getNotDefaultValueFromAnnotation(dbTrigger.databaseName(), "databaseName", dbTrigger);
        collectionName = getNotDefaultValueFromAnnotation(dbTrigger.collectionName(), "collectionName", dbTrigger);
        leaseConnectionStringSetting = getNotDefaultValueFromAnnotation(dbTrigger.leaseConnectionStringSetting(),
                "leaseConnectionStringSetting", dbTrigger);
        leaseCollectionName = getNotDefaultValueFromAnnotation(dbTrigger.leaseCollectionName(),
                "leaseCollectionName", dbTrigger);
        leaseDatabaseName = getNotDefaultValueFromAnnotation(dbTrigger.leaseDatabaseName(),
                "leaseDatabaseName", dbTrigger);
        createLeaseCollectionIfNotExists = getNotDefaultValueFromAnnotation(
                dbTrigger.createLeaseCollectionIfNotExists(), "createLeaseCollectionIfNotExists", dbTrigger);
        leasesCollectionThroughput = getNotDefaultValueFromAnnotation(dbTrigger.leasesCollectionThroughput(),
                "leasesCollectionThroughput", dbTrigger);
        leaseCollectionPrefix = getNotDefaultValueFromAnnotation(dbTrigger.leaseCollectionPrefix(),
                "leaseCollectionPrefix", dbTrigger);
        checkpointInterval = getNotDefaultValueFromAnnotation(dbTrigger.checkpointInterval(),
                "checkpointInterval", dbTrigger);
        checkpointDocumentCount = getNotDefaultValueFromAnnotation(dbTrigger.checkpointDocumentCount(),
                "checkpointDocumentCount", dbTrigger);
        connectionStringSetting = getNotDefaultValueFromAnnotation(dbTrigger.connectionStringSetting(),
                "connectionStringSetting", dbTrigger);
        leaseRenewInterval = getNotDefaultValueFromAnnotation(dbTrigger.leaseRenewInterval(),
                "leaseRenewInterval", dbTrigger);
        leaseAcquireInterval = getNotDefaultValueFromAnnotation(dbTrigger.leaseAcquireInterval(),
                "leaseAcquireInterval", dbTrigger);
        leaseExpirationInterval = getNotDefaultValueFromAnnotation(dbTrigger.leaseExpirationInterval(),
                "leaseExpirationInterval", dbTrigger);
        maxItemsPerInvocation = getNotDefaultValueFromAnnotation(dbTrigger.maxItemsPerInvocation(),
                "maxItemsPerInvocation", dbTrigger);
        startFromBeginning = getNotDefaultValueFromAnnotation(dbTrigger.startFromBeginning(),
                "startFromBeginning", dbTrigger);
        preferredLocations = getNotDefaultValueFromAnnotation(dbTrigger.preferredLocations(),
                "preferredLocations", dbTrigger);
        feedPollDelay = getNotDefaultValueFromAnnotation(dbTrigger.feedPollDelay(),
                "feedPollDelay", dbTrigger);
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
