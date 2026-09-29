package com.example.testservice.config;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

@Configuration
@EnableCaching
public class RedisCacheConfig {

    @Bean
    public RedisCacheManager cacheManager(RedisConnectionFactory redisConnectionFactory) {
        // Configure Jackson to store class type info so deserialization works for generics/interfaces
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        objectMapper.activateDefaultTyping(
                LaissezFaireSubTypeValidator.instance,
                ObjectMapper.DefaultTyping.NON_FINAL,
                JsonTypeInfo.As.PROPERTY
        );
        GenericJackson2JsonRedisSerializer serializer = new GenericJackson2JsonRedisSerializer(objectMapper);

        // Base configuration
        RedisCacheConfiguration defaultCacheConfig = RedisCacheConfiguration.defaultCacheConfig()
                .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(new StringRedisSerializer()))
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(serializer))
                // Disable the default double-colon prefixing; we will explicitly define full keys in the cache names or let it use a single colon
                .computePrefixWith(cacheName -> cacheName + ":") 
                .entryTtl(Duration.ofMinutes(10)); // Default fallback TTL

        // TTL configurations for specific cache namespaces
        Map<String, RedisCacheConfiguration> cacheConfigurations = new HashMap<>();

        // Categories: 10 mins
        cacheConfigurations.put("baahubali:test:categories", defaultCacheConfig.entryTtl(Duration.ofMinutes(10)));
        
        // Homepage: 120 secs
        cacheConfigurations.put("baahubali:test:homepage:series:popular", defaultCacheConfig.entryTtl(Duration.ofSeconds(120)));
        cacheConfigurations.put("baahubali:test:homepage:mock-tests:featured", defaultCacheConfig.entryTtl(Duration.ofSeconds(120)));
        
        // Series & Tests: 5 mins
        cacheConfigurations.put("baahubali:test:series:list", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("baahubali:test:series", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));
        cacheConfigurations.put("baahubali:test:test", defaultCacheConfig.entryTtl(Duration.ofMinutes(5)));

        return RedisCacheManager.builder(redisConnectionFactory)
                .cacheDefaults(defaultCacheConfig)
                .withInitialCacheConfigurations(cacheConfigurations)
                .enableStatistics()
                .build();
    }
}
