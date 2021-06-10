package com.microsoft.azure.toolkit.lib.common.messager;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.function.Consumer;

@RequiredArgsConstructor
@Getter
public class SimpleMessageAction implements IAzureMessage.Action {
    private final String name;
    private final Consumer<IAzureMessage> handler;

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public void actionPerformed(IAzureMessage payload) {
        this.handler.accept(payload);
    }

    @Override
    public String toString() {
        return String.format("[%s]", this.name);
    }
}