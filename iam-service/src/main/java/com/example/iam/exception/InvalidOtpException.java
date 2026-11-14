package com.example.iam.exception;

public class InvalidOtpException extends RuntimeException {

    public InvalidOtpException() {
        super("The code is invalid or has expired. Request a new code and try again.");
    }
}
