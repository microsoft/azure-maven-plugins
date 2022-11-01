/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.operation;

import java.util.HashSet;
import java.util.Set;

public class OperationManager {
    private final Set<OperationListener> listeners = new HashSet<>();

    private OperationManager() {
    }

    public static OperationManager getInstance() {
        return SingletonHolder.INSTANCE;
    }

    public void addListener(OperationListener listener) {
        this.listeners.add(listener);
    }

    public void removeListener(OperationListener listener) {
        this.listeners.remove(listener);
    }

    void fireBeforeEnter(Operation operation, Object source) {
        try {
            this.listeners.forEach(l -> l.beforeEnter(operation, source));
        } catch (Throwable e) {
            // ignore
        }
    }

    void fireAfterReturning(Operation operation, Object source) {
        try {
            this.listeners.forEach(l -> l.afterReturning(operation, source));
        } catch (Throwable e) {
            // ignore
        }
    }

    void fireAfterThrowing(Throwable e, Operation operation, Object source) {
        try {
            this.listeners.forEach(l -> l.afterThrowing(e, operation, source));
        } catch (Throwable ex) {
            // ignore
        }
    }

    private static class SingletonHolder {
        public static final OperationManager INSTANCE = new OperationManager();
    }
}
