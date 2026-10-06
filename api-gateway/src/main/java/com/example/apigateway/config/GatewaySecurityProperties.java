package com.example.apigateway.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.validation.annotation.Validated;


@Validated
@ConfigurationProperties(prefix = "gateway.security")
public record GatewaySecurityProperties(
        @NotEmpty List<@NotBlank String> publicPaths,
        @Valid @NotNull Jwt jwt) {

    public record Jwt(
            @DefaultValue("RS256") @NotBlank String algorithm,
            String secret,
            String publicKey,
            @DefaultValue("jwt_token") @NotBlank String cookieName,
            @DefaultValue("sub") @NotBlank String userIdClaim,
            @DefaultValue("role") @NotBlank String roleClaim,
            @NotBlank String issuer,
            @NotBlank String audience,
            @DefaultValue("30") @PositiveOrZero long clockSkewSeconds) {
    }
}
