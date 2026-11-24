package com.campusmarketplace.exception;

/**
 * EXCEPTIONS: custom CHECKED exception (extends Exception, not RuntimeException),
 * so the compiler forces every caller to either catch it or declare "throws".
 * It carries the HTTP status code; Router (the one central handler) turns it into
 * {"error": "..."} JSON. Subclasses below show INHERITANCE of exceptions.
 */
public class ApiException extends Exception {

    private final int status; // ENCAPSULATION: private + getter only

    public ApiException(int status, String message) {
        super(message); // constructor chaining to Exception(String)
        this.status = status;
    }

    public int getStatus() {
        return status;
    }
}
