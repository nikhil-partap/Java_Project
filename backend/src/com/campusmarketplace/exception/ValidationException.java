package com.campusmarketplace.exception;

/** EXCEPTIONS + INHERITANCE: HTTP 400. Request data is invalid (blank title, bad number, bad JSON...). */
public class ValidationException extends ApiException {

    public ValidationException(String message) {
        super(400, message); // super(...) calls the ApiException constructor
    }
}
