/*
 * Copyright (c) Microsoft Corporation. All rights reserved.
 * Licensed under the MIT License. See License.txt in the project root for license information.
 */

package com.microsoft.azure.toolkit.lib.common.messager;

import com.microsoft.azure.toolkit.lib.common.bundle.AzureString;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Optional;

public interface IAzureMessager {
    String DEFAULT_TITLE = AzureMessage.DEFAULT_TITLE;

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void success(@Nonnull String message, Object... actions) {
        this.show(this.buildMessage(IAzureMessage.Type.SUCCESS, AzureString.fromString(message), null, actions, null));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void success(@Nonnull String message, String title, Object... actions) {
        this.show(this.buildMessage(IAzureMessage.Type.SUCCESS, AzureString.fromString(message), title, actions, null));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void success(@Nonnull AzureString message, Object... actions) {
        this.show(this.buildMessage(IAzureMessage.Type.SUCCESS, message, null, actions, null));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void success(@Nonnull AzureString message, String title, Object... actions) {
        this.show(this.buildMessage(IAzureMessage.Type.SUCCESS, message, title, actions, null));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void info(@Nonnull String message, Object... actions) {
        this.show(this.buildMessage(IAzureMessage.Type.INFO, AzureString.fromString(message), null, actions, null));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void info(@Nonnull String message, String title, Object... actions) {
        this.show(this.buildMessage(IAzureMessage.Type.INFO, AzureString.fromString(message), title, actions, null));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void info(@Nonnull AzureString message, Object... actions) {
        this.show(this.buildMessage(IAzureMessage.Type.INFO, message, null, actions, null));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void info(@Nonnull AzureString message, String title, Object... actions) {
        this.show(this.buildMessage(IAzureMessage.Type.INFO, message, title, actions, null));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void warning(@Nonnull String message, Object... actions) {
        this.show(this.buildMessage(IAzureMessage.Type.WARNING, AzureString.fromString(message), null, actions, null));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void warning(@Nonnull String message, String title, Object... actions) {
        this.show(this.buildMessage(IAzureMessage.Type.WARNING, AzureString.fromString(message), title, actions, null));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void warning(@Nonnull AzureString message, Object... actions) {
        this.show(this.buildMessage(IAzureMessage.Type.WARNING, message, null, actions, null));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void warning(@Nonnull AzureString message, String title, Object... actions) {
        this.show(this.buildMessage(IAzureMessage.Type.WARNING, message, title, actions, null));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void error(@Nonnull String message, Object... actions) {
        this.show(this.buildMessage(IAzureMessage.Type.ERROR, AzureString.fromString(message), null, actions, null));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void error(@Nonnull String message, String title, Object... actions) {
        this.show(this.buildMessage(IAzureMessage.Type.ERROR, AzureString.fromString(message), title, actions, null));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void error(@Nonnull AzureString message, Object... actions) {
        this.show(this.buildMessage(IAzureMessage.Type.ERROR, message, null, actions, null));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void error(@Nonnull AzureString message, String title, Object... actions) {
        this.show(this.buildMessage(IAzureMessage.Type.ERROR, message, title, actions, null));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void error(@Nonnull Throwable throwable, Object... actions) {
        final String message = Optional.ofNullable(throwable.getMessage()).orElse(throwable.getClass().getSimpleName());
        this.show(this.buildMessage(IAzureMessage.Type.ERROR, AzureString.fromString(message), null, actions, throwable));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void error(@Nonnull Throwable throwable, String title, Object... actions) {
        final String message = Optional.ofNullable(throwable.getMessage()).orElse(throwable.getClass().getSimpleName());
        this.show(this.buildMessage(IAzureMessage.Type.ERROR, AzureString.fromString(message), title, actions, throwable));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void debug(@Nonnull String message, Object... actions) {
        this.show(this.buildMessage(IAzureMessage.Type.DEBUG, AzureString.fromString(message), null, actions, null));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void debug(@Nonnull String message, String title, Object... actions) {
        this.show(this.buildMessage(IAzureMessage.Type.DEBUG, AzureString.fromString(message), title, actions, null));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void debug(@Nonnull AzureString message, Object... actions) {
        this.show(this.buildMessage(IAzureMessage.Type.DEBUG, message, null, actions, null));
    }

    /**
     * @param actions array of action id or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action} or
     *                {@link com.microsoft.azure.toolkit.lib.common.action.Action.Id}
     */
    default void debug(@Nonnull AzureString message, String title, Object... actions) {
        this.show(this.buildMessage(IAzureMessage.Type.DEBUG, message, title, actions, null));
    }

    default boolean confirm(@Nonnull String message, String title) {
        return this.show(this.buildMessage(IAzureMessage.Type.CONFIRM, AzureString.fromString(message), title, null, null));
    }

    default boolean confirm(@Nonnull AzureString message, String title) {
        return this.show(this.buildMessage(IAzureMessage.Type.CONFIRM, message, title, null, null));
    }

    default void alert(@Nonnull String message, String title) {
        this.show(this.buildMessage(IAzureMessage.Type.ALERT, AzureString.fromString(message), title, null, null));
    }

    default void alert(@Nonnull AzureString message, String title) {
        this.show(this.buildMessage(IAzureMessage.Type.ALERT, message, title, null, null));
    }

    default boolean confirm(@Nonnull String message) {
        return this.confirm(message, null);
    }

    default boolean confirm(@Nonnull AzureString message) {
        return this.confirm(message, null);
    }

    default void alert(@Nonnull String message) {
        this.alert(message, null);
    }

    default void alert(@Nonnull AzureString message) {
        this.alert(message, null);
    }

    default void success(@Nonnull String message) {
        this.success(message, (Object[]) null);
    }

    default void success(@Nonnull AzureString message) {
        this.success(message, (Object[]) null);
    }

    default void info(@Nonnull String message) {
        this.info(message, (Object[]) null);
    }

    default void info(@Nonnull AzureString message) {
        this.info(message, (Object[]) null);
    }

    default void warning(@Nonnull String message) {
        this.warning(message, (Object[]) null);
    }

    default void warning(@Nonnull AzureString message) {
        this.warning(message, (Object[]) null);
    }

    default void error(@Nonnull String message) {
        this.error(message, (Object[]) null);
    }

    default void error(@Nonnull AzureString message) {
        this.error(message, (Object[]) null);
    }

    default void error(@Nonnull Throwable throwable) {
        this.error(throwable, (Object[]) null);
    }

    default void debug(@Nonnull String message) {
        this.debug(message, (Object[]) null);
    }

    default void debug(@Nonnull AzureString message) {
        this.debug(message, (Object[]) null);
    }

    default IAzureMessage buildMessage(@Nonnull IAzureMessage.Type type, @Nonnull AzureString content,
                                       @Nullable String title, @Nullable Object[] actions, @Nullable Object payload) {
        final AzureMessage message = new AzureMessage(type, content).setPayload(payload).setActions(actions).setTitle(title);
        if (this instanceof IAzureMessage.ValueDecorator) {
            message.setValueDecorator((IAzureMessage.ValueDecorator) this);
        }
        return message;
    }

    boolean show(IAzureMessage message);
}
