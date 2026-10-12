package com.example.common.cache.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties(prefix = "app.cache")
public class CacheProperties {

    /**
     * Enables the shared application cache configuration.
     */
    private boolean enabled = false;

    /**
     * Default TTL for caches without a specific policy.
     */
    private Duration defaultTtl = Duration.ofMinutes(10);

    /**
     * Per-cache TTL overrides.
     *
     * Keys must match the cache names used by @Cacheable,
     * @CachePut, and @CacheEvict.
     */
    private Map<String, Duration> ttl = new HashMap<>();

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public Duration getDefaultTtl() {
        return defaultTtl;
    }

    public void setDefaultTtl(Duration defaultTtl) {
        this.defaultTtl = defaultTtl;
    }

    public Map<String, Duration> getTtl() {
        return ttl;
    }

    public void setTtl(Map<String, Duration> ttl) {
        this.ttl = ttl != null ? new HashMap<>(ttl) : new HashMap<>();
    }
}
