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
public @interface Cacheable {

    /**
     * alias of {@link Cacheable#cacheName()} }
     * jtwig expression or template for computing the cacheName dynamically.
     * e.g. jtwig expression: {@code "$subscriptionId" }
     * e.g. jtwig template: {@code "{{$subscriptionId}}-{{$clusterId}}"}
     */
    String value() default "";

    /**
     * jtwig expression or template for computing the cacheName dynamically.
     * e.g. jtwig expression: {@code "$subscriptionId" }
     * e.g. jtwig template: {@code "{{$subscriptionId}}-{{$clusterId}}"}
     */
    String cacheName() default "";

    /**
     * jtwig expression or template for computing the key dynamically.
     * e.g. jtwig expression: {@code "$subscriptionId" }
     * e.g. jtwig template: {@code "{{$subscriptionId}}-{{$clusterId}}"}
     */
    String key() default "<cache>";

    /**
     * jtwig expression used for making the method caching conditional.
     * e.g. jtwig expression: {@code "$subscriptionId.equals("xxx")" }
     */
    String condition() default "";
}
