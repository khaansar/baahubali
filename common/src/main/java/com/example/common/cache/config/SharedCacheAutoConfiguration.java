package com.example.common.cache.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@AutoConfiguration(
        beforeName = "org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration"
)
@ConditionalOnClass({
        RedisConnectionFactory.class,
        RedisCacheManager.class
})
@ConditionalOnProperty(
        name = "app.cache.enabled",
        havingValue = "true"
)
@EnableCaching
@EnableConfigurationProperties(CacheProperties.class)
public class SharedCacheAutoConfiguration {

    private static final Duration DEFAULT_TTL = Duration.ofMinutes(10);

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public RedisCacheManager cacheManager(
            RedisConnectionFactory redisConnectionFactory
    ) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.EVERYTHING,
                JsonTypeInfo.As.PROPERTY
        );

        GenericJackson2JsonRedisSerializer valueSerializer =
                new GenericJackson2JsonRedisSerializer(objectMapper);

        RedisCacheConfiguration defaultConfiguration =
                RedisCacheConfiguration.defaultCacheConfig()
                        .serializeKeysWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(new StringRedisSerializer())
                        )
                        .serializeValuesWith(
                                RedisSerializationContext.SerializationPair
                                        .fromSerializer(valueSerializer)
                        )
                        .computePrefixWith(cacheName -> cacheName + ":")
                        .entryTtl(DEFAULT_TTL);

        Map<String, RedisCacheConfiguration> cacheConfigurations =
                new HashMap<>();

        // Test-service cache policies: preserve existing TTLs.
        cacheConfigurations.put(
                "baahubali:test:categories",
                defaultConfiguration.entryTtl(Duration.ofMinutes(10))
        );
        cacheConfigurations.put(
                "baahubali:test:homepage:series:popular",
                defaultConfiguration.entryTtl(Duration.ofSeconds(120))
        );
        cacheConfigurations.put(
                "baahubali:test:homepage:mock-tests:featured",
                defaultConfiguration.entryTtl(Duration.ofSeconds(120))
        );
        cacheConfigurations.put(
                "baahubali:test:series:list",
                defaultConfiguration.entryTtl(Duration.ofMinutes(5))
        );
        cacheConfigurations.put(
                "baahubali:test:series",
                defaultConfiguration.entryTtl(Duration.ofMinutes(5))
        );
        cacheConfigurations.put(
                "baahubali:test:test",
                defaultConfiguration.entryTtl(Duration.ofMinutes(5))
        );
        cacheConfigurations.put(
                "baahubali:test:mocktests:list",
                defaultConfiguration.entryTtl(Duration.ofMinutes(10))
        );
        cacheConfigurations.put(
                "test_blueprints",
                defaultConfiguration.entryTtl(Duration.ofMinutes(10))
        );

        // Analytics-service policy: preserve its existing 30-day TTL.
        cacheConfigurations.put(
                "analytics:report",
                defaultConfiguration.entryTtl(Duration.ofDays(30))
        );

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfiguration)
                .withInitialCacheConfigurations(cacheConfigurations)
                .enableStatistics()
                .build();
    }
}
