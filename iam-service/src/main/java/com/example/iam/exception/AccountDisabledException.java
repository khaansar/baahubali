package com.example.iam.exception;

public class AccountDisabledException extends RuntimeException {

    public AccountDisabledException() {
        super("This account has been deactivated. Contact support if you think this is a mistake.");
    }
}
