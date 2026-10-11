package com.example.common.cache.service;

import com.example.common.cache.exception.CacheRefreshException;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Objects;

/**
 * Performs targeted evictions through Spring's Cache abstraction.
 *
 * This intentionally does not clear every cache in the application. Each
 * refreshable endpoint explicitly declares the cache regions and key it owns.
 */
@Service
public class CacheRefreshService {

    private final CacheManager cacheManager;

    public CacheRefreshService(CacheManager cacheManager) {
        this.cacheManager = Objects.requireNonNull(cacheManager, "cacheManager must not be null");
    }

    public void evict(Collection<String> cacheNames, Object key) {
        if (cacheNames == null || cacheNames.isEmpty()) {
            throw new CacheRefreshException("At least one cache name must be configured");
        }

        for (String cacheName : cacheNames) {
            if (cacheName == null || cacheName.isBlank()) {
                throw new CacheRefreshException("Cache names must not be blank");
            }

            Cache cache = cacheManager.getCache(cacheName);

            if (cache == null) {
                throw new CacheRefreshException(
                        "Cache region is not configured: " + cacheName
                );
            }

            try {
                cache.evict(key);
            } catch (RuntimeException exception) {
                throw new CacheRefreshException(
                        "Failed to evict key from cache region: " + cacheName,
                        exception
                );
            }
        }
    }
}