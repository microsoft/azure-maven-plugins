/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.bindings;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.microsoft.azure.serverless.functions.annotation.DocumentDBInput;
import com.microsoft.azure.serverless.functions.annotation.DocumentDBOutput;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class DocumentDBBinding extends BaseBinding {
    public static final String DOCUMENT_DB = "documentdb";

    private String databaseName = "";

    private String collectionName = "";

    private String id = "";

    private String sqlQuery = "";

    private String connection = "";

    private boolean createIfNotExists = false;

    public DocumentDBBinding(final DocumentDBInput dbInput) {
        super(dbInput.name(), DOCUMENT_DB, Direction.IN);

        databaseName = dbInput.databaseName();
        collectionName = dbInput.collectionName();
        id = dbInput.id();
        sqlQuery = dbInput.sqlQuery();
        connection = dbInput.connection();
    }

    public DocumentDBBinding(final DocumentDBOutput dbOutput) {
        super(dbOutput.name(), DOCUMENT_DB, Direction.OUT);

        databaseName = dbOutput.databaseName();
        collectionName = dbOutput.collectionName();
        connection = dbOutput.connection();
        createIfNotExists = dbOutput.createIfNotExists();
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
    public String getConnection() {
        return connection;
    }

    @JsonGetter
    public boolean isCreateIfNotExists() {
        return createIfNotExists;
    }
}
