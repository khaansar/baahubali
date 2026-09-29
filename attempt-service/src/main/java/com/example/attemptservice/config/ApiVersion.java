package com.example.attemptservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ApiVersion {

    private static volatile String value = "unconfigured";

    public ApiVersion(@Value("${app.api.version}") String configuredValue) {
        value = configuredValue;
    }

    public static String current() {
        return value;
    }
}
