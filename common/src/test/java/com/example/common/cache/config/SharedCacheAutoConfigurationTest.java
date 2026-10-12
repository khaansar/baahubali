package com.example.common.cache.config;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

class SharedCacheAutoConfigurationTest {

    private final SharedCacheAutoConfiguration configuration =
            new SharedCacheAutoConfiguration();

    @Test
    void createsATransactionAwareManager() {
        CacheProperties properties = new CacheProperties();

        RedisCacheManager cacheManager = configuration.cacheManager(
                mock(RedisConnectionFactory.class), properties);

        assertThat(cacheManager.isTransactionAware()).isTrue();
    }
}
