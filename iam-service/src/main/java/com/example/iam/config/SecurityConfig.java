package com.example.iam.config;

import com.example.iam.dto.ApiResponse;
import com.example.iam.security.GatewayHeaderAuthenticationFilter;
import com.example.iam.security.OAuth2AuthenticationFailureHandler;
import com.example.iam.security.OAuth2AuthenticationSuccessHandler;
import com.example.iam.security.OAuth2AuthorizationRequestResolver;
import com.example.platformcommon.openapi.SwaggerPaths;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final GatewayHeaderAuthenticationFilter gatewayHeaderAuthenticationFilter;
    private final OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler;
    private final OAuth2AuthorizationRequestResolver oauth2AuthorizationRequestResolver;

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public SecurityConfig(
            GatewayHeaderAuthenticationFilter gatewayHeaderAuthenticationFilter,
            OAuth2AuthenticationSuccessHandler oauth2AuthenticationSuccessHandler,
            OAuth2AuthenticationFailureHandler oauth2AuthenticationFailureHandler,
            OAuth2AuthorizationRequestResolver oauth2AuthorizationRequestResolver) {

        this.gatewayHeaderAuthenticationFilter =
                gatewayHeaderAuthenticationFilter;

        this.oauth2AuthenticationSuccessHandler =
                oauth2AuthenticationSuccessHandler;

        this.oauth2AuthenticationFailureHandler =
                oauth2AuthenticationFailureHandler;

        this.oauth2AuthorizationRequestResolver =
                oauth2AuthorizationRequestResolver;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        return http
                .csrf(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)

                /*
                 * Normal platform authentication remains cookie/JWT based.
                 *
                 * IF_REQUIRED allows Spring Security to create a temporary
                 * session only for the OAuth2 authorization-code flow.
                 */
                .sessionManagement(session ->
                        session.sessionCreationPolicy(
                                SessionCreationPolicy.IF_REQUIRED
                        )
                )

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(SwaggerPaths.PUBLIC).permitAll()

                        .requestMatchers(
                                "/v3/api-docs",
                                "/v3/api-docs/**",
                                "/swagger-ui.html",
                                "/swagger-ui/**",
                                "/webjars/**"
                        ).permitAll()

                        .requestMatchers(
                                "/register",
                                "/login",
                                "/internal/**",
                                "/refresh",
                                "/verify-email",
                                "/resend-verification",
                                "/forgot-password",
                                "/reset-password",
                                "/reset-password/validate",
                                "/verify-email/validate",

                                /*
                                 * Spring Security OAuth2 login endpoints.
                                 */
                                "/oauth2/authorization/**",
                                "/login/oauth2/code/**"
                        ).permitAll()

                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/users").hasRole("ADMIN")
                        .requestMatchers(SwaggerPaths.PUBLIC).permitAll()
                        .requestMatchers("/v3/api-docs").permitAll()
                        .anyRequest().authenticated()
                )

                .oauth2Login(oauth2 ->
                        oauth2
                                .authorizationEndpoint(authorization ->
                                        authorization
                                                .authorizationRequestResolver(
                                                        oauth2AuthorizationRequestResolver
                                                )
                                )
                                .successHandler(
                                        oauth2AuthenticationSuccessHandler
                                )
                                .failureHandler(
                                        oauth2AuthenticationFailureHandler
                                )
                )

                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(
                                (request, response, authException) ->
                                        writeSecurityError(
                                                response,
                                                HttpStatus.UNAUTHORIZED,
                                                "AUTHENTICATION_REQUIRED",
                                                "Authentication is required to access this resource"
                                        )
                        )
                        .accessDeniedHandler(
                                (request, response, accessDeniedException) ->
                                        writeSecurityError(
                                                response,
                                                HttpStatus.FORBIDDEN,
                                                "ACCESS_DENIED",
                                                "You do not have permission to access this resource"
                                        )
                        )
                )

                .addFilterBefore(
                        gatewayHeaderAuthenticationFilter,
                        UsernamePasswordAuthenticationFilter.class
                )

                .build();
    }

    private void writeSecurityError(
            HttpServletResponse response,
            HttpStatus status,
            String code,
            String message) throws IOException {

        response.setStatus(status.value());
        response.setContentType("application/json");

        OBJECT_MAPPER.writeValue(
                response.getOutputStream(),
                ApiResponse.error(
                        status.value(),
                        message,
                        code,
                        List.of()
                )
        );
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }
}