package com.microsoft.azure.toolkit.lib.common.messager;

import javax.annotation.Nonnull;

public interface IAzureMessager {
    String DEFAULT_TITLE = "Azure";

    default void success(@Nonnull String message, String title, IAzureMessage.Action... actions) {
        this.show(new SimpleMessage(IAzureMessage.Type.SUCCESS, message).setActions(actions).setTitle(title));
    }

    default void info(@Nonnull String message, String title, IAzureMessage.Action... actions) {
        this.show(new SimpleMessage(IAzureMessage.Type.INFO, message).setActions(actions).setTitle(title));
    }

    default void warning(@Nonnull String message, String title, IAzureMessage.Action... actions) {
        this.show(new SimpleMessage(IAzureMessage.Type.WARNING, message).setActions(actions).setTitle(title));
    }

    default void error(@Nonnull String message, String title, IAzureMessage.Action... actions) {
        this.show(new SimpleMessage(IAzureMessage.Type.ERROR, message).setActions(actions).setTitle(title));
    }

    default void error(@Nonnull Throwable throwable, String title, IAzureMessage.Action... actions) {
        this.show(new SimpleMessage(IAzureMessage.Type.ERROR, throwable.getMessage()).setPayload(throwable).setActions(actions).setTitle(title));
    }

    default boolean confirm(@Nonnull String message, String title) {
        return this.show(new SimpleMessage(IAzureMessage.Type.CONFIRM, message).setTitle(title));
    }

    default void alert(@Nonnull String message, String title) {
        this.show(new SimpleMessage(IAzureMessage.Type.ALERT, message).setTitle(title));
    }

    default void success(@Nonnull String message, String title) {
        this.success(message, title, new IAzureMessage.Action[0]);
    }

    default void info(@Nonnull String message, String title) {
        this.info(message, title, new IAzureMessage.Action[0]);
    }

    default void warning(@Nonnull String message, String title) {
        this.warning(message, title, new IAzureMessage.Action[0]);
    }

    default void error(@Nonnull String message, String title) {
        this.error(message, title, new IAzureMessage.Action[0]);
    }

    default void error(@Nonnull Throwable throwable, String title) {
        this.error(throwable, title, new IAzureMessage.Action[0]);
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

    boolean show(IAzureMessage message);
}
