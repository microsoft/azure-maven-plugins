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
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Aspect
@Log
public class CacheableAspect {
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

    @Around("cacheable()")
    public Object around(ProceedingJoinPoint point) throws Throwable {
        final MethodSignature signature = (MethodSignature) point.getSignature();
        final MethodInvocation invocation = MethodInvocation.from(point);
        final Cacheable annotation = signature.getMethod().getAnnotation(Cacheable.class);

        final String cacheName = StringUtils.firstNonBlank(annotation.cacheName(), annotation.value());
        final Cache<Object, Object> cache = caches.get(ExpressionUtils.render(cacheName, invocation));

        final String key = ExpressionUtils.render(annotation.key(), invocation);

        final String condition = annotation.condition();
        final boolean toUseCache = StringUtils.isBlank(condition) || ExpressionUtils.evaluate(condition, invocation, true);
        if (toUseCache) {
            return getFromCache(cache, key, point);
        }
        final Object result = point.proceed();
        if (Objects.nonNull(result)) {
            cache.put(key, Optional.of(result));
        }
        return result;
    }

    private Object getFromCache(Cache<Object, Object> cache, String key, ProceedingJoinPoint point) throws Throwable {
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
