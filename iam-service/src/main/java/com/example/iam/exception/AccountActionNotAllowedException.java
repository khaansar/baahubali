package com.example.iam.exception;

/** An account action that conflicts with the account's current state or the caller's role. */
public class AccountActionNotAllowedException extends RuntimeException {

    public AccountActionNotAllowedException(String message) {
        super(message);
    }
}
