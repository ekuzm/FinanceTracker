package com.finance.tracker.cache;

import java.util.List;

public record CacheKey(Class<?> entityClass, String methodName, List<Object> args) {
    public CacheKey(Class<?> entityClass, String methodName, Object... args) {
        this(entityClass, methodName, List.of(args));
    }
}
