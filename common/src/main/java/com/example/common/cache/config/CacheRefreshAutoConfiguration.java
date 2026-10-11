package com.example.common.cache.config;

import com.example.common.cache.aspect.CacheRefreshAspect;
import com.example.common.cache.security.CacheRefreshAuthorizer;
import com.example.common.cache.service.CacheRefreshService;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.cache.CacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Registers the shared cache-refresh infrastructure in servlet-based services
 * that provide a Spring CacheManager. Reactive services and services without
 * Spring caching remain unaffected.
 */
@AutoConfiguration(
        afterName = "org.springframework.boot.cache.autoconfigure.CacheAutoConfiguration"
)
@ConditionalOnClass(CacheManager.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@ConditionalOnBean(CacheManager.class)
@EnableAspectJAutoProxy
public class CacheRefreshAutoConfiguration {

    @Bean
    public CacheRefreshAuthorizer cacheRefreshAuthorizer(
            org.springframework.core.env.Environment environment
    ) {
        return new CacheRefreshAuthorizer(
                environment.getProperty("cache.refresh.secret", "")
        );
    }

    @Bean
    public CacheRefreshService cacheRefreshService(
            CacheManager cacheManager
    ) {
        return new CacheRefreshService(cacheManager);
    }

    @Bean
    public CacheRefreshAspect cacheRefreshAspect(
            CacheRefreshService cacheRefreshService,
            CacheRefreshAuthorizer cacheRefreshAuthorizer
    ) {
        return new CacheRefreshAspect(
                cacheRefreshService,
                cacheRefreshAuthorizer
        );
    }
}