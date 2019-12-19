package com.microsoft.azure.common;

public interface IProviderContext {
	void registerProvider(Class<? extends Object> clazz, Object provider);

	<T>T getProvider(Class<T> clazz);
}
