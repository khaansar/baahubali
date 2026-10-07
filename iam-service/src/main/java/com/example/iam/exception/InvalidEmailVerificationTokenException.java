package com.example.iam.exception;

public class InvalidEmailVerificationTokenException extends RuntimeException {

    public InvalidEmailVerificationTokenException() {
        super("The email verification link is invalid or has expired");
    }
}