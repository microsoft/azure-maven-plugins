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
     * alias of {@link CacheEvict#cacheName()} }
     * string literal or groovy expression or template for computing the cacheName dynamically.
     * e.g. groovy expression/templates: {@code "$subscriptionId" }, {@code "$subscriptionId-$clusterId"}, {@code "@.subscriptionId"}
     */
    String value() default "";

    /**
     * string literal or groovy expression or template for computing the cacheName dynamically.
     * e.g. groovy expression/templates: {@code "$subscriptionId" }, {@code "$subscriptionId-$clusterId"}, {@code "@.subscriptionId"}
     */
    String cacheName() default "";

    /**
     * string literal or groovy expression or template for computing the key dynamically.
     * e.g. groovy expression/templates: {@code "$subscriptionId" }, {@code "$subscriptionId-$clusterId"}, {@code "@.subscriptionId"}
     */
    String key() default "<cache>";

    /**
     * groovy expression used for making the method caching conditional.
     * e.g. groovy expression: {@code "$items && $items[0]=='xxx'" }
     */
    String condition() default "";
}
