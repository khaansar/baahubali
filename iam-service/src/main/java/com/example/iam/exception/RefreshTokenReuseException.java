package com.example.iam.exception;

public class RefreshTokenReuseException extends RuntimeException {

    public RefreshTokenReuseException() {
        super("A refresh token was reused. The session has been revoked.");
    }
}
