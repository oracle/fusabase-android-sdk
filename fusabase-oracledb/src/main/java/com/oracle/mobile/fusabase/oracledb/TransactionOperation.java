// Copyright (c) 2015, 2025, Oracle and/or its affiliates.

//-----------------------------------------------------------------------------
//
// This software is dual-licensed to you under the Universal Permissive License
// (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl and Apache License
// 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
// either license.
//
// If you elect to accept the software under the Apache License, Version 2.0,
// the following applies:
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
//-----------------------------------------------------------------------------

package com.oracle.mobile.fusabase.oracledb;

import androidx.annotation.NonNull;
import java.util.Objects;
/**
 * Represents the operational parameters for a transaction operation,
 * including transaction boundaries, naming, and versioning information.
 */
class TransactionOperation {

    /** Default version number used for new documents. */
    protected final static String DEFAULT_VERSION = "1";

    /** Version number indicating a document does not exist. */
    protected final static String DOES_NOT_EXIST_VERSION = "0";

    /** Flag indicating the beginning of a transaction (1) or continuation (0). */
    private int beginTrans;

    /** Flag indicating the end of a transaction (1) or continuation (0). */
    private int endTrans;

    /** Unique name identifying this transaction. */
    private final String transactionName;

    /** Configuration options for this transaction operation. */
    private final TransactionOptions options;

    /** Version number of the document being operated on. */
    private String version;

    /**
     * Constructor for creating TransactionOperation with full parameters.
     *
     * @param beginTrans 1 if this begins a transaction, 0 otherwise
     * @param endTrans 1 if this ends a transaction, 0 otherwise
     * @param transactionName unique name for the transaction
     * @param options transaction configuration options
     * @param version current version of the document
     */
    public TransactionOperation (int beginTrans,
                                 int endTrans,
                                 @NonNull String transactionName,
                                 @NonNull TransactionOptions options,
                                 String version) {
        Objects.requireNonNull(transactionName, "Transaction name cannot be null");
        Objects.requireNonNull(options, "TransactionOptions cannot be null");
        this.beginTrans = beginTrans;
        this.endTrans = endTrans;
        this.transactionName = transactionName;
        this.options = options;
        this.version = version;
    }

    /**
     * Constructor for creating TransactionOperation with minimal parameters.
     *
     * @param options transaction configuration options
     */
    public TransactionOperation (@NonNull TransactionOptions options) {
        java.util.Objects.requireNonNull(options, "TransactionOptions cannot be null");
        this.beginTrans = 0;
        this.endTrans = 0;
        this.transactionName = "";
        this.options = options;
        this.version = DEFAULT_VERSION;
    }

    /**
     * Default constructor creating TransactionOperation with default values.
     */
    public TransactionOperation() {
        this.beginTrans = 0;
        this.endTrans = 0;
        this.transactionName = "";
        this.options = new TransactionOptions.Builder().build();
        this.version = DEFAULT_VERSION;
    }

    /**
     * Returns the version number of the document.
     *
     * @return the document version
     */
    public String getVersion() {
        return this.version;
    }

    /**
     * Returns the begin transaction flag.
     *
     * @return 1 if this operation begins a transaction, 0 otherwise
     */
    public int getBeginTrans() {
        return this.beginTrans;
    }

    /**
     * Returns the end transaction flag.
     *
     * @return 1 if this operation ends a transaction, 0 otherwise
     */
    public int getEndTrans() {
        return this.endTrans;
    }

    /**
     * Returns the unique name of the transaction.
     *
     * @return the transaction name
     */
    public String getTransactionName() {
        return this.transactionName;
    }

    /**
     * Returns the transaction options.
     *
     * @return the transaction configuration options
     */
    public TransactionOptions getOptions() {
        return options;
    }

    /**
     * Sets the version number of the document.
     *
     * @param version the new version number
     */
    public void setVersion(String version) {
        this.version = version;
    }

    protected void unsetBeginTrans() { this.beginTrans = 0; }

    protected void unsetEndTrans() { this.endTrans = 0; }

    protected void setBeginTrans() { this.beginTrans = 1; }

    protected void setEndTrans() { this.endTrans = 1;}
}
