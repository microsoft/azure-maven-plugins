package com.microsoft.azure.toolkit.lib.common.event;

import com.microsoft.azure.toolkit.lib.common.operation.AzureOperationRef;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import javax.annotation.Nonnull;

@Getter
@RequiredArgsConstructor
public class AzureOperationEvent<T extends AzureOperationEvent.Source<T>> implements AzureEvent<T> {
    private final T source;
    private final AzureOperationRef operation;
    private final Stage stage;

    public T getPayload() {
        return this.source;
    }

    public interface Source<T> {
        @Nonnull
        default Source<T> getEventSource() {
            return this;
        }
    }

    public enum Stage {
        BEFORE, AFTER, THROW
    }
}
