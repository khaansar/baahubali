package com.example.payment.config;

import java.util.HashMap;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "internal.auth")
public class InternalAuthProperties {

    private String mySecret;
    private Map<String, String> allowedClients = new HashMap<>();
}