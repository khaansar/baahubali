package com.example.common.cache.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Enables an explicitly authorized request to evict selected Spring cache entries
 * before the annotated controller method invokes its underlying cached service.
 *
 * The keyExpression must resolve to exactly the same key type and value used by
 * the corresponding @Cacheable annotation. An empty keyExpression means the
 * Spring Cache default key for a zero-argument method: SimpleKey.EMPTY.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface CacheRefreshable {

    /**
     * Names of the cache regions from which entries should be evicted.
     */
    String[] cacheNames();

    /**
     * SpEL expression evaluated against the annotated controller method arguments.
     * Leave empty only when the cached service uses the default zero-argument key.
     */
    String keyExpression() default "";
}