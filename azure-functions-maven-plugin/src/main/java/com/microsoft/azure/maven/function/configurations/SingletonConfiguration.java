/**
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for
 * license information.
 */

package com.microsoft.azure.maven.function.configurations;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class SingletonConfiguration {
    private String lockPeriod;

    private String listenerLockPeriod;

    private String listenerLockRecoveryPollingInterval;

    private String lockAcquisitionTimeout;

    private String lockAcquisitionPollingInterval;

    @JsonGetter("lockPeriod")
    public String getLockPeriod() {
        return lockPeriod;
    }

    public void setLockPeriod(String lockPeriod) {
        this.lockPeriod = lockPeriod;
    }

    @JsonGetter("listenerLockPeriod")
    public String getListenerLockPeriod() {
        return listenerLockPeriod;
    }

    public void setListenerLockPeriod(String listenerLockPeriod) {
        this.listenerLockPeriod = listenerLockPeriod;
    }

    @JsonGetter("listenerLockRecoveryPollingInterval")
    public String getListenerLockRecoveryPollingInterval() {
        return listenerLockRecoveryPollingInterval;
    }

    public void setListenerLockRecoveryPollingInterval(String listenerLockRecoveryPollingInterval) {
        this.listenerLockRecoveryPollingInterval = listenerLockRecoveryPollingInterval;
    }

    @JsonGetter("lockAcquisitionTimeout")
    public String getLockAcquisitionTimeout() {
        return lockAcquisitionTimeout;
    }

    public void setLockAcquisitionTimeout(String lockAcquisitionTimeout) {
        this.lockAcquisitionTimeout = lockAcquisitionTimeout;
    }

    @JsonGetter("lockAcquisitionPollingInterval")
    public String getLockAcquisitionPollingInterval() {
        return lockAcquisitionPollingInterval;
    }

    public void setLockAcquisitionPollingInterval(String lockAcquisitionPollingInterval) {
        this.lockAcquisitionPollingInterval = lockAcquisitionPollingInterval;
    }
}
