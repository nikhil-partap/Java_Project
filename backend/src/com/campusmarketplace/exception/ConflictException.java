package com.campusmarketplace.exception;

/** EXCEPTIONS + INHERITANCE: HTTP 409. Request conflicts with current state (item already sold, duplicate email). */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(409, message); // super(...) calls the ApiException constructor
    }
}
