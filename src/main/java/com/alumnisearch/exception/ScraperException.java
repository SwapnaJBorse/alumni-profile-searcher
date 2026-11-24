package com.alumnisearch.exception;

import org.springframework.http.HttpStatus;

/** Raised when the profile-scraping provider fails, times out, or is not configured. */
public class ScraperException extends RuntimeException {

    private final HttpStatus status;

    public ScraperException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public ScraperException(String message, HttpStatus status, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
