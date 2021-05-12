package com.microsoft.azure.toolkit.lib.common.messager;

import javax.annotation.Nonnull;

public interface IAzureMessager {
    public static final String DEFAULT_TITLE = "Azure";

    default boolean confirm(@Nonnull String message, String title) {
        return false;
    }

    default void alert(@Nonnull String message, String title) {
    }

    default void success(@Nonnull String message, String title) {
    }

    default void info(@Nonnull String message, String title) {
    }

    default void warning(@Nonnull String message, String title) {
    }

    default void error(@Nonnull String message, String title) {
    }

    default void error(@Nonnull Throwable throwable, String title) {
    }

    default void error(@Nonnull Throwable throwable, @Nonnull String message, String title) {
    }

    default boolean confirm(@Nonnull String message) {
        return this.confirm(message, DEFAULT_TITLE);
    }

    default void alert(@Nonnull String message) {
        this.alert(message, DEFAULT_TITLE);
    }

    default void success(@Nonnull String message) {
        this.success(message, DEFAULT_TITLE);
    }

    default void info(@Nonnull String message) {
        this.info(message, DEFAULT_TITLE);
    }

    default void warning(@Nonnull String message) {
        this.warning(message, DEFAULT_TITLE);
    }

    default void error(@Nonnull String message) {
        this.error(message, DEFAULT_TITLE);
    }

    default void error(@Nonnull Throwable throwable) {
        this.error(throwable, DEFAULT_TITLE);
    }

    default String value(String val) {
        return val;
    }
}
