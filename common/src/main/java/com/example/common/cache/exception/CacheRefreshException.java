package com.example.common.cache.exception;

/**
 * Signals a cache refresh configuration or execution failure.
 */
public class CacheRefreshException extends RuntimeException {

    public CacheRefreshException(String message) {
        super(message);
    }

    public CacheRefreshException(String message, Throwable cause) {
        super(message, cause);
    }
}