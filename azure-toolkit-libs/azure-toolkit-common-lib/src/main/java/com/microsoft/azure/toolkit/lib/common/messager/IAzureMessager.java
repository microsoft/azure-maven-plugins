package com.microsoft.azure.toolkit.lib.common.messager;

import javax.annotation.Nonnull;

public interface IAzureMessager {

    default boolean confirm(@Nonnull String message, String... title) {
        return false;
    }

    default void alert(@Nonnull String message, String... title) {
    }

    default void success(@Nonnull String message, String... title) {
    }

    default void info(@Nonnull String message, String... title) {
    }

    default void warning(@Nonnull String message, String... title) {
    }

    default void error(@Nonnull String message, String... title) {
    }

    default void error(@Nonnull Throwable throwable, String... title) {
    }

    default String value(String val) {
        return val;
    }
}
