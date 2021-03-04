package com.microsoft.azure.toolkit.lib.cache;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(value = {ElementType.METHOD})
@Retention(value = RetentionPolicy.RUNTIME)
@Inherited
@Documented
public @interface Cacheable {
    /**
     * alias for name
     */
    String value() default "";

    String cacheName();

    /**
     * jtwig expression for computing the key dynamically.
     * e.g. {@code "{{$subscriptionId}}-{{$clusterId}}"}
     * e.g. {@code "$subscriptionId" }
     */
    String key();
}
