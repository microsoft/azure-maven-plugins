package com.microsoft.azure.toolkit.lib.common.messager;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import lombok.experimental.Accessors;

import javax.annotation.Nonnull;

@Getter
@Setter
@RequiredArgsConstructor
@Accessors(chain = true)
@ToString
public class SimpleMessage implements IAzureMessage {
    @Nonnull
    private final Type type;
    private String title;
    @Nonnull
    private final String message;
    @ToString.Exclude
    private Object payload;
    @ToString.Exclude
    private Action[] actions;
}
