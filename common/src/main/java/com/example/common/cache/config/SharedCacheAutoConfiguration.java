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

    private static final Duration FALLBACK_TTL = Duration.ofMinutes(10);

    @Bean
    @ConditionalOnMissingBean(CacheManager.class)
    public RedisCacheManager cacheManager(
            RedisConnectionFactory redisConnectionFactory,
            CacheProperties properties
    ) {
        Duration defaultTtl = properties.getDefaultTtl() != null
                ? properties.getDefaultTtl()
                : FALLBACK_TTL;
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
                        .entryTtl(defaultTtl);

                        Map<String, RedisCacheConfiguration> cacheConfigurations =
                                new HashMap<>();

                        if (properties.getTtl() != null) {
                            properties.getTtl().forEach((cacheName, ttl) -> {
                                if (cacheName != null
                                        && !cacheName.isBlank()
                                        && ttl != null
                                        && !ttl.isNegative()) {
                                    cacheConfigurations.put(
                                            cacheName,
                                            defaultConfiguration.entryTtl(ttl)
                                    );
                                }
                            });
                        }

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultConfiguration)
                .withInitialCacheConfigurations(cacheConfigurations)
                .enableStatistics()
                .build();
    }
}
