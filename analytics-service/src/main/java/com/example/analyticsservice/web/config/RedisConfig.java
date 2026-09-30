package com.example.analyticsservice.web.config;

import com.example.analyticsservice.web.service.ReportNotificationService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;

@Configuration
public class RedisConfig {

    @Bean
    RedisMessageListenerContainer container(
            RedisConnectionFactory connectionFactory,
            ReportNotificationService reportNotificationService) {

        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(reportNotificationService,new ChannelTopic(ReportNotificationService.TOPIC));
        return container;
    }
}