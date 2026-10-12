package com.example.common.cache.config;

import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CachePropertiesTest {

    @Test
    void usesTenMinutesAsTheDefaultTtl() {
        CacheProperties properties = new CacheProperties();

        assertThat(properties.getDefaultTtl()).isEqualTo(Duration.ofMinutes(10));
    }

    @Test
    void retainsPerRegionTtlOverrides() {
        CacheProperties properties = new CacheProperties();

        properties.setTtl(Map.of("analytics:report", Duration.ofDays(30)));

        assertThat(properties.getTtl())
                .containsEntry("analytics:report", Duration.ofDays(30));
    }

    @Test
    void rejectsZeroAndNegativeTtls() {
        CacheProperties properties = new CacheProperties();

        assertThatThrownBy(() -> properties.setDefaultTtl(Duration.ZERO))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("default-ttl");
        assertThatThrownBy(() -> properties.setTtl(Map.of("catalog", Duration.ofSeconds(-1))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("app.cache.ttl.catalog");
    }
}
