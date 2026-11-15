package com.example.iam.exception;

/**
 * A recently rotated refresh token arrived after its replacement had itself been rotated.
 * The browser already holds a newer token, so the request is refused without clearing cookies.
 */
public class RefreshTokenSupersededException extends RuntimeException {

    public RefreshTokenSupersededException() {
        super("This refresh token was already replaced. Retry with the current session.");
    }
}
