/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.tools.utils;

import rx.Observable;
import rx.schedulers.Schedulers;

import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

public class RxUtils {
    private static final int POLLING_INTERVAL = 1;

    /**
     * Get resource repeatedly until it match the predicate or timeout, will return null when meet exception
     * with default pollingInterval = 1s
     * @param callable         callable to get resource
     * @param predicate        function that evaluate the resource
     * @param timeOutInSeconds max time for the method
     * @return the first resource which fit the predicate or the last result before timeout
     */
    public static <T> T pollUntil(Callable<T> callable, Predicate<T> predicate, int timeOutInSeconds) {
        return RxUtils.pollUntil(callable, predicate, timeOutInSeconds, POLLING_INTERVAL);
    }

    /**
     * Get resource repeatedly until it match the predicate or timeout, will return null when meet exception
     *
     * @param callable         callable to get resource
     * @param predicate        function that evaluate the resource
     * @param timeOutInSeconds max time for the method
     * @param pollingInterval  polling interval
     * @return the first resource which fit the predicate or the last result before timeout
     */
    public static <T> T pollUntil(Callable<T> callable, Predicate<T> predicate, int timeOutInSeconds, int pollingInterval) {
        final long timeout = System.currentTimeMillis() + timeOutInSeconds * 1000;
        return Observable.interval(pollingInterval, TimeUnit.SECONDS)
            .timeout(timeOutInSeconds, TimeUnit.SECONDS)
            .flatMap(aLong -> Observable.fromCallable(callable))
            .subscribeOn(Schedulers.io())
            .takeUntil(resource -> predicate.test(resource) || System.currentTimeMillis() > timeout)
            .toBlocking().last();
    }
}
