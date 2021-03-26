package com.microsoft.azure.toolkit.lib.common.cache;

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
public @interface CacheEvict {

    /**
     * alias of {@link Cacheable#cacheName()} }
     * string literal or groovy template for computing the cacheName dynamically.
     * e.g. groovy templates: {@code "$subscriptionId" }, {@code "$subscriptionId-$clusterId"}, {@code "${this.subscriptionId}"}
     * {@link CacheEvict#ALL} to evict all caches.
     */
    String value() default "";

    /**
     * string literal or groovy template for computing the cacheName dynamically.
     * e.g. groovy templates: {@code "$subscriptionId" }, {@code "$subscriptionId-$clusterId"}, {@code "${this.subscriptionId}"}
     * {@link CacheEvict#ALL} to evict all caches.
     */
    String cacheName() default "";

    /**
     * string literal or groovy template for computing the key dynamically.
     * e.g. groovy templates: {@code "$subscriptionId" }, {@code "$subscriptionId-$clusterId"}, {@code "${this.subscriptionId}"}
     * {@link CacheEvict#ALL} to evict all the cache entries of the named cache .
     */
    String key() default "";

    /**
     * groovy expression used for making the method caching conditional.
     * e.g. groovy expression: {@code "this.isLoading()" } {@code "this.loading" },  {@code "this.subscriptionId=='xxx'" }
     */
    String condition() default "";

    String ALL = "<ALL>";
}
