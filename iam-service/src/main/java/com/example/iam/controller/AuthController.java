package com.example.iam.controller;

import com.example.iam.dto.ApiResponse;
import com.example.iam.dto.AuthenticationResult;
import com.example.iam.dto.LoginRequest;
import com.example.iam.dto.RegisterRequest;
import com.example.iam.dto.UserResponse;
import com.example.iam.security.AuthCookieFactory;
import com.example.iam.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AuthCookieFactory authCookieFactory;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse<UserResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthenticationResult result = authService.register(request);
        ResponseCookie cookie = authCookieFactory.buildAuthCookie(result.token());
        ResponseCookie refreshCookie = authCookieFactory.buildRefreshCookie(result.refreshToken());

        return ResponseEntity.status(HttpStatus.CREATED)
                .header(HttpHeaders.SET_COOKIE, cookie.toString(), refreshCookie.toString())
                .body(ApiResponse.success(
                        HttpStatus.CREATED.value(),
                        "Account registered successfully",
                        UserResponse.from(result.user())
                ));
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<UserResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthenticationResult result = authService.authenticate(request);
        ResponseCookie cookie = authCookieFactory.buildAuthCookie(result.token());
        ResponseCookie refreshCookie = authCookieFactory.buildRefreshCookie(result.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, cookie.toString(), refreshCookie.toString())
                .body(ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Login successful",
                        UserResponse.from(result.user())
                ));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, Object>>> logout(@RequestHeader("X-User-Id") String userId) {
        authService.logout(userId);
        ResponseCookie expiredCookie = authCookieFactory.buildExpiredAuthCookie();
        ResponseCookie expiredRefreshCookie = authCookieFactory.buildExpiredRefreshCookie();

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, expiredCookie.toString(), expiredRefreshCookie.toString())
                .body(ApiResponse.success(HttpStatus.OK.value(), "Logout successful", Map.of()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<Map<String, Object>>> refresh(
            @CookieValue(name = "${app.jwt.refresh-cookie-name}", required = false) String refreshToken) {
        AuthenticationResult result = authService.refresh(refreshToken);
        ResponseCookie authCookie = authCookieFactory.buildAuthCookie(result.token());
        ResponseCookie refreshCookie = authCookieFactory.buildRefreshCookie(result.refreshToken());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, authCookie.toString(), refreshCookie.toString())
                .body(ApiResponse.success(HttpStatus.OK.value(), "Token refreshed", Map.of()));
    }

    @GetMapping("/verify-email")
    public ResponseEntity<ApiResponse<Map<String, Object>>> verifyEmail(
            @RequestParam String token) {

        authService.verifyEmail(token);

        return ResponseEntity.ok()
                .body(ApiResponse.success(
                        HttpStatus.OK.value(),
                        "Email verified successfully",
                        Map.of()
                ));
    }

    @PostMapping("/resend-verification")
    public ResponseEntity<ApiResponse<Map<String, Object>>> resendVerification(
            @RequestParam String email) {

        authService.resendVerificationEmail(email);

        return ResponseEntity.ok()
                .body(ApiResponse.success(
                        HttpStatus.OK.value(),
                        "If the account exists and requires verification, a verification email has been sent",
                        Map.of()
                ));
    }
}