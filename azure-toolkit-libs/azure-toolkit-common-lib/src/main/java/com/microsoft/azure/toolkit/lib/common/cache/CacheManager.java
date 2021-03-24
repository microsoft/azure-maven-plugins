package com.microsoft.azure.toolkit.lib.common.cache;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.microsoft.azure.toolkit.lib.common.utils.aspect.ExpressionUtils;
import com.microsoft.azure.toolkit.lib.common.utils.aspect.MethodInvocation;
import lombok.extern.java.Log;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.aspectj.lang.reflect.MethodSignature;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Aspect
@Log
public class CacheManager {
    private static final CacheLoader<String, Cache<Object, Object>> loader = new CacheLoader<String, Cache<Object, Object>>() {
        @Override
        public Cache<Object, Object> load(@Nonnull String key) {
            return CacheBuilder.newBuilder()
                    .softValues()
                    .expireAfterAccess(4, TimeUnit.HOURS) // TODO: justify
                    .build();
        }
    };
    private static final LoadingCache<String, Cache<Object, Object>> caches = CacheBuilder.newBuilder()
            .softValues()
            .expireAfterAccess(4, TimeUnit.HOURS) // TODO: justify
            .build(loader);

    @Pointcut("execution(@com.microsoft.azure.toolkit.lib.common.cache.Cacheable * *..*.*(..))")
    public void cacheable() {
    }

    @Pointcut("execution(@com.microsoft.azure.toolkit.lib.common.cache.CacheEvict * *..*.*(..))")
    public void cacheEvict() {
    }

    @Around("cacheable()")
    public Object aroundCacheable(@Nonnull final ProceedingJoinPoint point) throws Throwable {
        final MethodSignature signature = (MethodSignature) point.getSignature();
        final MethodInvocation invocation = MethodInvocation.from(point);
        final Cacheable annotation = signature.getMethod().getAnnotation(Cacheable.class);

        final String cacheName = StringUtils.firstNonBlank(annotation.cacheName(), annotation.value());
        final String name = ExpressionUtils.render(cacheName, invocation);
        final String key = ExpressionUtils.render(annotation.key(), invocation);

        if (Objects.isNull(name) || Objects.isNull(key)) {
            log.severe(String.format("invalid @Cacheable on method(%s)", signature.getName()));
            return point.proceed();
        }
        final String condition = annotation.condition();
        final boolean toUseCache = StringUtils.isBlank(condition) || ExpressionUtils.evaluate(condition, invocation, true);
        final Cache<Object, Object> cache = caches.get(name);
        if (toUseCache) {
            return readCache(cache, key, point);
        }
        final Object result = point.proceed();
        if (Objects.nonNull(result)) {
            cache.put(key, Optional.of(result));
        }
        return result;
    }

    @Around("cacheEvict()")
    public Object aroundCacheEvict(@Nonnull final ProceedingJoinPoint point) throws Throwable {
        final MethodSignature signature = (MethodSignature) point.getSignature();
        final MethodInvocation invocation = MethodInvocation.from(point);
        final CacheEvict annotation = signature.getMethod().getAnnotation(CacheEvict.class);

        final String cacheName = StringUtils.firstNonBlank(annotation.cacheName(), annotation.value());
        final String name = ExpressionUtils.render(cacheName, invocation);
        final String key = ExpressionUtils.render(annotation.key(), invocation);
        final String condition = annotation.condition();
        final boolean toEvictCache = StringUtils.isBlank(condition) || ExpressionUtils.evaluate(condition, invocation, true);

        if (toEvictCache) {
            invalidateCache(name, key);
        }
        return point.proceed();
    }

    private void invalidateCache(@Nullable final String name, @Nullable final String key) throws ExecutionException {
        if (StringUtils.isBlank(name)) { // invalidate all cache entries if cache name not specified
            caches.invalidateAll();
        } else {
            final Cache<Object, Object> cache = caches.get(name);
            if (StringUtils.isBlank(key)) { // invalidate all cache entries of named cache if only cache name is specified
                cache.invalidateAll();
            } else { // invalidate key specified cache entry of named cache if both cache name and key are specified
                cache.invalidate(key);
            }
        }
    }

    private Object readCache(Cache<Object, Object> cache, String key, ProceedingJoinPoint point) throws Throwable {
        final Optional<?> result = (Optional<?>) cache.get(key, () -> {
            try {
                return Optional.ofNullable(point.proceed());
            } catch (final Throwable throwable) {
                return Optional.of(throwable);
            }
        });
        if (result.isPresent() && result.get() instanceof Throwable) {
            cache.invalidate(key);
            throw (Throwable) result.get();
        } else {
            return result.orElse(null);
        }
    }
}
